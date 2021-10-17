package fr.redstonneur1256.modlib.net.packets;

import mindustry.net.Packet;

import java.nio.ByteBuffer;
import java.util.BitSet;

public class PacketsAckPacket implements Packet {

    public BitSet availablePackets;

    @Override
    public void write(ByteBuffer buffer) {
        byte[] bytes = availablePackets.toByteArray();
        buffer.putInt(bytes.length);
        buffer.put(bytes);
    }

    @Override
    public void read(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.getInt()];
        buffer.get(bytes);
        availablePackets = BitSet.valueOf(bytes);
    }

}
