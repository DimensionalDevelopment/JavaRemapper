package org.dimdev.javaremapper;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.*;

public class InheritanceMapper extends ClassVisitor implements InheritanceProvider {
    private Map<String, Set<String>> inheritanceMap = new HashMap<>();
    private Map<String, Set<String>> allSuperclassCache = new HashMap<>();
    private Map<String, Set<MemberRef>> inheritableMethods = new HashMap<>();
    private Map<String, Set<MemberRef>> inheritableFields = new HashMap<>();
    private String className;

    public InheritanceMapper() {
        super(Opcodes.ASM6);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);

        Set<String> inheritanceSet = inheritanceMap.computeIfAbsent(name, k -> new HashSet<>());
        inheritanceSet.add(superName);
        inheritanceSet.addAll(Arrays.asList(interfaces));
        className = name;
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        FieldVisitor fieldVisitor = super.visitField(access, name, descriptor, signature, value);
        if ((access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) != 0) {
            inheritableFields.computeIfAbsent(className, k -> new HashSet<>()).add(new MemberRef(name, descriptor));
        }
        return fieldVisitor;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
        if ((access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) != 0) {
            inheritableMethods.computeIfAbsent(className, k -> new HashSet<>()).add(new MemberRef(name, descriptor));
        }
        return methodVisitor;
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
    public Set<MemberRef> getInheritableFields(String name) {
        return inheritableFields.getOrDefault(name, Collections.emptySet());
    }

    @Override
    public Set<MemberRef> getInheritableMethods(String name) {
        return inheritableMethods.getOrDefault(name, Collections.emptySet());
    }
}
