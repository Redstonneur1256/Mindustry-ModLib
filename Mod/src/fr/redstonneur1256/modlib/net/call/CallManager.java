package fr.redstonneur1256.modlib.net.call;

import arc.Events;
import arc.func.Cons;
import arc.struct.ObjectIntMap;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;
import fr.redstonneur1256.modlib.MVars;
import fr.redstonneur1256.modlib.events.net.client.ServerConnectEvent;
import fr.redstonneur1256.modlib.events.net.server.PreServerHostEvent;
import fr.redstonneur1256.modlib.net.io.MTypeIO;
import fr.redstonneur1256.modlib.net.packet.MConnection;
import fr.redstonneur1256.modlib.net.packets.CustomInvokePacket;
import fr.redstonneur1256.modlib.net.packets.CustomInvokeResultPacket;
import mindustry.Vars;
import mindustry.gen.Player;
import mindustry.net.NetConnection;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

@SuppressWarnings("unchecked")
public class CallManager {

    private ObjectMap<String, CallClass<?>> registeredClasses;
    private ObjectMap<Class<?>, Object> proxyCache;
    private Seq<Class<?>> activeClasses;
    private Seq<CallMethod> activeMethods;
    private ObjectIntMap<Method> methodIds;

    public CallManager() {
        registeredClasses = new ObjectMap<>();
        proxyCache = new ObjectMap<>();
        activeClasses = new Seq<>();
        activeMethods = new Seq<>();
        methodIds = new ObjectIntMap<>();

        Events.on(ServerConnectEvent.class, event -> {
            Log.debug("Connecting to server, clearing call methods");
            activeClasses.clear();
            activeMethods.clear();
            methodIds.clear();
        });
        Events.on(PreServerHostEvent.class, event -> {
            Log.debug("Starting server, attributing all call methods");
            resetMethods();
        });

        Vars.net.handleClient(CustomInvokePacket.class, this::callMethod);
        Vars.net.handleServer(CustomInvokePacket.class, this::callMethod);
    }

    public <T> void registerCall(Class<T> type, T implementation) {
        if (Vars.net.active()) {
            throw new IllegalStateException("Cannot register new call classes while connected to a server or hosting a server");
        }

        Seq<CallMethod> methods = new Seq<>();
        CallClass<T> callClass = new CallClass<>(type, implementation, methods);

        for (Method method : type.getDeclaredMethods()) {
            Class<?> returnType = method.getReturnType();
            if (!returnType.equals(CallResult.class) && !returnType.equals(void.class)) {
                Log.warn("The method @#@ does not return CallResult or void and will be ignored", type.getName(), method.getName());
                continue;
            }

            Side side = Side.BOTH;
            Execution execution = Execution.MAIN;

            Remote remote = method.getAnnotation(Remote.class);
            if (remote != null) {
                side = remote.side();
                execution = remote.execution();
            }

            Class<?>[] parameters = method.getParameterTypes();
            if ((side == Side.SERVER || side == Side.BOTH) && (parameters.length == 0 || parameters[0] != Player.class)) {
                Log.warn("The method @#@ is declared to be available on server side but the first parameter is not Player, it will be ignored", type.getName(), method.getName());
                continue;
            }

            methods.add(new CallMethod(callClass, side, execution, method));
        }

        registeredClasses.put(type.getName(), callClass);
    }

    public <T> boolean isCallAvailable(Class<T> type) {
        return activeClasses.contains(type);
    }

    public <T> T getCall(Class<T> type) {
        return (T) proxyCache.get(type, () -> createProxy(type));
    }

