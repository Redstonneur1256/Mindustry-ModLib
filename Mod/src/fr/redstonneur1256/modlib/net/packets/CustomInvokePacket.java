package fr.redstonneur1256.modlib.net.packets;

import arc.util.io.Reads;
import arc.util.io.Writes;
import fr.redstonneur1256.modlib.net.io.MTypeIO;
import fr.redstonneur1256.modlib.net.packet.MPacket;

public class CustomInvokePacket extends MPacket {

    public int method;
    public Object[] arguments;

    public CustomInvokePacket() {
    }

    public CustomInvokePacket(int method, Object[] arguments) {
        this.method = method;
        this.arguments = arguments;
    }

    @Override
    protected void read0(Reads reads) {
        method = reads.i();
        arguments = new Object[reads.i()];
        for (int i = 0; i < arguments.length; i++) {
            arguments[i] = MTypeIO.readObject(reads);
        }
    }

    @Override
    protected void write0(Writes writes) {
        writes.i(method);
        writes.i(arguments.length);
        for (Object argument : arguments) {
            MTypeIO.writeObject(writes, argument);
        }
    }

}
