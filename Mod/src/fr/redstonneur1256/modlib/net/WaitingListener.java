package fr.redstonneur1256.modlib.net;

import arc.func.Cons;
import fr.redstonneur1256.modlib.net.packet.MPacket;

import java.util.concurrent.ScheduledFuture;

public class WaitingListener<T extends MPacket> {

    private Class<T> type;
    private Cons<T> callback;
    private ScheduledFuture<?> timeoutTask;

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

    public ScheduledFuture<?> getTimeoutTask() {
        return timeoutTask;
    }

    public void setTimeoutTask(ScheduledFuture<?> timeoutTask) {
        this.timeoutTask = timeoutTask;
    }

}
