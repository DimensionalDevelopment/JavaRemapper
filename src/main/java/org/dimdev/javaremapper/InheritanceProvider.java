package org.dimdev.javaremapper;

import java.util.Set;

public interface InheritanceProvider {
    Set<String> getSuperclasses(String name);
    Set<String> getAllSuperclasses(String name);
    Set<MemberRef> getInheritableMethods(String name);
    Set<MemberRef> getInheritableFields(String name);
}
