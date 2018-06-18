package org.dimdev.javaremapper;

import org.objectweb.asm.Type;

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
                if (methodName.equals(ref.name) && methodDescriptorOverrides(methodDescriptor, ref.descriptor)) {
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

    private boolean methodDescriptorOverrides(String descriptor1, String descriptor2) {
        // Check return types
        Type ret1 = Type.getReturnType(descriptor1);
        Type ret2 = Type.getReturnType(descriptor2);
        if (!ret1.equals(ret2) && !inheritanceProvider.getAllSuperclasses(ret1.getClassName()).contains(ret2.getClassName())) {
            return false;
        }

        // Check argument types
        Type[] args1 = Type.getArgumentTypes(descriptor1);
        Type[] args2 = Type.getArgumentTypes(descriptor2);

        if (args1.length != args2.length) return false;

        for (int i = 0; i < args1.length; i++) {
            String class1 = args1[i].getClassName();
            String class2 = args2[i].getClassName();

            // Arguments must be the identical in Java, not supertypes
            if (!class1.equals(class2)) {
                return false;
            }
        }

        return true;
    }
}
