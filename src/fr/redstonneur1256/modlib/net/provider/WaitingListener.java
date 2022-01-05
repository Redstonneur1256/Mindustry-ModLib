package fr.redstonneur1256.modlib.net.provider;

import arc.func.Cons;
import fr.redstonneur1256.modlib.net.IPacket;

import java.util.concurrent.ScheduledFuture;

public class WaitingListener<T extends IPacket> {

    private Class<T> type;
    private Cons<T> callback;
    protected ScheduledFuture<?> timeoutTask;

    public WaitingListener(Class<T> type, Cons<T> callback) {
        this.type = type;
        this.callback = callback;
    }

    public Class<T> getType() {
        return type;
    }

    public Cons<T> getCallback() {
        return callback;
    }

}
