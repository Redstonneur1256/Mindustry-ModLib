package fr.redstonneur1256.modlib.net.provider;

import arc.Events;
import arc.net.Connection;
import arc.net.DcReason;
import arc.net.InputStreamSender;
import arc.struct.Bits;
import arc.struct.IntMap;
import arc.util.Log;
import fr.redstonneur1256.modlib.events.net.PlayerPacketsSyncedEvent;
import fr.redstonneur1256.modlib.net.PacketManager;
import fr.redstonneur1256.modlib.net.packets.PacketsAckPacket;
import fr.redstonneur1256.modlib.net.packets.StreamBeginPacket;
import mindustry.Vars;
import mindustry.net.Net;
import mindustry.net.NetConnection;
import mindustry.net.Packets;
import mindustry.net.Streamable;

public class MArcConnection extends NetConnection {

    public final MProvider provider;
    public final Connection connection;
    private IntMap<StreamBuilder> streams;
    private Bits supportedPackets;

    public MArcConnection(String address, MProvider provider, Connection connection) {
        super(address);
        this.provider = provider;
        this.connection = connection;
        this.streams = new IntMap<>();
        this.supportedPackets = new Bits(PacketManager.getConstantPackets());

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

    public void onSync(PacketsAckPacket packet) {
        int count = packet.availablePackets.size();
        supportedPackets = new Bits(count);
        for(int i = 0; i < count; i++) {
            supportedPackets.set(i, packet.availablePackets.get(i));
        }

        Events.fire(new PlayerPacketsSyncedEvent(player));
    }

}
