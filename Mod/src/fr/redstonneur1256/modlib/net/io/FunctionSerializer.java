package fr.redstonneur1256.modlib.net.io;

import arc.func.Cons2;
import arc.func.Func;
import arc.util.io.Reads;
import arc.util.io.Writes;

public class FunctionSerializer<T> implements Serializer<T> {

    private Func<Reads, T> reader;
    private Cons2<Writes, T> writer;

    public FunctionSerializer(Func<Reads, T> reader, Cons2<Writes, T> writer) {
        this.reader = reader;
        this.writer = writer;
    }


    @Override
    public T read(Reads reads) {
        return reader.get(reads);
    }

    @Override
    public void write(Writes writes, T value) {
        writer.get(writes, value);
    }

}
