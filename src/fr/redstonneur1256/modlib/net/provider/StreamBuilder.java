package fr.redstonneur1256.modlib.net.provider;

import arc.net.ArcNetException;
import fr.redstonneur1256.modlib.net.PacketManager;
import fr.redstonneur1256.modlib.net.packets.StreamBeginPacket;
import fr.redstonneur1256.modlib.net.serializer.ClassEntry;
import mindustry.net.Packet;
import mindustry.net.Packets;
import mindustry.net.Streamable;

import java.io.ByteArrayInputStream;

public class StreamBuilder extends Streamable.StreamBuilder {

    private short type;

    public StreamBuilder(StreamBeginPacket begin) {
        super(transform(begin));
        this.type = begin.type;
    }

    private static Packets.StreamBegin transform(StreamBeginPacket begin) {
        Packets.StreamBegin stream = new Packets.StreamBegin();
        stream.id = begin.id;
        stream.total = begin.total;
        stream.type = 0;
        return stream;
    }

    @Override
    public Streamable build() {
        ClassEntry<Packet> entry = PacketManager.getEntry(type);
        if(entry == null) {
            throw new ArcNetException("Received stream type " + type + " doesn't have a packet registered with it");
        }
        Packet packet = entry.constructor.get();
        if(!(packet instanceof Streamable)) {
            throw new ArcNetException("Received stream type " + packet.getClass() + " isn't streamable");
        }
        Streamable streamable = (Streamable) packet;
        streamable.stream = new ByteArrayInputStream(stream.toByteArray());
        return streamable;
    }

}
