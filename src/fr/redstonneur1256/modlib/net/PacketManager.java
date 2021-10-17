package fr.redstonneur1256.modlib.net;

import arc.ApplicationCore;
import arc.ApplicationListener;
import arc.Core;
import arc.Events;
import arc.func.Prov;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Reflect;
import arc.util.Structs;
import arc.util.io.ReusableByteOutStream;
import fr.redstonneur1256.modlib.MVars;
import fr.redstonneur1256.modlib.events.net.PlayerPacketsSyncedEvent;
import fr.redstonneur1256.modlib.net.packets.PacketsAckPacket;
import fr.redstonneur1256.modlib.net.packets.PacketsSyncPacket;
import fr.redstonneur1256.modlib.net.packets.StreamBeginPacket;
import fr.redstonneur1256.modlib.net.provider.MArcConnection;
import fr.redstonneur1256.modlib.net.provider.MNet;
import fr.redstonneur1256.modlib.net.provider.MProvider;
import fr.redstonneur1256.modlib.net.serializer.ClassEntry;
import mindustry.ClientLauncher;
import mindustry.Vars;
import mindustry.core.NetClient;
import mindustry.core.NetServer;
import mindustry.net.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;

@SuppressWarnings("unchecked")
public class PacketManager {

    private static boolean initialized;
    private static Seq<ClassEntry<?>> registeredPackets;
    private static ClassEntry<?>[] packets;
    private static int constantPackets;

    static {
        registeredPackets = new Seq<>();
        packets = new ClassEntry[8];
        constantPackets = packets.length;

        Registrator.ClassEntry[] classes = Registrator.getClasses();

        packets[0] = new ClassEntry<>(StreamBeginPacket.class, StreamBeginPacket::new);
        packets[1] = new ClassEntry<>(Packets.StreamChunk.class, Packets.StreamChunk::new);
        packets[2] = new ClassEntry<>(Packets.WorldStream.class, Packets.WorldStream::new);
        packets[3] = new ClassEntry<>(Packets.ConnectPacket.class, Packets.ConnectPacket::new);
        packets[4] = new ClassEntry<>(Packets.InvokePacket.class, Packets.InvokePacket::new);
        if(classes.length > 5) { // Foo client network packet if available
            packets[5] = new ClassEntry<>((Class<Packet>) classes[5].type, (Prov<Packet>) classes[5].constructor);
        }
        packets[6] = new ClassEntry<>(PacketsSyncPacket.class, PacketsSyncPacket::new);
        packets[7] = new ClassEntry<>(PacketsAckPacket.class, PacketsAckPacket::new);
    }

    public static void initialize() {
        if(initialized) {
            return;
        }
        initialized = true;

        Vars.net.dispose();
        Vars.net = MVars.net = new MNet(new MProvider());

        Vars.net.handleClient(PacketsSyncPacket.class, PacketManager::onSyncPacket);

        Vars.net.handleServer(Packets.ConnectPacket.class, PacketManager::onServerConnect);
        Vars.net.handleServer(StreamBeginPacket.class, PacketManager::onServerStreamBegin);
        Vars.net.handleServer(Packets.StreamChunk.class, PacketManager::onServerStreamChunk);
        Vars.net.handleServer(PacketsAckPacket.class, PacketManager::onServerSync);

        // Recreate NetServer & NetClient after, so they register their packet listener on the new net instance
        Vars.netServer = new NetServer();
        Vars.netClient = Vars.headless ? null : new NetClient();

        Object application = Core.app.getListeners().find(listener -> listener instanceof ClientLauncher);
        if(application != null) {
            ApplicationListener[] listeners = Reflect.get(ApplicationCore.class, application, "modules");
            listeners[Structs.indexOf(listeners, listener -> listener instanceof NetServer)] = Vars.netServer;
            listeners[Structs.indexOf(listeners, listener -> listener instanceof NetClient)] = Vars.netClient;
        }else {
            Seq<ApplicationListener> listeners = Core.app.getListeners();
            listeners.set(listeners.indexOf(listener -> listener instanceof NetServer), Vars.netServer);
        }
    }

    public static <P extends Packet> void registerPacket(@NotNull Class<P> type, @NotNull Prov<P> constructor) {
        if(Vars.net.active()) {
            throw new IllegalStateException("Cannot register new packets while the network is active");
        }
        registeredPackets.add(new ClassEntry<>(type, constructor));
    }

