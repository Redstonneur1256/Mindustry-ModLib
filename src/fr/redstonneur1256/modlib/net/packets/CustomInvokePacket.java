package fr.redstonneur1256.modlib.net.packets;

import arc.util.io.Reads;
import arc.util.io.Writes;
import fr.redstonneur1256.modlib.net.IPacket;
import fr.redstonneur1256.modlib.net.serializer.MTypeIO;
import fr.redstonneur1256.modlib.util.NetworkUtil;

import java.nio.ByteBuffer;

public class CustomInvokePacket extends IPacket {

    public int method;
    public Object[] arguments;

    public CustomInvokePacket() {
    }

    public CustomInvokePacket(int method, Object[] arguments) {
        this.method = method;
        this.arguments = arguments;
    }

    @Override
    protected void read0(ByteBuffer buffer) {
        Reads reads = NetworkUtil.reads(buffer);

        method = reads.i();
        arguments = new Object[buffer.getInt()];
        for(int i = 0; i < arguments.length; i++) {
            arguments[i] = MTypeIO.readObject(reads);
        }
    }

    @Override
    protected void write0(ByteBuffer buffer) {
        Writes writes = NetworkUtil.writes(buffer);

        writes.i(method);
        writes.i(arguments.length);
        for(Object argument : arguments) {
            MTypeIO.writeObject(writes, argument);
        }
    }

}
