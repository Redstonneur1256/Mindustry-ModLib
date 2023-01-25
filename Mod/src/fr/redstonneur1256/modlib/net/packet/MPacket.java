package fr.redstonneur1256.modlib.net.packet;

import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.net.Packet;

/**
 * A packet instance that can be replied, see CustomReplyPacketExample for examples of how to use it
 */
public abstract class MPacket extends Packet {

    /**
     * The nonce used when this packet was sent
     */
    public short nonce;
    /**
     * The nonce of the packet we're replying to
     */
    public short parent;

    @Override
    public final void read(Reads reads) {
        nonce = reads.s();
        parent = reads.s();
        read0(reads);
    }

    protected void read0(Reads reads) {
    }

    @Override
    public final void write(Writes writes) {
        writes.s(nonce);
        writes.s(parent);
        write0(writes);
    }

    protected void write0(Writes writes) {
    }

}
