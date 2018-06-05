package org.dimdev.javaremapper;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.util.*;

public class InheritanceMapper extends ClassVisitor implements InheritanceProvider {
    private Map<String, Set<String>> inheritanceMap = new HashMap<>();
    private Map<String, Set<String>> allSuperclassCache = new HashMap<>();

    public InheritanceMapper() {
        super(Opcodes.ASM6);
    }

    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);

        Set<String> inheritanceSet = inheritanceMap.computeIfAbsent(name, k -> new HashSet<>());
        inheritanceSet.add(superName);
        inheritanceSet.addAll(Arrays.asList(interfaces));
    }

    @Override
    public Set<String> getSuperclasses(String name) {
        return inheritanceMap.getOrDefault(name, Collections.emptySet());
    }

    @Override
    public Set<String> getAllSuperclasses(String name) {
        Set<String> cacheResult = allSuperclassCache.get(name);
        if (cacheResult != null) return cacheResult;

        Set<String> superclasses = new HashSet<>();
        Deque<String> stack = new ArrayDeque<>(getSuperclasses(name));

        while (!stack.isEmpty()) {
            String currentClass = stack.pop();
            superclasses.add(currentClass);
            stack.addAll(getSuperclasses(currentClass));
        }

        allSuperclassCache.put(name, superclasses);
        return superclasses;
    }

    @Override
    public Map<String, Set<String>> getInheritanceMap() {
        return inheritanceMap;
    }
}
