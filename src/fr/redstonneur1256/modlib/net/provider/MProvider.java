package fr.redstonneur1256.modlib.net.provider;

import arc.Core;
import arc.Events;
import arc.func.Cons;
import arc.func.Prov;
import arc.net.Client;
import arc.net.Server;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Time;
import arc.util.async.Threads;
import arc.util.pooling.Pools;
import fr.redstonneur1256.modlib.events.net.client.ServerConnectEvent;
import fr.redstonneur1256.modlib.events.net.server.ServerListPingEvent;
import fr.redstonneur1256.modlib.net.PacketManager;
import fr.redstonneur1256.modlib.net.ServerPinger;
import fr.redstonneur1256.modlib.net.serializer.PacketSerializer;
import mindustry.Vars;
import mindustry.net.Host;
import mindustry.net.Net;
import mindustry.net.NetworkIO;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class MProvider implements Net.NetProvider {

    private Client client;
    private Server server;
    private ServerPinger pinger;
    protected List<MArcConnection> connections;
    protected ScheduledExecutorService executor;

    public MProvider() {
        this.client = new Client(8192, 8192, new PacketSerializer());
        Prov<DatagramPacket> packetSupplier = () -> new DatagramPacket(new byte[512], 512);
        this.server = new Server(32768, 8192, new PacketSerializer());
        this.pinger = new ServerPinger();
        this.connections = new CopyOnWriteArrayList<>();
        this.executor = Executors.newSingleThreadScheduledExecutor();

        client.addListener(new MClientListener());
        client.setDiscoveryPacket(packetSupplier);

        server.addListener(new MServerListener(this));
        server.setMulticast(Vars.multicastGroup, Vars.multicastPort);
        server.setDiscoveryHandler((address, handler) -> {

            ServerListPingEvent event = Pools.obtain(ServerListPingEvent.class, ServerListPingEvent::new);
            event.address = address;
            event.offline = false;
            event.setDefaults();
            Events.fire(event);

            if(!event.offline) {
                ByteBuffer buffer = event.writeServerData();
                buffer.position(0);
                handler.respond(buffer);
            }

            Pools.free(event);
        });
    }

    @Override
    public void connectClient(String ip, int port, Runnable success) {
        Threads.daemon(() -> {
            try {
                //just in case
                client.stop();

                Threads.daemon("Net Client", () -> {
                    try {
                        client.run();
                    }catch(Exception exception) {
                        if(!(exception instanceof ClosedSelectorException)) {
                            Vars.net.handleException(exception);
                        }
                    }
                });

                Events.fire(new ServerConnectEvent(ip, port));
                PacketManager.onClientConnect();

                client.connect(5000, ip, port, port);
                success.run();
            }catch(Exception exception) {
                Vars.net.handleException(exception);
            }
        });
    }

    @Override
    public void disconnectClient() {
        client.close();
    }

    @Override
    public void sendClient(Object object, Net.SendMode mode) {
        try {
            if(!PacketManager.isAvailableClient(object.getClass())) {
                return;
            }

            if(mode == Net.SendMode.tcp) {
                client.sendTCP(object);
            }else {
                client.sendUDP(object);
            }
            //sending things can cause an under/overflow, catch it and disconnect instead of crashing
        }catch(BufferOverflowException | BufferUnderflowException exception) {
            Vars.net.showError(exception);
        }
        Pools.free(object);
    }

    @Override
    public void discoverServers(Cons<Host> callback, Runnable done) {
        Seq<InetAddress> foundAddresses = new Seq<>();
        long time = Time.millis();
        client.discoverHosts(Vars.port, Vars.multicastGroup, Vars.multicastPort, 3000, packet -> Core.app.post(() -> {
            try {
                if(foundAddresses.contains(address -> address.equals(packet.getAddress()) || (isLocal(address) && isLocal(packet.getAddress())))) {
                    return;
                }
                ByteBuffer buffer = ByteBuffer.wrap(packet.getData());
                Host host = NetworkIO.readServerData((int) Time.timeSinceMillis(time), packet.getAddress().getHostAddress(), buffer);
                callback.get(host);
                foundAddresses.add(packet.getAddress());
            }catch(Exception exception) {
                //don't crash when there's an error pinging a server or parsing data
                Log.err("Failed to discover server", exception);
            }
        }), () -> Core.app.post(done));
    }

    @Override
    public void pingHost(String address, int port, Cons<Host> valid, Cons<Exception> failed) {
        pinger.ping(address, port, valid, failed);
    }

    @Override
    public void hostServer(int port) throws IOException {
        PacketManager.onServerHost();

        connections.clear();
        server.bind(port, port);

        Threads.daemon("Net Server", () -> {
            try {
                server.run();
            }catch(Throwable throwable) {
                if(!(throwable instanceof ClosedSelectorException)) {
                    Threads.throwAppException(throwable);
                }
            }
        });
    }

    @Override
    public void closeServer() {
        connections.clear();
        Threads.daemon(server::stop);
    }

    @Override
    public Iterable<MArcConnection> getConnections() {
        return connections;
    }

    @Override
    public void dispose() {
        disconnectClient();
        closeServer();
        try {
            client.dispose();
        }catch(IOException ignored) {
        }
    }

    public MArcConnection getByArcID(int id) {
        //noinspection ForLoopReplaceableByForEach
        for(int i = 0; i < connections.size(); i++) {
            MArcConnection connection = connections.get(i);
            if(connection.connection != null && connection.connection.getID() == id) {
                return connection;
            }
        }
        return null;
    }

    private static boolean isLocal(InetAddress address) {
        if(address.isAnyLocalAddress() || address.isLoopbackAddress()) return true;

        try {
            return NetworkInterface.getByInetAddress(address) != null;
        }catch(Exception e) {
            return false;
        }
    }

}
