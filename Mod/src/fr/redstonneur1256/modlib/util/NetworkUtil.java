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
        input.buffer = buffer;
        return Reads.get(input);
    }

    public static Writes writes(ByteBuffer buffer) {
        output.buffer = buffer;
        return Writes.get(output);
    }

    public static int readExtendedByte(Intp byteReader) {
        int value = 0;
        int shift = 0;
        int read;

        do {
            read = byteReader.get();
            value |= (read & 0x7F) << shift;
            shift += 7;
        } while((read & 0x80) != 0);

        return value;
    }

    public static void writeExtendedByte(Intc byteWriter, int value) {
        while((value & ~0x7F) != 0) {
            byteWriter.get((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        byteWriter.get(value);
    }

    public static void writeExtendedByte(ByteBuffer buffer, int value) {
        writeExtendedByte(i -> buffer.put((byte) i), value);
    }

}
