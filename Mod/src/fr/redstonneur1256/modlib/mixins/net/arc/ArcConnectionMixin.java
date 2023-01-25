package fr.redstonneur1256.modlib.mixins.net.arc;

import arc.Core;
import arc.func.Cons;
import arc.net.Connection;
import arc.struct.IntMap;
import arc.struct.Seq;
import fr.redstonneur1256.modlib.MVars;
import fr.redstonneur1256.modlib.net.MNet;
import fr.redstonneur1256.modlib.net.WaitingListener;
import fr.redstonneur1256.modlib.net.packet.MConnection;
import fr.redstonneur1256.modlib.net.packet.MPacket;
import fr.redstonneur1256.modlib.net.packet.MPlayerConnection;
import fr.redstonneur1256.modlib.net.packet.PacketManager;
import fr.redstonneur1256.modlib.net.packets.DataAckPacket;
import fr.redstonneur1256.modlib.util.StreamSender;
import mindustry.net.Streamable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.BitSet;
import java.util.concurrent.TimeUnit;

@Mixin(targets = "mindustry.net.ArcNetProvider$ArcConnection")
public abstract class ArcConnectionMixin implements MPlayerConnection, MConnection {

    @Shadow
    @Final
    public Connection connection;

    private int nonce = 1;
    private IntMap<WaitingListener<?>> listeners = new IntMap<>();
    private BitSet supportedPackets;
    private Seq<String> supportedCallClasses;

    @Override
    public void handleSyncPacket(DataAckPacket packet) {
        this.supportedPackets = packet.availablePackets;
        this.supportedCallClasses = packet.availableCallClasses;
    }

    /**
     * @author Redstonneur1256
     * @reason Replace packet ID by extended ID
     */
    @Overwrite
    public void sendStream(Streamable streamable) {
        connection.addListener(new StreamSender(connection, streamable, 512));
    }

    @Override
    public boolean supportsPacket(Class<?> packet) {
        int id = PacketManager.getId(packet);
        if(id == -1) {
            // The packet class is not registered, unsupported.
            return false;
        }
        if(supportedPackets == null) {
            // Not synced yet, assume only vanilla packets are available
            return id < PacketManager.getVanillaPacketCount();
        }
        return supportedPackets.get(id);
    }

    @Override
    public <T> boolean isCallAvailable(Class<T> type) {
        return supportedCallClasses != null && supportedCallClasses.contains(type.getName());
    }

    @Override
    public <R extends MPacket> void sendPacket(@NotNull MPacket packet, @Nullable MPacket original,
                                               @Nullable Class<R> expectedReply, @Nullable Cons<R> callback,
                                               @Nullable Runnable timeout, long timeoutDuration) {
        if(nonce >= Short.MAX_VALUE) {
            nonce = 1;
        }
        short nonce = (short) this.nonce++;
        packet.nonce = nonce;
        packet.parent = original == null ? 0 : original.nonce;

        if(expectedReply != null) {
            WaitingListener<R> listener = new WaitingListener<>(expectedReply, callback);
            listeners.put(nonce, listener);
            if(timeoutDuration > 0) {
                listener.setTimeoutTask(MVars.net.getScheduler().schedule(() -> {
                    listeners.remove(nonce);
                    if(timeout != null) {
                        Core.app.post(timeout);
                    }
                }, timeoutDuration, TimeUnit.MILLISECONDS));
            }
        }

        send(packet, true);
    }

    @Override
    public void received(MPacket packet) {
        MNet.handlePacket(packet, listeners);
    }

    @Shadow
    public abstract void send(Object object, boolean reliable);

}
