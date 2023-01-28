package fr.redstonneur1256.modlib.net.packet;

import arc.Events;
import arc.func.Prov;
import arc.struct.ObjectIntMap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Nullable;
import fr.redstonneur1256.modlib.events.net.client.ServerConnectEvent;
import fr.redstonneur1256.modlib.events.net.server.PreServerHostEvent;
import fr.redstonneur1256.modlib.net.ClassEntry;
import fr.redstonneur1256.modlib.net.packets.CustomInvokePacket;
import fr.redstonneur1256.modlib.net.packets.CustomInvokeResultPacket;
import fr.redstonneur1256.modlib.net.packets.DataAckPacket;
import mindustry.Vars;
import mindustry.net.Packet;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

@SuppressWarnings("unchecked")
public class PacketManager {

    public static final int FRAMEWORK_MESSAGE_ID = 254; // -2 on 8 bits

    /**
     * All registered packets
     */
    private static final Seq<ClassEntry<?>> registeredPackets;
    /**
     * Packets currently available on both sides
     * On server all registered packets are available but might only be sent to players with them
     * On client only the packets with id from 0 to {@link #vanillaPacketCount} are available before the packets are
     * being synchronized and once they are synchronized the packets available on the server are available
     */
    private static final Seq<ClassEntry<?>> activePackets;
    /**
     * @see #activePackets
     */
    private static final ObjectIntMap<Class<?>> packetIds;
    private static int vanillaPacketCount;

    static {
        registeredPackets = new Seq<>();
        activePackets = new Seq<>();
        packetIds = new ObjectIntMap<>();

        Events.on(ServerConnectEvent.class, event -> {
            Log.debug("Connecting to server, reinitializing packets");
            resetPacketValues(false);
        });
        Events.on(PreServerHostEvent.class, event -> {
            Log.debug("Starting server, attributing all packets");
            resetPacketValues(true);
        });
    }

    public static void finalizeRegistration() {
        if(vanillaPacketCount != 0) {
            throw new IllegalStateException("Vanilla packet registration is already completed");
        }

        Log.debug("Vanilla packets registration complete, total: @", registeredPackets.size);

        vanillaPacketCount = registeredPackets.size;

        registerPacket(DataAckPacket.class, DataAckPacket::new);
        registerPacket(CustomInvokePacket.class, CustomInvokePacket::new);
        registerPacket(CustomInvokeResultPacket.class, CustomInvokeResultPacket::new);

        resetPacketValues(false);
    }

    public static void resetPacketValues(boolean host) {
        int activePacketCount = host ? registeredPackets.size : vanillaPacketCount;

        activePackets.clear();
        activePackets.ensureCapacity(activePacketCount);
        packetIds.clear();
        packetIds.ensureCapacity(activePacketCount);

        activePackets.addAll(registeredPackets, 0, Math.min(activePacketCount, FRAMEWORK_MESSAGE_ID));
        if(activePacketCount >= FRAMEWORK_MESSAGE_ID) {
            activePackets.add((ClassEntry<?>) null); // fake framework message
            activePackets.addAll(registeredPackets, FRAMEWORK_MESSAGE_ID, activePacketCount - FRAMEWORK_MESSAGE_ID);
        }

        for(int i = 0; i < activePackets.size; i++) {
            ClassEntry<?> entry = activePackets.get(i);
            if(entry != null) {
                packetIds.put(entry.type, i);
            }
        }
    }

    public static <T> void registerPacket(Class<T> type, Prov<T> prov) {
        // Might be null during the initial packet registration in the class static initialization
        if(Vars.net != null && Vars.net.active()) {
            throw new IllegalStateException("Cannot register new packets while connected to a server or hosting a game.");
        }
        registeredPackets.add(new ClassEntry<>(type, prov));
    }

    public static int getId(@NotNull Class<?> type) {
        return packetIds.get(type, -1);
    }

    @Nullable
    public static ClassEntry<Packet> getEntry(int id) {
        return id < 0 || id >= activePackets.size ? null : ((ClassEntry<Packet>) activePackets.get(id));
    }

    public static void writeAvailablePackets(DataOutput stream) throws IOException {
        stream.writeInt(activePackets.size);
        for(ClassEntry<?> entry : activePackets) {
            stream.writeUTF(entry == null ? "ae" : entry.type.getName());
        }
    }

    /**
     * Call on client side to set the available packets to the ones available on the server
     */
    public static void readAvailablePackets(DataInput stream) throws IOException {
        int packetCount = stream.readInt();

        activePackets.clear();
        activePackets.ensureCapacity(packetCount);

        packetIds.clear();
        packetIds.ensureCapacity(packetCount);

        for(int i = 0; i < packetCount; i++) {
            String name = stream.readUTF();
            ClassEntry<?> entry = registeredPackets.find(e -> e.type.getName().equals(name));
            activePackets.add(entry);
            if(entry == null) {
                continue;
            }
            packetIds.put(entry.type, i);
        }
    }

    public static Seq<ClassEntry<?>> getRegisteredPackets() {
        return registeredPackets;
    }

    public static int getActivePacketCount() {
        return activePackets.size;
    }

    public static Seq<ClassEntry<?>> getActivePackets() {
        return activePackets;
    }

    public static ObjectIntMap<Class<?>> getPacketIds() {
        return packetIds;
    }

    public static int getVanillaPacketCount() {
        return vanillaPacketCount;
    }

}
