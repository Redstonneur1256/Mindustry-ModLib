package fr.redstonneur1256.modlib.net.call;

import fr.redstonneur1256.modlib.MVars;
import fr.redstonneur1256.modlib.net.packets.CustomInvokePacket;
import fr.redstonneur1256.modlib.net.packets.CustomInvokeResultPacket;
import fr.redstonneur1256.modlib.net.provider.MArcConnection;
import mindustry.gen.Player;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class InvokeHandler implements InvocationHandler {

    private Class<?> type;
    private CallManager manager;

    public InvokeHandler(Class<?> type, CallManager manager) {
        this.type = type;
        this.manager = manager;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        switch(method.getName()) {
            case "hashCode":
                return 0;
            case "equals":
                return false;
            case "toString":
                return "RemoteCall" + type.getName();
            default:
                int id = manager.getMethodID(method);
                if(id == -1) {
                    throw new NoSuchMethodError();
                }

                CallResult<?> result = new CallResult<>();

                if(MVars.net.server()) {
                    Player player = args.length >= 1 && args[0] instanceof Player ? (Player) args[0] : null;
                    if(player == null) {
                        throw new NoSuchMethodError("Can not call method " + method + " from server with no player");
                    }
                    MArcConnection con = (MArcConnection) player.con;
                    con.sendPacket(new CustomInvokePacket(id, args), CustomInvokeResultPacket.class, result::complete);
                } else {
                    MVars.net.sendPacket(new CustomInvokePacket(id, args), CustomInvokeResultPacket.class, result::complete);
                }

                return result;
        }
    }

}
