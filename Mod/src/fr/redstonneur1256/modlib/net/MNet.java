package fr.redstonneur1256.modlib.net;

import arc.Core;
import arc.Events;
import arc.func.Cons;
import arc.net.Server;
import arc.struct.IntMap;
import arc.util.Log;
import arc.util.Reflect;
import arc.util.Threads;
import arc.util.pooling.Pools;
import fr.redstonneur1256.modlib.events.net.PlayerDataSyncedEvent;
import fr.redstonneur1256.modlib.events.net.server.ServerListPingEvent;
import fr.redstonneur1256.modlib.net.call.CallManager;
import fr.redstonneur1256.modlib.net.io.MTypeIO;
import fr.redstonneur1256.modlib.net.packet.MConnection;
import fr.redstonneur1256.modlib.net.packet.MPacket;
import fr.redstonneur1256.modlib.net.packet.MPlayerConnection;
import fr.redstonneur1256.modlib.net.packet.PacketManager;
import fr.redstonneur1256.modlib.net.packets.DataAckPacket;
import fr.redstonneur1256.modlib.net.udp.UdpConnectionManager;
import fr.redstonneur1256.modlib.util.dns.SimpleDns;
import mindustry.Vars;
import mindustry.io.SaveVersion;
import mindustry.net.ArcNetProvider;
import mindustry.net.Net;
import mindustry.net.NetConnection;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4FastDecompressor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MNet implements MConnection {

    public static final LZ4FastDecompressor decompressor = Reflect.get(ArcNetProvider.class, "decompressor");
    public static final LZ4Compressor compressor = Reflect.get(ArcNetProvider.class, "compressor");

    private UdpConnectionManager udpConnectionManager;
    private SimpleDns dns;
    private ServerPing ping;
    /**
     * Scheduler used for player pings on server side and packet timeouts
     */
    private ScheduledExecutorService scheduler;
    private ExecutorService executor;
    private int nonce;
    private IntMap<WaitingListener<?>> listeners;
    private CallManager callManager;

    public MNet() {
        this.udpConnectionManager = new UdpConnectionManager();
        this.dns = new SimpleDns(udpConnectionManager);
        this.ping = new ServerPing(udpConnectionManager, dns);
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            return thread;
        });
        this.executor = Threads.unboundedExecutor();
        this.nonce = 1;
        this.listeners = new IntMap<>();
        this.callManager = new CallManager();

        MTypeIO.init();

        Net.NetProvider provider = Reflect.get(Vars.net, "provider");
        if (!(provider instanceof ArcNetProvider)) { // must be steam networking
            provider = Reflect.get(provider, "provider");
        }
        Server server = Reflect.get(provider, "server");

        server.setDiscoveryHandler((address, handler) -> {
            try {
                ServerListPingEvent event = Pools.obtain(ServerListPingEvent.class, ServerListPingEvent::new);
                event.address = address;
                event.offline = false;
                event.setDefaults();
                Events.fire(event);

                if (!event.offline) {
                    ByteBuffer buffer = event.writeServerData();
                    buffer.position(0);
                    handler.respond(buffer);
                }

                Pools.free(event);
            } catch (Throwable throwable) {
                Log.err("Exception processing server ping", throwable);
            }
        });

        SaveVersion.addCustomChunk("ModLib", new ModLibChunk());
        Vars.net.handleServer(DataAckPacket.class, this::onDataSynced);
    }

    private void onDataSynced(NetConnection connection, DataAckPacket packet) {
        ((MPlayerConnection) connection).handleSyncPacket(packet);

        Events.fire(new PlayerDataSyncedEvent(connection.player));
    }

    @Override
    public boolean supportsPacket(Class<?> packet) {
        // From client side we can just check if the packet is active or not
        return PacketManager.getId(packet) != -1;
    }

    public <R extends MPacket> void sendPacket(@NotNull MPacket packet, @Nullable MPacket original,
                                               @Nullable Class<R> expectedReply, @Nullable Cons<R> callback,
                                               @Nullable Runnable timeout, long timeoutDuration) {
        if (nonce >= Short.MAX_VALUE) {
            nonce = 1;
        }
        short nonce = (short) this.nonce++;
        packet.nonce = nonce;
        packet.parent = original == null ? 0 : original.nonce;

        if (expectedReply != null) {
            WaitingListener<R> listener = new WaitingListener<>(expectedReply, callback);
            listeners.put(nonce, listener);
            if (timeoutDuration > 0) {
                listener.setTimeoutTask(scheduler.schedule(() -> {
                    listeners.remove(nonce);
                    if (timeout != null) {
                        Core.app.post(timeout);
                    }
                }, timeoutDuration, TimeUnit.MILLISECONDS));
            }
        }

        Vars.net.send(packet, true);
    }

    @Override
    public void received(MPacket packet) {
        handlePacket(packet, listeners);
    }

    @SuppressWarnings("unchecked")
    public static void handlePacket(MPacket packet, IntMap<WaitingListener<?>> listeners) {
        WaitingListener<?> listener = listeners.remove(packet.parent);
        if (listener != null) {
            if (listener.getType().isAssignableFrom(packet.getClass())) {
                ((Cons<MPacket>) listener.getCallback()).get(packet);
            } else {
                listener.getCallback().get(null);
            }
            if (listener.getTimeoutTask() != null) {
                listener.getTimeoutTask().cancel(false);
            }
        }
    }

    @Override
    public <T> boolean isCallAvailable(Class<T> type) {
        return callManager.isCallAvailable(type);
    }

    public <T> T getCall(Class<T> type) {
        return callManager.getCall(type);
    }

    public <T> void registerCall(Class<T> type, T implementation) {
        callManager.registerCall(type, implementation);
    }

    public UdpConnectionManager getUdpConnectionManager() {
        return udpConnectionManager;
    }

    public SimpleDns getDns() {
        return dns;
    }

    public ServerPing getPing() {
        return ping;
    }

    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public CallManager getCallManager() {
        return callManager;
    }

}
