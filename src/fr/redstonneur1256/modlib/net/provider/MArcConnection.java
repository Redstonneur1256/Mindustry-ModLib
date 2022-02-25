package fr.redstonneur1256.modlib.net.provider;

import arc.Core;
import arc.Events;
import arc.func.Cons;
import arc.net.Connection;
import arc.net.DcReason;
import arc.net.InputStreamSender;
import arc.struct.Bits;
import arc.struct.IntMap;
import arc.util.Log;
import fr.redstonneur1256.modlib.events.net.PlayerDataSyncedEvent;
import fr.redstonneur1256.modlib.net.IPacket;
import fr.redstonneur1256.modlib.net.PacketManager;
import fr.redstonneur1256.modlib.net.packets.DataAckPacket;
import fr.redstonneur1256.modlib.net.packets.StreamBeginPacket;
import mindustry.Vars;
import mindustry.net.Net;
import mindustry.net.NetConnection;
import mindustry.net.Packets;
import mindustry.net.Streamable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

/**
 * Methods when sending a packet with a callback setting timeout to 0 will disable it, all callbacks are executed
 * from the application main thread
 */
public class MArcConnection extends NetConnection {

    public final MProvider provider;
    public final Connection connection;
    private IntMap<StreamBuilder> streams;
    private Bits supportedPackets;
    public IntMap<WaitingListener<?>> listeners;
    private int nonce;

    public MArcConnection(String address, MProvider provider, Connection connection) {
        super(address);
        this.provider = provider;
        this.connection = connection;
        this.streams = new IntMap<>();
        this.supportedPackets = new Bits(PacketManager.getConstantPackets());
        this.listeners = new IntMap<>();
        this.nonce = 1;

        for(int i = 0; i < supportedPackets.numBits(); i++) {
            supportedPackets.set(i, true);
        }
    }

    @Override
    public boolean isConnected() {
        return connection.isConnected();
    }

    @Override
    public void sendStream(Streamable stream) {
        connection.addListener(new InputStreamSender(stream.stream, 512) {
            int id;

            @Override
            protected void start() {
                //send an object so the receiving side knows how to handle the following chunks
                StreamBeginPacket begin = new StreamBeginPacket();
                begin.total = stream.stream.available();
                begin.type = PacketManager.getID(stream.getClass());
                connection.sendTCP(begin);
                id = begin.id;
            }

            @Override
            protected Object next(byte[] bytes) {
                Packets.StreamChunk chunk = new Packets.StreamChunk();
                chunk.id = id;
                chunk.data = bytes;
                return chunk; //wrap the byte[] with an object so the receiving side knows how to handle it.
            }
        });
    }

    @Override
    public void send(Object object, Net.SendMode mode) {
        try {
            if(mode == Net.SendMode.tcp) {
                connection.sendTCP(object);
            }else {
                connection.sendUDP(object);
            }
        }catch(Exception exception) {
            Log.info("Error sending packet. Disconnecting invalid client!");
            connection.close(DcReason.error);

            MArcConnection arcConnection = provider.getByArcID(connection.getID());
            if(arcConnection != null) {
                provider.connections.remove(arcConnection);
            }
        }
    }

    public void sendReply(@NotNull IPacket original, @NotNull IPacket reply) {
        sendPacket(reply, original, null, null, null, 0);
    }

    public <R extends IPacket> void sendReply(@NotNull IPacket original, @NotNull IPacket reply,
                                              @NotNull Class<R> expectedReply, @NotNull Cons<R> callback) {
        sendPacket(reply, original, expectedReply, callback, null, 0);
    }

    public <R extends IPacket> void sendReply(@NotNull IPacket original, @NotNull IPacket reply,
                                              @NotNull Class<R> expectedReply, @NotNull Cons<R> callback,
                                              @Nullable Runnable timeout, long timeoutDuration) {
        sendPacket(reply, original, expectedReply, callback, timeout, timeoutDuration);

    }

    public <R extends IPacket> void sendPacket(@NotNull IPacket packet,
                                               @NotNull Class<R> expectedReply, @NotNull Cons<R> callback) {
        sendPacket(packet, null, expectedReply, callback, null, 0);
    }

    public <R extends IPacket> void sendPacket(@NotNull IPacket packet,
                                               @NotNull Class<R> expectedReply, @NotNull Cons<R> callback,
                                               @Nullable Runnable timeout, long timeoutDuration) {
        sendPacket(packet, null, expectedReply, callback, timeout, timeoutDuration);
    }

    private <R extends IPacket> void sendPacket(@NotNull IPacket packet, @Nullable IPacket original,
                                                @Nullable Class<R> expectedReply, @Nullable Cons<R> callback,
                                                @Nullable Runnable timeout, long timeoutDuration) {
        if(nonce >= Short.MAX_VALUE) {
            nonce = 1;
        }
        packet.nonce = (short) nonce++;
        packet.parent = original == null ? 0 : original.nonce;

        if(expectedReply != null) {
            short nonce = packet.nonce;
            WaitingListener<R> listener = new WaitingListener<>(expectedReply, callback);
            listeners.put(nonce, listener);
            if(timeoutDuration > 0) {
                listener.timeoutTask = provider.executor.schedule(() -> {
                    listeners.remove(nonce);
                    if(timeout != null) {
                        Core.app.post(timeout);
                    }
                }, timeoutDuration, TimeUnit.MILLISECONDS);
            }
        }

        send(packet, Net.SendMode.tcp);
    }

    @Override
    public void close() {
        if(connection.isConnected()) {
            connection.close(DcReason.closed);
        }
    }

    public void onStreamBegin(StreamBeginPacket packet) {
        streams.put(packet.id, new StreamBuilder(packet));
    }

    public void onStreamChunk(Packets.StreamChunk packet) {
        StreamBuilder stream = streams.get(packet.id);
        if(stream == null) {
            throw new RuntimeException("Received stream chunk without a StreamBegin beforehand!");
        }
        stream.add(packet.data);

        if(stream.isDone()) {
            streams.remove(stream.id);
            Vars.net.handleServerReceived(this, stream.build());
        }
    }

    public boolean isPacketSupported(short id) {
        return supportedPackets != null && id >= 0 && id < supportedPackets.numBits() && supportedPackets.get(id);
    }

    public void onSync(DataAckPacket packet) {
        int count = packet.availablePackets.size();
        supportedPackets = new Bits(count);
        for(int i = 0; i < count; i++) {
            supportedPackets.set(i, packet.availablePackets.get(i));
        }

        Events.fire(new PlayerDataSyncedEvent(player));
    }

}
