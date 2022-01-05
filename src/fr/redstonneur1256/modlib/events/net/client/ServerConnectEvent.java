package fr.redstonneur1256.modlib.events.net.client;

public class ServerConnectEvent {

    public final String address;
    public final int port;

    public ServerConnectEvent(String address, int port) {
        this.address = address;
        this.port = port;
    }

}