    private <T> T createProxy(Class<T> type) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[] { type }, new CallProxyHandler(type, this));
    }

    // Called upon server start to reset active methods and assign them an ID
    private void resetMethods() {
        activeClasses.clear();
        activeMethods.clear();
        methodIds.clear();

        int i = 0;
        for (CallClass<?> callClass : registeredClasses.values()) {
            activeClasses.add(callClass.getType());
            activeMethods.ensureCapacity(callClass.getMethodCount());
            methodIds.ensureCapacity(callClass.getMethodCount());

            for (CallMethod method : callClass.getMethods()) {
                method.setId(i++);

                activeMethods.add(method);
                methodIds.put(method.getMethod(), method.getId());
            }
        }
    }

    // Called on server-side when synchronizing all methods to clients
    public void writeMethods(DataOutput stream) throws IOException {
        stream.writeInt(registeredClasses.size);

        for (CallClass<?> callClass : registeredClasses.values()) {
            stream.writeUTF(callClass.getType().getName());

            stream.writeInt(callClass.getMethods().size);
            for (CallMethod method : callClass.getMethods()) {
                stream.writeInt(method.getId());
                stream.writeUTF(method.getName());
                stream.writeInt(method.getParameters().length);
                for (Class<?> parameter : method.getParameters()) {
                    stream.writeUTF(parameter.getName());
                }
            }
        }
    }

    // Called on client-side to read methods sent by the server
    public void readMethods(DataInput stream) throws IOException {
        activeClasses.clear();
        activeMethods.clear();
        methodIds.clear();

        int classCount = stream.readInt();
        for (int i = 0; i < classCount; i++) {
            String className = stream.readUTF();

            int methodCount = stream.readInt();
            Seq<MethodSignature> methods = new Seq<>(methodCount);

            for (int j = 0; j < methodCount; j++) {
                int id = stream.readInt();
                String name = stream.readUTF();
                String[] parameters = new String[stream.readInt()];
                for (int h = 0; h < parameters.length; h++) {
                    parameters[h] = stream.readUTF();
                }
                methods.add(new MethodSignature(id, name, parameters));
            }

            CallClass<?> callClass = registeredClasses.get(className);
            if (callClass == null) {
                Log.warn("Call class @ is present on server but not on client", className);
                continue;
            }

            activeClasses.add(callClass.getType());

            for (MethodSignature signature : methods) {
                String name = null;
                try {
                    Class<?>[] parameters = new Class<?>[signature.getParameterCount()];
                    for (int j = 0; j < parameters.length; j++) {
                        name = signature.getParametersName()[j];
                        parameters[j] = MTypeIO.getType(name);
                    }

                    CallMethod method = callClass.getMethods().find(m -> m.getName().equals(signature.getName()) && Arrays.equals(m.getParameters(), parameters));
                    if (method == null) {
                        activeMethods.add((CallMethod) null);
                        Log.warn("Could not resolve method @ with parameters @ in class @", signature.getName(), Arrays.toString(parameters), className);
                        continue;
                    }

                    method.setId(signature.getId());

                    activeMethods.add(method);
                    methodIds.put(method.getMethod(), method.getId());
                } catch (ClassNotFoundException exception) {
                    activeMethods.add((CallMethod) null);
                    Log.warn("Method @ in class @ contains invalid type @", signature.getName(), className, name);
                }
            }
        }
    }

    protected int getMethodId(Method method) {
        return methodIds.get(method, -1);
    }

    private void callMethod(CustomInvokePacket packet) {
        invokeMethod(null, packet, result -> MVars.net.sendReply(packet, result));
    }

    private void callMethod(NetConnection conn, CustomInvokePacket packet) {
        invokeMethod(conn.player, packet, result -> ((MConnection) conn).sendReply(packet, result));
    }

    private void invokeMethod(Player player, CustomInvokePacket packet, Cons<CustomInvokeResultPacket> consumer) {
        CallMethod method = activeMethods.get(packet.method);
        if (method == null) {
            consumer.get(new CustomInvokeResultPacket(null, new NoSuchMethodException()));
            return;
        }

        Object[] args = packet.arguments;
        if (player != null) {
            args[0] = player;
        }

        method.getExecution().execute(() -> {
            try {
                Object result = method.invoke(args);

                if (result instanceof CallResult) {
                    CallResult<?> callResult = (CallResult<?>) result;
                    callResult.onComplete(object -> consumer.get(new CustomInvokeResultPacket(object, null)));
                    callResult.onFail(object -> consumer.get(new CustomInvokeResultPacket(object, null)));
                    return;
                }

                consumer.get(new CustomInvokeResultPacket(result, null));
            } catch (Throwable exception) {
                Throwable throwable = exception;
                if (throwable instanceof InvocationTargetException) {
                    throwable = throwable.getCause();
                }

                consumer.get(new CustomInvokeResultPacket(null, throwable));
            }
        });
    }

    public ObjectMap<String, CallClass<?>> getRegisteredClasses() {
        return registeredClasses;
    }

    public ObjectMap<Class<?>, Object> getProxyCache() {
        return proxyCache;
    }

    public Seq<Class<?>> getActiveClasses() {
        return activeClasses;
    }

    public Seq<CallMethod> getActiveMethods() {
        return activeMethods;
    }

    public ObjectIntMap<Method> getMethodIds() {
        return methodIds;
    }

}
