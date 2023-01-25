package fr.redstonneur1256.examplemod.net.packet.reply;

import arc.util.io.Reads;
import arc.util.io.Writes;
import fr.redstonneur1256.modlib.net.packet.MPacket;

/**
 * This is the same implementation as {@link fr.redstonneur1256.examplemod.net.packet.direct.MessagePacket} except that
 * it's extending {@link MPacket} and not {@link mindustry.net.Packet} and allows it to get replied
 */
public class ReplyMessagePacket extends MPacket {

    public String message;

    public ReplyMessagePacket() {
    }

    public ReplyMessagePacket(String message) {
        this.message = message;
    }


    @Override
    protected void read0(Reads reads) {
        message = reads.str();
    }

    @Override
    protected void write0(Writes writes) {
        writes.str(message);
    }

}
