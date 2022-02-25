package fr.redstonneur1256.modlib.util;

import arc.func.Intc;
import arc.func.Intp;
import arc.util.io.ByteBufferInput;
import arc.util.io.ByteBufferOutput;
import arc.util.io.Reads;
import arc.util.io.Writes;

import java.nio.ByteBuffer;

public class NetworkUtil {

    private static final ByteBufferInput input = new ByteBufferInput();
    private static final ByteBufferOutput output = new ByteBufferOutput();

    public static Reads reads(ByteBuffer buffer) {
        input.setBuffer(buffer);
        return Reads.get(input);
    }

    public static Writes writes(ByteBuffer buffer) {
        output.setBuffer(buffer);
        return Writes.get(output);
    }

    public static long readExtendedByte(Intp byteReader) {
        long value = 0;
        int shift = 0;
        long read;

        do {
            read = byteReader.get() & 0xFF;
            value |= read << (7 * shift);
            shift++;
        } while((read & 0x80) != 0);

        return value;
    }

    public static void writeExtendedByte(Intc byteWriter, long value) {
        while((value & 0xFFFFFF80) != 0) {
            byteWriter.get((int) ((value & 0x7F) | 0x80));
            value >>>= 7;
        }
        byteWriter.get((int) value);
    }

    public static void writeExtendedByte(ByteBuffer buffer, long value) {
        writeExtendedByte(i -> buffer.put((byte) i), value);
    }

}
