package fr.redstonneur1256.modlib.net.packets;

import arc.util.io.Reads;
import arc.util.io.Writes;
import fr.redstonneur1256.modlib.net.io.MTypeIO;
import fr.redstonneur1256.modlib.net.packet.MPacket;

public class CustomInvokeResultPacket extends MPacket {

    public Object result;
    public Throwable throwable;

    public CustomInvokeResultPacket() {
    }

    public CustomInvokeResultPacket(Object result, Throwable throwable) {
        this.result = result;
        this.throwable = throwable;
    }

    @Override
    protected void read0(Reads reads) {
        result = MTypeIO.readObject(reads);
        throwable = MTypeIO.readException(reads);
    }

    @Override
    protected void write0(Writes writes) {
        MTypeIO.writeObject(writes, result);
        MTypeIO.writeException(writes, throwable);
    }

}
