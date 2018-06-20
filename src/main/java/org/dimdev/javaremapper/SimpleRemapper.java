package org.dimdev.javaremapper;

import org.objectweb.asm.commons.Remapper;

public class SimpleRemapper extends Remapper {
    private Mapping mapping;
    private InheritanceProvider inheritanceProvider;

    public SimpleRemapper(Mapping mapping, InheritanceProvider inheritanceProvider) {
        this.mapping = mapping;
        this.inheritanceProvider = inheritanceProvider;
    }

    @Override
    public String map(String typeName) {
        return mapping.mapClass(typeName);
    }

    @Override
    public String mapFieldName(String owner, String name, String desc) {
        String newName = mapping.getField(owner, name, desc);

        for (String superclass : inheritanceProvider.getAllSuperclasses(owner)) {
            if (inheritanceProvider.getInheritableFields(superclass).contains(new MemberRef(name, desc))) {
                String inheritedNewName = mapping.getField(superclass, name, desc);
                if (inheritedNewName != null) {
                    if (newName != null && !inheritedNewName.equals(newName)) {
                        System.err.println("Field inheritance problem: " + owner + "." + name + " " + desc +
                                           " inherits " + superclass + "." + name + " " + desc +
                                           " but " + newName + " != " + inheritedNewName);
                    }
                    return inheritedNewName;
                }
            }
        }
        if (newName != null) return newName;
        return name;
    }

    @Override
    public String mapMethodName(String owner, String name, String desc) {
        String newName = mapping.getMethod(owner, name, desc);

        for (String superclass : inheritanceProvider.getAllSuperclasses(owner)) {
            for (MemberRef ref : inheritanceProvider.getInheritableMethods(superclass)) {
                if (name.equals(ref.name) && TypeUtil.methodDescriptorOverrides(inheritanceProvider, desc, ref.descriptor)) {
                    String inheritedNewName = mapping.getMethod(superclass, name, desc);
                    if (inheritedNewName != null) {
                        if (newName != null && !inheritedNewName.equals(newName)) {
                            System.err.println("Method inheritance problem: " + owner + "." + name + desc +
                                               " inherits " + superclass + "." + name + desc +
                                               " but " + newName + " != " + inheritedNewName);
                        }
                        return inheritedNewName;
                    }
                }
            }
        }
        if (newName != null) return newName;
        return name;
    }
}
