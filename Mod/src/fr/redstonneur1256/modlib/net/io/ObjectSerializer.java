package fr.redstonneur1256.modlib.net.io;

import arc.util.io.Reads;
import arc.util.io.Writes;

public class ObjectSerializer<T> {

    private Class<T> type;
    private Serializer<T> serializer;
    private int id;

    public ObjectSerializer(Class<T> type, Serializer<T> serializer) {
        this.type = type;
        this.serializer = serializer;
    }

    public T read(Reads reads) {
        return serializer.read(reads);
    }

    public void write(Writes writes, T value) {
        serializer.write(writes, value);
    }

    public Class<T> getType() {
        return type;
    }

    public Serializer<T> getSerializer() {
        return serializer;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

}
