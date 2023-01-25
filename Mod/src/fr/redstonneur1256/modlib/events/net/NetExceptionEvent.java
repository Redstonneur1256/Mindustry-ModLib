package fr.redstonneur1256.modlib.events.net;

public class NetExceptionEvent {

    public final Throwable throwable;

    public NetExceptionEvent(Throwable throwable) {
        this.throwable = throwable;
    }

}
