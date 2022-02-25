package fr.redstonneur1256.modlib.net.serializer;

import arc.util.io.Reads;
import arc.util.io.Writes;

public interface Serializer<T> {

    T read(Reads reads);

    void write(Writes writes, T value);

}
