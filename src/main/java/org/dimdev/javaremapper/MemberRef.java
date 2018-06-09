package org.dimdev.javaremapper;

public class MemberRef {
    public String name;
    public String descriptor;

    public MemberRef(String name, String descriptor) {
        this.name = name;
        this.descriptor = descriptor;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        return ((MemberRef) obj).name.equals(name) && ((MemberRef) obj).descriptor.equals(descriptor);
    }

    @Override
    public int hashCode() {
        return name.hashCode() ^ descriptor.hashCode();
    }
}
