package fr.redstonneur1256.modlib.util;

import arc.net.Connection;
import arc.net.InputStreamSender;
import fr.redstonneur1256.modlib.net.packet.PacketManager;
import fr.redstonneur1256.modlib.net.packet.PacketTypeAccessor;
import mindustry.net.Packets;
import mindustry.net.Streamable;

public class StreamSender extends InputStreamSender {

    private Connection connection;
    private Streamable streamable;
    private int streamId;

    public StreamSender(Connection connection, Streamable streamable, int chunkSize) {
        super(streamable.stream, chunkSize);
        this.connection = connection;
        this.streamable = streamable;
    }

    @Override
    protected void start() {
        Packets.StreamBegin begin = new Packets.StreamBegin();
        begin.total = streamable.stream.available();
        ((PacketTypeAccessor) begin).setType(PacketManager.getId(streamable.getClass()));

        connection.sendTCP(begin);
        streamId = begin.id;
    }

    @Override
    protected Object next(byte[] bytes) {
        Packets.StreamChunk chunk = new Packets.StreamChunk();
        chunk.id = streamId;
        chunk.data = bytes;
        return chunk;
    }

}
