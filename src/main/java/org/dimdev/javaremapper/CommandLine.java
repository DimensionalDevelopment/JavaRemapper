package org.dimdev.javaremapper;

import org.dimdev.srg2jam.Srg2Jam;

import java.io.*;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class CommandLine {
    public static void main(String... args) throws IOException {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            System.out.println("JavaRemapper - Tool for remapping obfuscated jar files");
            System.out.println("JAM format specification: https://github.com/caseif/JAM");
            System.out.println();
            System.out.println("Subcommands:");
            System.out.println(" remap <jar> <target> <mappings> - Remaps a jar file using a JAM mapping file");
            System.out.println(" rename <jar> <target> <mappings> - Generates mappings with unique identifiers for everything");
            System.out.println(" srg2jam <path to MCP config folder> - Converts a MCP config folder to a JAM file");
            System.out.println(" help - Displays this help message");
            return;
        }

        switch (args[0].toLowerCase()) {
            case "remap": {
                File inputFile = new File(args[1]);
                File remapTarget = new File(args[2]);
                File mappingFile = new File(args[3]);

                if (remapTarget.exists()) remapTarget.delete();

                Mapping mapping = new Mapping();
                try (Reader reader = new FileReader(mappingFile)) {
                    mapping.readFromJAM(reader);
                }

                new JavaRemapper(mapping).remapJar(inputFile, remapTarget);
                break;
            }

            case "rename" : {
                File inputFile = new File(args[1]);
                File remapTarget = new File(args[2]);
                File mappingTarget = new File(args[3]);

                if (remapTarget.exists()) remapTarget.delete();
                if (mappingTarget.exists()) mappingTarget.delete();

                Set<String> classesInJar = new HashSet<>();
                try (JarFile jar = new JarFile(inputFile)) {
                    Enumeration<JarEntry> entries = jar.entries();
                    do {
                        JarEntry entry = entries.nextElement();
                        String name = entry.getName();
                        if (name.endsWith(".class")) {
                            String className = name.substring(0, name.length() - 6);
                            classesInJar.add(className);
                        }
                    } while (entries.hasMoreElements());
                }

                InheritanceProvider inheritanceProvider = JavaRemapper.makeInheritanceProvider(inputFile);
                Mapping mapping = new GeneratingMapping(inheritanceProvider, classesInJar);
                new JavaRemapper(mapping).remapJar(inputFile, remapTarget);

                mapping.writeToJAM(new FileWriter(mappingTarget));

                break;
            }

            case "srg2jam": {
                Srg2Jam.convert(new File(args[1]));
                break;
            }

            default: {
                System.err.println("Unknown subcommand, use \"help\" for a list of subcommands.");
                break;
            }
        }
    }
}
