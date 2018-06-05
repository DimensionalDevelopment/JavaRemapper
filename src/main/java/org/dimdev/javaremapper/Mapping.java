package org.dimdev.javaremapper;

import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

public class Mapping {
    public Map<String, String> classes = new LinkedHashMap<>();
    public Map<String, String> methods = new LinkedHashMap<>();
    public Map<String, String> fields = new LinkedHashMap<>();
    public Map<String, String> parameters = new LinkedHashMap<>();
    public Map<String, String> locals = new LinkedHashMap<>();

    public String getClass(String className) {
        return classes.get(className);
    }

    public String getField(String className, String fieldName, String fieldDescriptor) {
        String newName = fields.get(className + " " + fieldName + " " + fieldDescriptor);
        if (newName != null) return newName;

        return fields.get(className + " " + fieldName + " *");
    }

    public String getMethod(String className, String methodName, String methodDescriptor) {
        return methods.get(className + " " + methodName + " " + methodDescriptor);
    }

    public String getParameter(String className, String methodName, String methodDescriptor, int index) {
        return parameters.get(className + " " + methodName + " " + methodDescriptor + " " + index);
    }

    public String getLocal(String className, String methodName, String methodDescriptor, int index) {
        return locals.get(className + " " + methodName + " " + methodDescriptor + " " + index);
    }

    public String mapClass(String className) {
        String newName = getClass(className);
        return newName == null ? className : newName;
    }

    public String mapField(String className, String fieldName, String fieldDescriptor) {
        String newName = getField(className, fieldName, fieldDescriptor);
        return newName == null ? fieldName : newName;
    }

    public String mapMethod(String className, String methodName, String methodDescriptor) {
        String newName = getMethod(className, methodName, methodDescriptor);
        return newName == null ? methodName : newName;
    }

    public String mapParameter(String className, String methodName, String methodDescriptor, int index) {
        String newName = getParameter(className, methodName, methodDescriptor, index);
        return newName == null ? "par" + index : newName;
    }

    public String mapLocal(String className, String methodName, String methodDescriptor, int index) {
        String newName = getLocal(className, methodName, methodDescriptor, index);
        return newName == null ? "var" + index : newName;
    }

    public void addClass(String className, String newName) {
        classes.put(className, newName);
    }

    public void addField(String className, String fieldName, String fieldDescriptor, String newName) {
        fields.put(className + " " + fieldName + " " + fieldDescriptor, newName);
    }

    public void addMethod(String className, String methodName, String methodDescriptor, String newName) {
        methods.put(className + " " + methodName + " " + methodDescriptor, newName);
    }

    public void addParameter(String className, String methodName, String methodDescriptor, int index, String newName) {
        parameters.put(className + " " + methodName + " " + methodDescriptor + " " + index, newName);
    }

    public void addLocal(String className, String methodName, String methodDescriptor, int index, String newName) {
        locals.put(className + " " + methodName + " " + methodName + " " + index, newName);
    }

    /** Reads the mappings from JAM format (https://github.com/caseif/JAM) **/
    public void readFromJAM(Reader reader) {
        Scanner scanner = new Scanner(reader);

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.startsWith("//")) continue; // Comment

            String[] entry = line.split(" ");
            switch (entry[0]) {
                case "CL":
                    addClass(entry[1], entry[2]);
                    break;
                case "FD":
                    addField(entry[1], entry[2], entry[3], entry[4]);
                    break;
                case "MD":
                    addMethod(entry[1], entry[2], entry[3], entry[4]);
                    break;
                case "MP":
                    addParameter(entry[1], entry[2], entry[3], Integer.parseInt(entry[4]), entry[5]);
                    break;
                case "LV":
                    addLocal(entry[1], entry[2], entry[3], Integer.parseInt(entry[4]), entry[5]);
                    break;
            }
        }
    }

    /** Writes the mappings in JAM format (https://github.com/caseif/JAM) **/
    public void writeToJAM(Writer writer) {
        PrintWriter w = new PrintWriter(writer);
        // Write classes
        for (Map.Entry<String, String> entry : classes.entrySet()) {
            w.println("CL " + entry.getKey() + " " + entry.getValue());
        }

        // Write fields
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            w.println("FD " + entry.getKey() + " " + entry.getValue());
        }

        // Write methods
        for (Map.Entry<String, String> entry : methods.entrySet()) {
            w.println("MD " + entry.getKey() + " " + entry.getValue());
        }

        // Write parameters
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            w.println("MP " + entry.getKey() + " " + entry.getValue());
        }

        // Write locals
        for (Map.Entry<String, String> entry : locals.entrySet()) {
            w.println("LV " + entry.getKey() + " " + entry.getValue());
        }

        w.flush();
    }
}
