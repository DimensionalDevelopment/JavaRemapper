package org.dimdev.javaremapper;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class JavaRemapper {
    public Mapping mapping;

    public JavaRemapper(Mapping mapping) {
        this.mapping = mapping;
    }

    public void remapJar(File inputFile, File remapTarget) throws IOException {
        // Make the inheritance map
        InheritanceMapper inheritanceMapper = new InheritanceMapper();
        try (JarFile jar = new JarFile(inputFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                InputStream in = jar.getInputStream(entry);

                // Visit the class and determine dependencies
                if (entry.getName().endsWith(".class")) {
                    ClassReader reader = new ClassReader(in);
                    reader.accept(inheritanceMapper, 0);
                }
            }
        }

        // Initialize the remapper using the mapping and inheritance provider
        SimpleRemapper remapper = new SimpleRemapper(mapping, inheritanceMapper);

        // Copy jar classes, remapping them if necessary
        try (JarFile jar = new JarFile(inputFile);
             FileOutputStream fileOutputStream = new FileOutputStream(remapTarget);
             JarOutputStream out = new JarOutputStream(fileOutputStream)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();

                // Read the entry
                InputStream in = jar.getInputStream(entry);
                String name = entry.getName();
                byte[] data = readStream(in);

                // Don't copy signatures
                if (name.endsWith(".DSA") || name.endsWith(".RSA") || name.endsWith(".EC") || name.endsWith(".SF")) {
                    continue;
                }

                // Remap classes
                if (name.endsWith(".class")) {
                    String className = name.substring(0, name.length() - 6);
                    data = remapClass(className, data, remapper);
                    name = remapClassName(className, remapper) + ".class";
                }

                // Write the new entry
                JarEntry newEntry = new JarEntry(name);
                out.putNextEntry(newEntry);
                out.write(data);
                out.closeEntry();

                System.out.println(entry.getName());
            }
        }
    }

    private String remapClassName(String name, Remapper remapper) {
        return remapper.map(name);
    }

    public byte[] remapClass(String name, byte[] data, Remapper remapper) {
        // Read the class
        ClassReader reader = new ClassReader(data);
        ClassNode clazz = new ClassNode();
        reader.accept(clazz, 0);

        // Rename inner class innerNames
        if (clazz.innerClasses != null) for (InnerClassNode innerClass : clazz.innerClasses) {
            if (innerClass.innerName != null) {
                String newName = mapping.mapClass(innerClass.name);
                innerClass.innerName = newName.substring(newName.indexOf('$') + 1);
            }
        }

        // Rename local variables and add parameters
        for (MethodNode method : clazz.methods) {
            boolean isStatic = (method.access & Opcodes.ACC_STATIC) != 0;
            int paramCount = Type.getArgumentsAndReturnSizes(method.desc) >> 2;

            // Add parameter names
            if (method.parameters == null) {
                method.parameters = new ArrayList<>();
                for (int index = 0; index < (isStatic ? paramCount : paramCount - 1); index++) { // TODO: implicit this?
                    method.parameters.add(new ParameterNode(mapping.mapParameter(name, method.name, method.desc, index), 0));
                }
            }

            int index = 0;
            HashMap<Integer, String> localNames = new HashMap<>();
            int varSuffix = 0;
            if (method.localVariables != null) for (LocalVariableNode local : method.localVariables) {
                // Name the local
                if (!isStatic && index == 0) {
                    local.name = "this";
                } else if (index < paramCount) {
                    local.name = method.parameters.get(isStatic ? index : index - 1).name;
                } else {
                    local.name = mapping.getLocal(name, method.name, method.desc, index);

                    // No mapping exists for that local, use name of previous local with
                    // same index, desc and signature, or assign a unique name.
                    int localHash = Objects.hash(local.index, local.desc, local.signature);
                    if (local.name == null) {
                        String localName = localNames.get(localHash);
                        if (localName != null) {
                            local.name = localName;
                        } else {
                            local.name = "var" + varSuffix++;
                            localNames.put(localHash, local.name);
                        }
                    } else {
                        localNames.put(localHash, local.name);
                    }
                }

                // Fix broken local start/end
                if (local.start == local.end) { // TODO: Why does this happen?
                    local.start = (LabelNode) method.instructions.getFirst();
                    local.end = (LabelNode) method.instructions.getFirst();
                }

                index++;
            }
        }

        // Write the transformed class
        ClassWriter writer = new ClassWriter(0);
        clazz.accept(writer);
        data = writer.toByteArray();

        // TODO: Do this properly, use a single CustomClassRemapper rather than a class node followed by a ClassRemapper

        reader = new ClassReader(data);
        reader.accept(clazz, 0);

        ClassNode node = new ClassNode();
        ClassRemapper classRemapper = new ClassRemapper(node, remapper);
        reader.accept(classRemapper, 0);

        writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static byte[] readStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
            outputStream.write(data, 0, bytesRead);
        }
        outputStream.flush();
        return outputStream.toByteArray();
    }
}
