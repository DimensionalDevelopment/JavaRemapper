package org.dimdev.srg2jam;

import org.dimdev.javaremapper.Mapping;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public final class Srg2Jam {
    public static void convert(File mcpDir) throws IOException {
        // Read CSVs
        Map<String, String> fieldSrgMcpMap = readCSV(new File(mcpDir, "fields.csv"));
        Map<String, String> methodSrgMcpMap = readCSV(new File(mcpDir, "methods.csv"));
        Map<String, String> paramSrgMcpMap = readCSV(new File(mcpDir, "params.csv"));

        // Read static_methods.txt
        Set<String> staticMethodSrgNames = new HashSet<>();
        try (FileInputStream inputStream = new FileInputStream(new File(mcpDir, "static_methods.txt"))) {
            Scanner scanner = new Scanner(inputStream);
            while (scanner.hasNextLine()) staticMethodSrgNames.add(scanner.nextLine());
        }

        Mapping srgMappings = new Mapping();
        Mapping mcpMappings = new Mapping();

        // Read joined.srg
        Map<String, String> reverseClassNameMap = new HashMap<>(); // For joined.exc
        try (FileInputStream inputStream = new FileInputStream(new File(mcpDir, "joined.srg"))) {
            Scanner scanner = new Scanner(inputStream);

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] entry = line.split(" ");
                switch (entry[0]) {
                    case "PK:": {
                        break;
                    }
                    case "CL:": {
                        srgMappings.addClass(entry[1], entry[2]);
                        mcpMappings.addClass(entry[1], entry[2]);
                        reverseClassNameMap.put(entry[2], entry[1]);
                        break;
                    }
                    case "FD:": {
                        int lastSlash = entry[1].lastIndexOf('/');
                        String className = entry[1].substring(0, lastSlash);
                        String fieldName = entry[1].substring(lastSlash + 1);
                        String srgName = entry[2].substring(entry[2].lastIndexOf('/') + 1);
                        String mcpName = fieldSrgMcpMap.getOrDefault(srgName, srgName);
                        srgMappings.addField(className, fieldName, "*", srgName);
                        mcpMappings.addField(className, fieldName, "*", mcpName);
                        break;
                    }
                    case "MD:": {
                        int lastSlash = entry[1].lastIndexOf('/');
                        String className = entry[1].substring(0, lastSlash);
                        String methodName = entry[1].substring(lastSlash + 1);
                        String srgName = entry[3].substring(entry[3].lastIndexOf('/') + 1);
                        String mcpName = methodSrgMcpMap.getOrDefault(srgName, srgName);
                        String descriptor = entry[2];
                        srgMappings.addMethod(className, methodName, descriptor, srgName);
                        mcpMappings.addMethod(className, methodName, descriptor, mcpName);

                        if (!srgName.startsWith("func_")) break;
                        String id = srgName.split("_")[1];
                        int index = 0;
                        for (int position : getParamPositions(descriptor, staticMethodSrgNames.contains(srgName))) {
                            String srgParamName = "p_" + id + "_" + position + "_";
                            String mcpParamName = paramSrgMcpMap.getOrDefault(srgParamName, srgParamName);
                            srgMappings.addParameter(className, methodName, descriptor, index, srgParamName);
                            mcpMappings.addParameter(className, methodName, descriptor, index, mcpParamName);
                            index++;
                        }
                        break;
                    }
                }
            }
        }

        // Read joined.exc for constructor params
        try (FileInputStream inputStream = new FileInputStream(new File(mcpDir, "joined.exc"))) {
            Scanner scanner = new Scanner(inputStream);

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.startsWith("#")) continue; // Comments
                if (line.startsWith("max_constructor_index")) continue;

                String[] keyAndValue = line.split("=");
                String[] classAndMethod = keyAndValue[0].split("\\.");
                int descriptorIndex = classAndMethod[1].indexOf('(');
                String methodName = classAndMethod[1].substring(0, descriptorIndex);
                if (!methodName.equals("<init>")) continue;

                String className = reverseClassNameMap.getOrDefault(classAndMethod[0], classAndMethod[0]);
                String descriptor = translateDescriptor(classAndMethod[1].substring(descriptorIndex), reverseClassNameMap);

                String[] params = keyAndValue[1].split("\\|")[1].split(",");

                int index = 0;
                for (String srgParamName : params) {
                    String mcpParamName = paramSrgMcpMap.getOrDefault(srgParamName, srgParamName);
                    srgMappings.addParameter(className, "<init>", descriptor, index, srgParamName);
                    mcpMappings.addParameter(className, "<init>", descriptor, index, mcpParamName);
                    index++;
                }
            }
        }

        srgMappings.writeToJAM(new FileWriter(new File("notch-srg.jam")));
        mcpMappings.writeToJAM(new FileWriter(new File("notch-mcp.jam")));
    }

    private static List<Integer> getParamPositions(String descriptor, boolean isStatic) {
        List<Integer> positions = new ArrayList<>();
        int position = isStatic ? 0 : 1;
        boolean inClassName = false;
        for (char c : descriptor.toCharArray()) {
            if (inClassName && c != ';') continue;
            inClassName = false;
            if (c == ')') break;
            if (c == '(' || c == '[' || c == ';') continue;
            if (c == 'L') inClassName = true;

            positions.add(position);
            position += c == 'D' || c == 'J' ? 2 : 1;
        }
        return positions;
    }

    private static String translateDescriptor(String descriptor, Map<String, String> classNameMap) {
        StringBuilder result = new StringBuilder();
        StringBuilder classNameBuilder = null;
        for (char c : descriptor.toCharArray()) {
            if (classNameBuilder != null) {
                if (c == ';') {
                    String className = classNameMap.getOrDefault(classNameBuilder.toString(), classNameBuilder.toString());
                    result.append(className).append(";");
                    classNameBuilder = null;
                } else {
                    classNameBuilder.append(c);
                }
            } else {
                if (c == 'L') classNameBuilder = new StringBuilder();
                result.append(c);
            }
        }
        return result.toString();
    }

    private static Map<String, String> readCSV(File file) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            Scanner scanner = new Scanner(inputStream);
            scanner.nextLine(); // skip header

            Map<String, String> results = new LinkedHashMap<>();
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] entry = line.split(",");
                results.put(entry[0], entry[1]);
            }

            return results;
        }
    }
}
