package fr.redstonneur1256.modlib.net.packets;

import fr.redstonneur1256.modlib.util.NetworkUtil;
import mindustry.net.Packet;

import java.nio.ByteBuffer;

public class StreamBeginPacket implements Packet {

    private static int lastID;

    public int id = lastID++;
    public int total;
    public short type;

    @Override
    public void write(ByteBuffer buffer) {
        buffer.putInt(id);
        buffer.putInt(total);
        NetworkUtil.writeExtendedByte(buffer, type);
    }

    @Override
    public void read(ByteBuffer buffer) {
        id = buffer.getInt();
        total = buffer.getInt();
        type = (short) NetworkUtil.readExtendedByte(buffer::get);
    }

}
