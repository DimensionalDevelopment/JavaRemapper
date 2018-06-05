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
        if (newName != null) return newName;

        for (String superClass : inheritanceProvider.getAllSuperclasses(owner)) {
            newName = mapping.getField(superClass, name, desc);
            if (newName != null) return newName;
        }
        return name;
    }

    @Override
    public String mapMethodName(String owner, String name, String desc) {
        String newName = mapping.getMethod(owner, name, desc);
        if (newName != null) return newName;

        for (String superClass : inheritanceProvider.getAllSuperclasses(owner)) {
            newName = mapping.getMethod(superClass, name, desc);
            if (newName != null) return newName;
        }
        return name;
    }
}
