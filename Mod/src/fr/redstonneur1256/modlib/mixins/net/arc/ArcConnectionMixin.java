package fr.redstonneur1256.modlib.mixins.net.arc;

import arc.Core;
import arc.func.Cons;
import arc.net.Connection;
import arc.struct.IntMap;
import arc.struct.Seq;
import fr.redstonneur1256.modlib.MVars;
import fr.redstonneur1256.modlib.net.ArcConnectionPing;
import fr.redstonneur1256.modlib.net.MNet;
import fr.redstonneur1256.modlib.net.NetworkDebuggable;
import fr.redstonneur1256.modlib.net.WaitingListener;
import fr.redstonneur1256.modlib.net.packet.MConnection;
import fr.redstonneur1256.modlib.net.packet.MPacket;
import fr.redstonneur1256.modlib.net.packet.MPlayerConnection;
import fr.redstonneur1256.modlib.net.packet.PacketManager;
import fr.redstonneur1256.modlib.net.packets.DataAckPacket;
import fr.redstonneur1256.modlib.util.StreamSender;
import mindustry.net.ArcNetProvider;
import mindustry.net.Streamable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.BitSet;
import java.util.concurrent.TimeUnit;

@Mixin(targets = "mindustry.net.ArcNetProvider$ArcConnection")
public abstract class ArcConnectionMixin implements MPlayerConnection, MConnection, NetworkDebuggable {

    @Shadow
    @Final
    public Connection connection;

    private @Unique int nonce = 1;
    private @Unique IntMap<WaitingListener<?>> listeners = new IntMap<>();
    private @Unique BitSet supportedPackets;
    private @Unique Seq<String> supportedCallClasses;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(ArcNetProvider this$0, String address, Connection connection, CallbackInfo ci) {
        connection.addListener(new ArcConnectionPing(connection));
    }

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
        if (id == -1) {
            // The packet class is not registered, unsupported.
            return false;
        }
        if (supportedPackets == null) {
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
                listener.setTimeoutTask(MVars.net.getScheduler().schedule(() -> {
                    listeners.remove(nonce);
                    if (timeout != null) {
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

    @Override
    public long getPing() {
        return connection.getReturnTripTime();
    }

    @Shadow
    public abstract void send(Object object, boolean reliable);

}
