package fr.redstonneur1256.modlib.net.call;

public class MethodInfo {

    private int id;
    private String name;
    private Class<?>[] arguments;
    private ReflectMethod method;

    public MethodInfo(int id, String name, Class<?>[] arguments) {
        this.id = id;
        this.name = name;
        this.arguments = arguments;
    }

    public MethodInfo(int id, String name, Class<?>[] arguments, ReflectMethod method) {
        this.id = id;
        this.name = name;
        this.arguments = arguments;
        this.method = method;
    }

    public int getMethodID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Class<?>[] getArguments() {
        return arguments;
    }

    public ReflectMethod getMethod() {
        return method;
    }

    public void setMethod(ReflectMethod method) {
        this.method = method;
    }

}
