package org.dimdev.javaremapper;

import java.util.Set;

public class GeneratingMapping extends Mapping {
    private InheritanceProvider inheritanceProvider;
    private Set<String> classFilter;
    private int classIndex = 0;
    private int fieldIndex = 0;
    private int methodIndex = 0;

    public GeneratingMapping(InheritanceProvider inheritanceProvider, Set<String> classFilter) {
        this.inheritanceProvider = inheritanceProvider;
        this.classFilter = classFilter;
    }

    public String getClass(String className) {
        // Don't remap classes not in jar
        if (!classFilter.contains(className)) return null;

        String result = super.getClass(className);
        if (result == null) {
            result = "Class" + classIndex++;
            addClass(className, result);
        }
        return result;
    }

    public String getField(String className, String fieldName, String fieldDescriptor) {
        // Don't remap classes not in jar
        if (!classFilter.contains(className)) return null;

        // Don't remap inherited fields, their name is inherited from the parent's mapping
        for (String superclass : inheritanceProvider.getAllSuperclasses(className)) {
            if (inheritanceProvider.getInheritableFields(superclass).contains(new MemberRef(fieldName, fieldDescriptor))) {
                return null;
            }
        }

        String result = super.getField(className, fieldName, fieldDescriptor);
        if (result == null) {
            result = "field" + fieldIndex++;
            addField(className, fieldName, fieldDescriptor, result);
        }
        return result;
    }

    public String getMethod(String className, String methodName, String methodDescriptor) {
        // Don't remap classes not in jar
        if (!classFilter.contains(className)) return null;

        // Don't remap <init>, <clinit>, values, valueOf, access$* TODO: don't remap any synthetic/bridge methods
        if (methodName.equals("<init>") ||
            methodName.equals("<clinit>") ||
            methodName.equals("values") ||
            methodName.equals("valueOf") ||
            methodName.startsWith("access$")) return null;

        // Don't remap inherited methods, their name is inherited from the parent's mapping
        for (String superclass : inheritanceProvider.getAllSuperclasses(className)) {
            for (MemberRef ref : inheritanceProvider.getInheritableMethods(superclass)) {
                if (methodName.equals(ref.name) && TypeUtil.methodDescriptorOverrides(inheritanceProvider, methodDescriptor, ref.descriptor)) {
                    return null;
                }
            }
        }

        String result = super.getMethod(className, methodName, methodDescriptor);
        if (result == null) {
            result = "method" + methodIndex++;
            addMethod(className, methodName, methodDescriptor, result);
        }
        return result;
    }
}
