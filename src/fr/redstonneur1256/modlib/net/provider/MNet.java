package fr.redstonneur1256.modlib.net.provider;

import arc.Core;
import arc.Events;
import arc.func.Cons;
import arc.func.Cons2;
import arc.net.ArcNetException;
import arc.struct.IntMap;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Strings;
import arc.util.Time;
import arc.util.pooling.Pools;
import fr.redstonneur1256.modlib.events.net.NetExceptionEvent;
import fr.redstonneur1256.modlib.net.packets.StreamBeginPacket;
import fr.redstonneur1256.modlib.net.provider.listener.ClientListener;
import fr.redstonneur1256.modlib.net.provider.listener.ServerListener;
import mindustry.Vars;
import mindustry.gen.Call;
import mindustry.net.*;
import mindustry.net.Packets.KickReason;
import mindustry.net.Packets.StreamChunk;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.channels.ClosedChannelException;

@SuppressWarnings("unchecked")
public class MNet extends Net {

    private boolean server;
    private boolean active;
    private boolean clientLoaded;
    private StreamBuilder currentStream;

    private final Seq<Object> packetQueue = new Seq<>();
    private final ObjectMap<Class<?>, Seq<Cons<?>>> clientListeners = new ObjectMap<>();
    private final ObjectMap<Class<?>, Seq<Cons2<NetConnection, Object>>> serverListeners = new ObjectMap<>();
    private final IntMap<StreamBuilder> streams = new IntMap<>();

    private final Net.NetProvider provider;
    private final LZ4FastDecompressor decompressor = LZ4Factory.fastestInstance().fastDecompressor();
    private final LZ4Compressor compressor = LZ4Factory.fastestInstance().fastCompressor();

    public MNet(Net.NetProvider provider) {
        super(null);
        this.provider = provider;
    }

    @Override
    public void handleException(Throwable throwable) {
        Events.fire(new NetExceptionEvent(throwable));

        if(throwable instanceof ArcNetException) {
            Core.app.post(() -> showError(new IOException("mismatch")));
        }else if(throwable instanceof ClosedChannelException) {
            Core.app.post(() -> showError(new IOException("alreadyconnected")));
        }else {
            Core.app.post(() -> showError(throwable));
        }
    }

    /**
     * Display a network error. Call on the graphics thread.
     */
    @Override
    public void showError(Throwable throwable) {
        if(!Vars.headless) {
            Throwable t = throwable;
            while(t.getCause() != null) {
                t = t.getCause();
            }

            String baseError = Strings.getFinalMessage(throwable);

            String error = baseError == null ? "" : baseError.toLowerCase();
            String type = t.getClass().toString().toLowerCase();
            boolean isError = false;

            if(throwable instanceof BufferUnderflowException || throwable instanceof BufferOverflowException) {
                error = Core.bundle.get("error.io");
            }else if(error.equals("mismatch")) {
                error = Core.bundle.get("error.mismatch");
            }else if(error.contains("port out of range") || error.contains("invalid argument") || (error.contains("invalid") && error.contains("address")) || Strings.neatError(throwable).contains("address associated")) {
                error = Core.bundle.get("error.invalidaddress");
            }else if(error.contains("connection refused") || error.contains("route to host") || type.contains("unknownhost")) {
                error = Core.bundle.get("error.unreachable");
            }else if(type.contains("timeout")) {
                error = Core.bundle.get("error.timedout");
            }else if(error.equals("alreadyconnected") || error.contains("connection is closed")) {
                error = Core.bundle.get("error.alreadyconnected");
            }else if(!error.isEmpty()) {
                error = Core.bundle.get("error.any");
                isError = true;
            }

            if(isError) {
                Vars.ui.showException("@error.any", throwable);
            }else {
                Vars.ui.showText("", Core.bundle.format("connectfail", error));
            }
            Vars.ui.loadfrag.hide();

            if(client()) {
                Vars.netClient.disconnectQuietly();
            }
        }

        Log.err("Raw network error:", throwable);
    }

    /**
     * Sets the client loaded status, or whether it will receive normal packets from the server.
     */
    @Override
    public void setClientLoaded(boolean loaded) {
        clientLoaded = loaded;

        if(loaded) {
            //handle all packets that were skipped while loading
            for(int i = 0; i < packetQueue.size; i++) {
                handleClientReceived(packetQueue.get(i));
            }
        }
        //clear inbound packet queue
        packetQueue.clear();
    }

    @Override
    public void setClientConnected() {
        active = true;
        server = false;
    }

    /**
     * Connect to an address.
     */
    @Override
    public void connect(String ip, int port, Runnable success) {
        try {
            if(!active) {
                provider.connectClient(ip, port, success);
                active = true;
                server = false;
            }else {
                throw new IOException("alreadyconnected");
            }
        }catch(IOException e) {
            showError(e);
        }
    }

    /**
     * Host a server at an address.
     */
    @Override
    public void host(int port) throws IOException {
        provider.hostServer(port);
        active = true;
        server = true;

        Time.runTask(60, Vars.platform::updateRPC);
    }

    /**
     * Closes the server.
     */
    @Override
    public void closeServer() {
        for(NetConnection con : getConnections()) {
            Call.kick(con, KickReason.serverClose);
        }

        provider.closeServer();
        server = false;
        active = false;
    }

