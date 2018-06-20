package org.dimdev.javaremapper;

import org.objectweb.asm.Type;

public final class TypeUtil {
    public static boolean methodDescriptorOverrides(InheritanceProvider inheritanceProvider, String descriptor1, String descriptor2) {
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
