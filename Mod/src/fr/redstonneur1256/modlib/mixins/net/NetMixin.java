package fr.redstonneur1256.modlib.mixins.net;

import arc.Events;
import arc.func.Cons;
import arc.func.Prov;
import arc.struct.IntMap;
import fr.redstonneur1256.modlib.MVars;
import fr.redstonneur1256.modlib.events.net.NetExceptionEvent;
import fr.redstonneur1256.modlib.events.net.client.ServerConnectEvent;
import fr.redstonneur1256.modlib.events.net.server.PreServerHostEvent;
import fr.redstonneur1256.modlib.net.packet.MConnection;
import fr.redstonneur1256.modlib.net.packet.MPacket;
import fr.redstonneur1256.modlib.net.packet.PacketManager;
import fr.redstonneur1256.modlib.net.packet.PacketTypeAccessor;
import mindustry.net.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.ExecutorService;

@Mixin(Net.class)
public class NetMixin {

    @Final
    @Shadow
    private ExecutorService pingExecutor;
    @Shadow
    private boolean active;
    @Shadow
    private Streamable.StreamBuilder currentStream;
    @Shadow
    @Final
    private IntMap<Streamable.StreamBuilder> streams;

    @Inject(method = "handleException", at = @At("RETURN"))
    public void handleException(Throwable throwable, CallbackInfo ci) {
        Events.fire(new NetExceptionEvent(throwable));
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(Net.NetProvider provider, CallbackInfo ci) {
        pingExecutor.shutdown();
    }

    @Inject(method = "pingHost", at = @At("HEAD"), cancellable = true)
    public void pingHost(String address, int port, Cons<Host> valid, Cons<Exception> failed, CallbackInfo ci) {
        ci.cancel();

        MVars.net.getPing().ping(address, port, valid, failed);
    }

    @Inject(method = "connect", at = @At("HEAD"))
    public void connect(String ip, int port, Runnable success, CallbackInfo ci) {
        if(!active) {
            Events.fire(new ServerConnectEvent(ip, port));
        }
    }

    @Inject(method = "host", at = @At("HEAD"))
    public void host(int port, CallbackInfo ci) {
        Events.fire(new PreServerHostEvent());
    }

    @SuppressWarnings("unchecked")
    @Inject(method = "registerPacket", at = @At("HEAD"), cancellable = true)
    private static <T> void registerPacket(Prov<T> cons, CallbackInfo ci) {
        ci.cancel();

        PacketManager.registerPacket((Class<T>) cons.get().getClass(), cons);
    }

    @Inject(method = "handleClientReceived", at = @At("HEAD"), cancellable = true)
    public void handleClientReceived(Packet packet, CallbackInfo ci) {
        if(packet instanceof Packets.StreamBegin) {
            Packets.StreamBegin begin = (Packets.StreamBegin) packet;

            currentStream = new Streamable.StreamBuilder(begin);
            // Forward extended non-byte type
            ((PacketTypeAccessor) currentStream).setType(((PacketTypeAccessor) packet).getType());

            streams.put(begin.id, currentStream);

            ci.cancel();
        }

        if(packet instanceof MPacket) {
            MVars.net.received((MPacket) packet);
        }
    }

    @Inject(method = "handleServerReceived", at = @At("HEAD"))
    public void handleServerReceived(NetConnection connection, Packet packet, CallbackInfo ci) {
        if(packet instanceof MPacket) {
            ((MConnection) connection).received((MPacket) packet);
        }
    }

}
