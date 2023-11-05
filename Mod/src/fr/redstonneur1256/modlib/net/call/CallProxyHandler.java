package fr.redstonneur1256.modlib.net.call;

import fr.redstonneur1256.modlib.MVars;
import fr.redstonneur1256.modlib.net.packet.MConnection;
import fr.redstonneur1256.modlib.net.packets.CustomInvokePacket;
import fr.redstonneur1256.modlib.net.packets.CustomInvokeResultPacket;
import mindustry.Vars;
import mindustry.gen.Player;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class CallProxyHandler implements InvocationHandler {

    private Class<?> type;
    private CallManager manager;

    public CallProxyHandler(Class<?> type, CallManager manager) {
        this.type = type;
        this.manager = manager;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        switch (method.getName()) {
            case "hashCode":
                return 0;
            case "equals":
                return false;
            case "toString":
                return "RemoteCall" + type.getName();
            default:
                int id = manager.getMethodId(method);
                if (id == -1) {
                    throw new NoSuchMethodError("Method " + method.getName() + " is not available.");
                }

                CallMethod callMethod = manager.getActiveMethods().get(id);
                if (!callMethod.getSide().available()) {
                    throw new IllegalStateException("Method " + method.getName() + " is only available to be called on side " + callMethod.getSide());
                }

                CallResult<?> result = new CallResult<>();

                if (Vars.net.server()) {
                    // The player on which to call this method
                    Player player = args[0] instanceof Player ? (Player) args[0] : null;
                    if (player == null) {
                        throw new NoSuchMethodError("In order to call a remote method from the server, the first argument of the method must be the player on which to call the method");
                    }
                    ((MConnection) player.con).sendPacket(new CustomInvokePacket(id, args), CustomInvokeResultPacket.class, result::complete);
                } else {
                    MVars.net.sendPacket(new CustomInvokePacket(id, args), CustomInvokeResultPacket.class, result::complete);
                }

                return result;
        }
    }

}
