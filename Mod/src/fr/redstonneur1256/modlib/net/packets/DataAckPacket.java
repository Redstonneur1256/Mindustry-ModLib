package fr.redstonneur1256.modlib.net.packets;

import arc.struct.Seq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import fr.redstonneur1256.modlib.net.io.MTypeIO;
import mindustry.net.Packet;

import java.util.BitSet;

public class DataAckPacket extends Packet {

    public BitSet availablePackets;
    public BitSet availableMethods;
    public Seq<String> availableCallClasses;

    @Override
    public void write(Writes writes) {
        MTypeIO.writeBitSet(writes, availablePackets);
        MTypeIO.writeBitSet(writes, availableMethods);

        writes.i(availableCallClasses.size);
        availableCallClasses.forEach(writes::str);
    }

    @Override
    public void read(Reads reads) {
        availablePackets = MTypeIO.readBitSet(reads);
        availableMethods = MTypeIO.readBitSet(reads);

        int callClassCount = reads.i();
        availableCallClasses = new Seq<>(callClassCount);
        for (int i = 0; i < callClassCount; i++) {
            availableCallClasses.add(reads.str());
        }
    }

}
