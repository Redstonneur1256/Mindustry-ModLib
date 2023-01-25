package fr.redstonneur1256.examplemod.net.packet.direct;

import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.net.Packet;

public class MessagePacket extends Packet {

    public String message;

    @Override
    public void read(Reads read) {
        message = read.str();
    }

    @Override
    public void write(Writes write) {
        write.str(message);
    }

}
