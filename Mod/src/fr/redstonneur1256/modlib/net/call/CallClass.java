package fr.redstonneur1256.modlib.net.call;

import arc.struct.Seq;

public class CallClass<T> {

    private Class<T> type;
    private T implementation;
    private Seq<CallMethod> methods;

    public CallClass(Class<T> type, T implementation, Seq<CallMethod> methods) {
        this.type = type;
        this.implementation = implementation;
        this.methods = methods;
    }

    public Class<T> getType() {
        return type;
    }

    public T getImplementation() {
        return implementation;
    }

    public int getMethodCount() {
        return methods.size;
    }

    public Seq<CallMethod> getMethods() {
        return methods;
    }

}
