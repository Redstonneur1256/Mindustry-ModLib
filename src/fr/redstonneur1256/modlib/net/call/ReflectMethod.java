package fr.redstonneur1256.modlib.net.call;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectMethod {

    private Object methodOwner;
    private Method method;

    public ReflectMethod(Object methodOwner, Method method) {
        this.methodOwner = methodOwner;
        this.method = method;
    }

    public Object invoke(Object[] args) throws InvocationTargetException, IllegalAccessException {
        return method.invoke(methodOwner, args);
    }

    public String getName() {
        return method.getName();
    }

    public Class<?>[] getParameterTypes() {
        return method.getParameterTypes();
    }

    public Object getMethodOwner() {
        return methodOwner;
    }

    public Method getMethod() {
        return method;
    }

}
