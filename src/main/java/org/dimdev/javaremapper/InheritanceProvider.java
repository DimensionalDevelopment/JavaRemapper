package org.dimdev.javaremapper;

import java.util.Map;
import java.util.Set;

public interface InheritanceProvider {
    Set<String> getSuperclasses(String name);
    Set<String> getAllSuperclasses(String name);
    Map<String, Set<String>> getInheritanceMap();
}
