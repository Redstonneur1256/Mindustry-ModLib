package fr.redstonneur1256.modlib.net;

import arc.func.Prov;

public class ClassEntry<T> {

    public Class<T> type;
    public Prov<T> constructor;

    public ClassEntry(Class<T> type, Prov<T> constructor) {
        this.type = type;
        this.constructor = constructor;
    }

    @Override
    public String toString() {
        return "ClassEntry{" + type.getSimpleName() + '}';
    }
}
