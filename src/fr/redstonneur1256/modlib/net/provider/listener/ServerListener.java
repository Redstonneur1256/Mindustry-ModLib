package fr.redstonneur1256.modlib.net.provider.listener;

import arc.func.Cons2;
import fr.redstonneur1256.modlib.net.provider.MNet;
import mindustry.net.NetConnection;

public class ServerListener<T> {

    private MNet net;
    private Class<T> type;
    private Cons2<NetConnection, T> listener;

    public ServerListener(MNet net, Class<T> type, Cons2<NetConnection, T> listener) {
        this.net = net;
        this.type = type;
        this.listener = listener;
    }

    public void unregister() {
        net.unregisterServer(type, listener);
    }

    public MNet getNet() {
        return net;
    }

    public Class<T> getType() {
        return type;
    }

    public Cons2<NetConnection, T> getListener() {
        return listener;
    }

}
