package fr.redstonneur1256.modlib.net.provider.listener;

import arc.func.Cons;
import fr.redstonneur1256.modlib.net.provider.MNet;

public class ClientListener<T> {

    private MNet net;
    private Class<T> type;
    private Cons<T> listener;

    public ClientListener(MNet net, Class<T> type, Cons<T> listener) {
        this.net = net;
        this.type = type;
        this.listener = listener;
    }

    public void unregister() {
        net.unregisterClient(type, listener);
    }

    public MNet getNet() {
        return net;
    }

    public Class<T> getType() {
        return type;
    }

    public Cons<T> getListener() {
        return listener;
    }

}
