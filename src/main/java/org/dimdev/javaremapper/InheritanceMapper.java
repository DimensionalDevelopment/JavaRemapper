package org.dimdev.javaremapper;

import org.objectweb.asm.*;

import java.io.IOException;
import java.io.InputStream;
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
        if (superName != null) inheritanceSet.add(superName); // java/lang/Object has a null superclass
        inheritanceSet.addAll(Arrays.asList(interfaces));
        if ((access & Opcodes.ACC_ENUM) != 0) inheritanceSet.add("java/lang/Enum");
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
        Set<String> result = inheritanceMap.get(name);
        if (result == null) {
            inheritanceMap.put(name, new HashSet<>());
            inheritableFields.put(name, new HashSet<>());
            inheritableMethods.put(name, new HashSet<>());
            visitClasspathClass(name);
            result = inheritanceMap.get(name);
        }
        return result;
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
        Set<MemberRef> result = inheritableFields.get(name);
        if (result == null) {
            inheritanceMap.put(name, new HashSet<>());
            inheritableFields.put(name, new HashSet<>());
            inheritableMethods.put(name, new HashSet<>());
            visitClasspathClass(name);
            result = inheritableFields.get(name);
        }
        return result;
    }

    @Override
    public Set<MemberRef> getInheritableMethods(String name) {
        Set<MemberRef> result = inheritableMethods.get(name);
        if (result == null) {
            inheritanceMap.put(name, new HashSet<>());
            inheritableFields.put(name, new HashSet<>());
            inheritableMethods.put(name, new HashSet<>());
            visitClasspathClass(name);
            result = inheritableMethods.get(name);
        }
        return result;
    }

    private void visitClasspathClass(String name) {
        try (InputStream inputStream = InheritanceMapper.class.getClassLoader().getResourceAsStream(name + ".class")) {
            if (inputStream == null) return;
            ClassReader reader = new ClassReader(inputStream);
            reader.accept(this, 0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
