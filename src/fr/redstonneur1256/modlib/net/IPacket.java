package fr.redstonneur1256.modlib.net;

import mindustry.net.Packet;

import java.nio.ByteBuffer;

/**
 * A packet instance that can be replied, see CustomReplyPacketExample for examples of how to use it
 */
public abstract class IPacket implements Packet {

    public short nonce;
    public short parent;

    @Override
    public final void read(ByteBuffer buffer) {
        nonce = buffer.getShort();
        parent = buffer.getShort();
        read0(buffer);
    }

    protected void read0(ByteBuffer buffer) {
    }

    @Override
    public final void write(ByteBuffer buffer) {
        buffer.putShort(nonce);
        buffer.putShort(parent);
        write0(buffer);
    }

    protected void write0(ByteBuffer buffer) {
    }

}
