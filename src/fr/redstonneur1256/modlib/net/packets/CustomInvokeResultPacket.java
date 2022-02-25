package fr.redstonneur1256.modlib.net.packets;

import arc.util.io.Reads;
import arc.util.io.Writes;
import fr.redstonneur1256.modlib.net.IPacket;
import fr.redstonneur1256.modlib.net.serializer.MTypeIO;
import fr.redstonneur1256.modlib.util.NetworkUtil;

import java.nio.ByteBuffer;

public class CustomInvokeResultPacket extends IPacket {

    public Object result;
    public Throwable throwable;

    public CustomInvokeResultPacket() {
    }

    public CustomInvokeResultPacket(Object result, Throwable throwable) {
        this.result = result;
        this.throwable = throwable;
    }

    @Override
    protected void read0(ByteBuffer buffer) {
        Reads reads = NetworkUtil.reads(buffer);
        result = MTypeIO.readObject(reads);
        throwable = MTypeIO.readException(reads);
    }

    @Override
    protected void write0(ByteBuffer buffer) {
        Writes writes = NetworkUtil.writes(buffer);
        MTypeIO.writeObject(writes, result);
        MTypeIO.writeException(writes, throwable);
    }

}