    @Override
    public void reset() {
        closeServer();
        Vars.netClient.disconnectNoReset();
    }

    @Override
    public void disconnect() {
        if(active && !server) {
            Log.info("Disconnecting.");
        }
        provider.disconnectClient();
        server = false;
        active = false;
    }

    @Override
    public byte[] compressSnapshot(byte[] input) {
        return compressor.compress(input);
    }

    @Override
    public byte[] decompressSnapshot(byte[] input, int size) {
        return decompressor.decompress(input, size);
    }

    /**
     * Starts discovering servers on a different thread.
     * Callback is run on the main Arc thread.
     */
    @Override
    public void discoverServers(Cons<Host> cons, Runnable done) {
        provider.discoverServers(cons, done);
    }

    /**
     * Returns a list of all connections IDs.
     */
    @Override
    public Iterable<NetConnection> getConnections() {
        return (Iterable<NetConnection>) provider.getConnections();
    }

    /**
     * Send an object to all connected clients, or to the server if this is a client.
     */
    @Override
    public void send(Object object, Net.SendMode mode) {
        if(server) {
            for(NetConnection con : provider.getConnections()) {
                con.send(object, mode);
            }
        }else {
            provider.sendClient(object, mode);
        }
    }

    /**
     * Send an object to everyone EXCEPT a certain client. Server-side only.
     */
    @Override
    public void sendExcept(NetConnection except, Object object, Net.SendMode mode) {
        for(NetConnection con : getConnections()) {
            if(con != except) {
                con.send(object, mode);
            }
        }
    }

    @Override
    public Streamable.StreamBuilder getCurrentStream() {
        return currentStream;
    }

    /**
     * Registers a client listener for when an object is received.
     */
    @Override
    public <T> void handleClient(Class<T> type, Cons<T> listener) {
        clientListeners.get(type, Seq::new).add(listener);
    }

    public <T> ClientListener<T> registerClient(Class<T> type, Cons<T> listener) {
        handleClient(type, listener);
        return new ClientListener<>(this, type, listener);
    }

    public <T> void unregisterClient(Class<T> type, Cons<T> listener) {
        clientListeners.get(type, Seq::new).remove(listener);
    }

    /**
     * Registers a server listener for when an object is received.
     */
    @Override
    public <T> void handleServer(Class<T> type, Cons2<NetConnection, T> listener) {
        serverListeners.get(type, Seq::new).add((Cons2<NetConnection, Object>) listener);
    }

    public <T> ServerListener<T> registerServer(Class<T> type, Cons2<NetConnection, T> listener) {
        handleServer(type, listener);
        return new ServerListener<>(this, type, listener);
    }

    public <T> void unregisterServer(Class<T> type, Cons2<NetConnection, T> listener) {
        serverListeners.get(type, Seq::new).remove((Cons2<NetConnection, Object>) listener);
    }

    /**
     * Call to handle a packet being received for the client.
     */
    @Override
    public void handleClientReceived(Object object) {
        if(!(object instanceof Packet)) {
            return;
        }
        Packet packet = (Packet) object;

        if(object instanceof StreamBeginPacket) {
            StreamBeginPacket begin = (StreamBeginPacket) object;
            streams.put(begin.id, currentStream = new StreamBuilder(begin));
        }else if(object instanceof StreamChunk) {
            StreamChunk chunk = (StreamChunk) object;
            StreamBuilder builder = streams.get(chunk.id);
            if(builder == null) {
                throw new RuntimeException("Received stream chunk without a StreamBegin beforehand!");
            }
            builder.add(chunk.data);
            if(builder.isDone()) {
                streams.remove(builder.id);
                handleClientReceived(builder.build());
                currentStream = null;
            }
        }else if(clientListeners.get(object.getClass()) != null) {
            if(clientLoaded || packet.isImportant()) {
                clientListeners.get(object.getClass()).forEach(o -> ((Cons<Object>) o).get(object));
                Pools.free(object);
            }else if(!packet.isUnimportant()) {
                packetQueue.add(object);
            }else {
                Pools.free(object);
            }
        }else {
            Log.err("Unhandled packet type: '@'!", object);
        }
    }

    /**
     * Call to handle a packet being received for the server.
     */
    @Override
    public void handleServerReceived(NetConnection connection, Object object) {
        Seq<Cons2<NetConnection, Object>> listeners = serverListeners.get(object.getClass());
        if(listeners != null) {
            listeners.forEach(cons -> cons.get(connection, object));
            Pools.free(object);
        }else {
            Log.err("Unhandled packet type: '@'!", object.getClass());
        }
    }

    /**
     * Pings a host in a new thread. If an error occurred, failed() should be called with the exception.
     */
    @Override
    public void pingHost(String address, int port, Cons<Host> valid, Cons<Exception> failed) {
        provider.pingHost(address, port, valid, failed);
    }

    /**
     * Whether the net is active, e.g. whether this is a multiplayer game.
     */
    @Override
    public boolean active() {
        return active;
    }

    /**
     * Whether this is a server or not.
     */
    @Override
    public boolean server() {
        return server && active;
    }

    /**
     * Whether this is a client or not.
     */
    @Override
    public boolean client() {
        return !server && active;
    }

    @Override
    public void dispose() {
        provider.dispose();
        server = false;
        active = false;
    }

}
