package fr.redstonneur1256.modlib.net.call;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class CallMethod {

    private CallClass<?> callClass;
    private Side side;
    private Execution execution;
    private Method method;
    private int id;

    public CallMethod(CallClass<?> callClass, Side side, Execution execution, Method method) {
        this.callClass = callClass;
        this.side = side;
        this.execution = execution;
        this.method = method;
    }

    public Object invoke(Object[] arguments) throws InvocationTargetException, IllegalAccessException {
        return method.invoke(callClass.getImplementation(), arguments);
    }

    public Side getSide() {
        return side;
    }

    public Execution getExecution() {
        return execution;
    }

    public String getName() {
        return method.getName();
    }

    public Class<?>[] getParameters() {
        return method.getParameterTypes();
    }

    public Method getMethod() {
        return method;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

}
