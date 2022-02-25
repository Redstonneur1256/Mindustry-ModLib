package fr.redstonneur1256.modlib.net.call;

import arc.struct.ObjectIntMap;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;
import fr.redstonneur1256.modlib.MVars;
import fr.redstonneur1256.modlib.net.packets.CustomInvokePacket;
import fr.redstonneur1256.modlib.net.packets.CustomInvokeResultPacket;
import fr.redstonneur1256.modlib.net.provider.MArcConnection;
import mindustry.gen.Player;
import mindustry.net.NetConnection;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

@SuppressWarnings("unchecked")
public class CallManager {

    private Seq<ReflectMethod> registeredMethods;
    private ObjectMap<Class<?>, Object> callCache;
    private Seq<MethodInfo> idMethods;
    private ObjectIntMap<Method> methodIDs;

    public CallManager() {
        registeredMethods = new Seq<>();
        callCache = new ObjectMap<>();
        idMethods = new Seq<>();
        methodIDs = new ObjectIntMap<>();
    }

    public <T> void registerCall(Class<T> type, T implementation) {
        for(Method method : type.getDeclaredMethods()) {
            Class<?> returnType = method.getReturnType();
            if(!returnType.equals(CallResult.class) && !returnType.equals(void.class)) {
                Log.warn("The method @ does not return CallResult or void and will be ignored, calling it may result in a NoSuchMethodException",
                        method.getName());
                continue;
            }

            registeredMethods.add(new ReflectMethod(implementation, method));
        }
    }

    public <T> T getCall(Class<T> type) {
        return (T) callCache.get(type, () -> createProxy(type));
    }

    private <T> T createProxy(Class<T> type) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[] { type }, new InvokeHandler(type, this));
    }

    /**
     * Called when the server is starting to assign an ID to each registered methods
     */
    public void updateMethodIDs() {
        idMethods.clear();
        methodIDs.clear();

        for(ReflectMethod method : registeredMethods) {
            int id = idMethods.size;
            MethodInfo info = new MethodInfo(id, method.getName(), method.getParameterTypes(), method);
            idMethods.add(info);
            methodIDs.put(method.getMethod(), id);
        }
    }

    /**
     * Called when connecting as a client syncing method IDs
     */
    public void syncMethods(MethodInfo[] methods) {
        idMethods.clear();
        methodIDs.clear();

        for(MethodInfo info : methods) {
            while(idMethods.size <= info.getMethodID()) {
                idMethods.add(null);
            }

            ReflectMethod localMethod = registeredMethods.find(method ->
                    method.getName().equals(info.getName()) &&
                    Arrays.equals(method.getParameterTypes(), info.getArguments()));
            info.setMethod(localMethod);


            idMethods.set(info.getMethodID(), info);
            methodIDs.put(localMethod.getMethod(), info.getMethodID());
        }
    }

    protected int getMethodID(Method method) {
        return methodIDs.get(method, -1);
    }

    public void callMethod(CustomInvokePacket packet) {
        MVars.net.sendReply(packet, invokeMethod(null, packet));
    }

    public void callMethod(NetConnection conn, CustomInvokePacket packet) {
        MArcConnection connection = (MArcConnection) conn;

        connection.sendReply(packet, invokeMethod(conn.player, packet));
    }

    private CustomInvokeResultPacket invokeMethod(Player player, CustomInvokePacket packet) {
        MethodInfo info = idMethods.get(packet.method);
        ReflectMethod method = info.getMethod();
        if(method == null) {
            return new CustomInvokeResultPacket(null, new NoSuchMethodException());
        }

        Object[] args = packet.arguments;
        if(player != null) {
            args[0] = player;
        }

        try {
            Object result = method.invoke(args);
            Throwable exception = null;

            if(result instanceof CallResult) {
                CallResult<?> callResult = (CallResult<?>) result;
                if(!callResult.isCompleted()) {
                    throw new RuntimeException("Custom call results should instantly be filled (CallResult#of())");
                }
                result = callResult.isFailed() ? null : callResult.get();
                exception = callResult.isFailed() ? callResult.getException() : null;
            }

            return new CustomInvokeResultPacket(result, exception);
        } catch(Throwable exception) {
            return new CustomInvokeResultPacket(null, exception);
        }
    }

    public ObjectIntMap<Method> getMethodIDs() {
        return methodIDs;
    }

    public Seq<MethodInfo> getIdMethods() {
        return idMethods;
    }

    public Seq<ReflectMethod> getRegisteredMethods() {
        return registeredMethods;
    }

}
