package fr.redstonneur1256.examplemod.packet.direct;

import mindustry.io.TypeIO;
import mindustry.net.Packet;

import java.nio.ByteBuffer;

public class MessagePacket implements Packet {

    public String message;

    @Override
    public void read(ByteBuffer buffer) {
        message = TypeIO.readString(buffer);
    }

    @Override
    public void write(ByteBuffer buffer) {
        TypeIO.writeString(buffer, message);
    }

}