    /**
     * Return if a packet is present on the server and can be sent
     */
    public static boolean isAvailableClient(@NotNull Class<?> type) {
        return getID(type) != -1;
    }

    /**
     * Return if a packet type is present on the connection and can be sent without any risk
     */
    public static boolean isAvailableServer(@NotNull NetConnection connection, @NotNull Class<?> type) {
        if(connection instanceof MArcConnection) {
            short id = getID(type);
            return ((MArcConnection) connection).isPacketSupported(id);
        }
        return Registrator.getID(type) != -1;
    }

    public static short getID(@NotNull Class<?> type) {
        for(int i = 0; i < packets.length; i++) {
            ClassEntry<?> entry = packets[i];
            if(entry != null && entry.type == type) {
                return (short) i;
            }
        }
        return -1;
    }

    @Nullable
    public static ClassEntry<Packet> getEntry(short id) {
        return id < 0 || id >= packets.length ? null : ((ClassEntry<Packet>) packets[id]);
    }

    public static void onClientConnect() {
        // Remove the modded packets to avoid them being sent until the server send a sync packet
        packets = Arrays.copyOf(packets, constantPackets);
    }

    public static void onServerHost() {
        int packetCount = constantPackets + registeredPackets.size;
        packets = Arrays.copyOf(packets, packetCount);

        for(int i = constantPackets; i < packets.length; i++) {
            packets[i] = null;
        }
        for(int i = 0; i < registeredPackets.size; i++) {
            packets[constantPackets + i] = registeredPackets.get(i);
        }
    }

    private static void onServerConnect(NetConnection connection, Packets.ConnectPacket packet) {
        // Who would change the alpha of the player color
        int alpha = packet.color & 0xFF;
        if(alpha == 0xFF) {
            return;
        }

        ReusableByteOutStream output = new ReusableByteOutStream();
        try(DataOutputStream stream = new DataOutputStream(output)) {
            stream.writeInt(packets.length);
            for(int i = constantPackets; i < packets.length; i++) {
                ClassEntry<?> entry = packets[i];
                stream.writeUTF(entry == null ? "null" : entry.type.getName());
            }
        }catch(IOException ignored) {
        }

        PacketsSyncPacket sync = new PacketsSyncPacket();
        sync.stream = new ByteArrayInputStream(output.getBytes(), 0, output.size());
        connection.sendStream(sync);
    }

    private static void onSyncPacket(PacketsSyncPacket packet) {
        try(DataInputStream stream = new DataInputStream(packet.stream)) {
            int packetCount = stream.readInt();
            packets = Arrays.copyOf(packets, packetCount);

            for(int i = constantPackets; i < packetCount; i++) {
                String name = stream.readUTF();
                packets[i] = registeredPackets.find(e -> e.type.getName().equals(name));
            }

            BitSet set = new BitSet(packets.length);
            for(int i = 0; i < packets.length; i++) {
                set.set(i, packets[i] != null);
            }

            Events.fire(new PlayerPacketsSyncedEvent(Vars.player));

            PacketsAckPacket ackPacket = new PacketsAckPacket();
            ackPacket.availablePackets = set;
            Vars.net.send(ackPacket, Net.SendMode.tcp);

        }catch(IOException ignored) {
            // Shouldn't happen
        }
    }

    private static void onServerStreamBegin(NetConnection connection, StreamBeginPacket packet) {
        if(!(connection instanceof MArcConnection)) {
            Log.debug("Received stream begin for a @ connection", connection.getClass().getName());
            return;
        }
        ((MArcConnection) connection).onStreamBegin(packet);
    }

    private static void onServerStreamChunk(NetConnection connection, Packets.StreamChunk packet) {
        if(!(connection instanceof MArcConnection)) {
            return;
        }
        ((MArcConnection) connection).onStreamChunk(packet);
    }

    private static void onServerSync(NetConnection connection, PacketsAckPacket packet) {
        if(!(connection instanceof MArcConnection)) {
            return;
        }
        ((MArcConnection) connection).onSync(packet);
    }

    public static int getConstantPackets() {
        return constantPackets;
    }

}
