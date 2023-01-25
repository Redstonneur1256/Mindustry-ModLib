package fr.redstonneur1256.modlib.net.call;

public class MethodSignature {

    private int id;
    private String name;
    private String[] parametersName;

    public MethodSignature(int id, String name, String[] parametersName) {
        this.id = id;
        this.name = name;
        this.parametersName = parametersName;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getParameterCount() {
        return parametersName.length;
    }

    public String[] getParametersName() {
        return parametersName;
    }

}
