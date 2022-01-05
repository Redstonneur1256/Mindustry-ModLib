package fr.redstonneur1256.examplemod.packet.reply;

import fr.redstonneur1256.modlib.net.IPacket;
import mindustry.io.TypeIO;

import java.nio.ByteBuffer;

/**
 * This is the same implementation as {@link fr.redstonneur1256.examplemod.packet.direct.MessagePacket} except that
 * it's extending {@link IPacket} and not {@link mindustry.net.Packet} and allows it to get replied
 */
public class ReplyMessagePacket extends IPacket {

    public String message;

    public ReplyMessagePacket() {
    }

    public ReplyMessagePacket(String message) {
        this.message = message;
    }

    @Override
    public void read0(ByteBuffer buffer) {
        message = TypeIO.readString(buffer);
    }

    @Override
    public void write0(ByteBuffer buffer) {
        TypeIO.writeString(buffer, message);
    }

}
