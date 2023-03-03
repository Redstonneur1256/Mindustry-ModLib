package fr.redstonneur1256.modlib.launcher.util;

import org.jetbrains.annotations.ApiStatus;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@ApiStatus.Internal
public class ArcSettings {

    public static final byte TYPE_BOOLEAN = 0;
    public static final byte TYPE_INTEGER = 1;
    public static final byte TYPE_LONG = 2;
    public static final byte TYPE_FLOAT = 3;
    public static final byte TYPE_STRING = 4;
    public static final byte TYPE_BINARY = 5;


    private Map<String, Object> values;

    public ArcSettings() {
        this.values = new HashMap<>();
    }

    public void load(File file) throws IOException {
        load(file.toPath());
    }

    public void load(Path path) throws IOException {
        try(InputStream stream = Files.newInputStream(path)) {
            load(stream);
        }
    }

    public void load(InputStream input) throws IOException {
        DataInput stream = new DataInputStream(new BufferedInputStream(input, 8192));
        int count = stream.readInt();

        for(int i = 0; i < count; i++) {
            String key = stream.readUTF();
            byte type = stream.readByte();

            switch(type) {
                case TYPE_BOOLEAN:
                    values.put(key, stream.readBoolean());
                    break;
                case TYPE_INTEGER:
                    values.put(key, stream.readInt());
                    break;
                case TYPE_LONG:
                    values.put(key, stream.readLong());
                    break;
                case TYPE_FLOAT:
                    values.put(key, stream.readFloat());
                    break;
                case TYPE_STRING:
                    values.put(key, stream.readUTF());
                    break;
                case TYPE_BINARY:
                    int length = stream.readInt();
                    byte[] bytes = new byte[length];
                    stream.readFully(bytes);
                    values.put(key, bytes);
                    break;
                default:
                    throw new IOException(String.format("Unknown key type: %s (key=\"%s\")", type, key));
            }
        }
    }

    public void write(File file) throws IOException {
        write(file.toPath());
    }

    public void write(Path path) throws IOException {
        try(OutputStream stream = Files.newOutputStream(path)) {
            write(stream);
        }
    }

    public void write(OutputStream output) throws IOException {
        DataOutput stream = new DataOutputStream(output);

        stream.writeInt(values.size());

        for(Map.Entry<String, Object> entry : values.entrySet()) {
            stream.writeUTF(entry.getKey());
            Object value = entry.getValue();

            if(value instanceof Boolean) {
                stream.writeByte(TYPE_BOOLEAN);
                stream.writeBoolean((Boolean) value);
            } else if(value instanceof Integer) {
                stream.writeByte(TYPE_INTEGER);
                stream.writeInt((Integer) value);
            } else if(value instanceof Long) {
                stream.writeByte(TYPE_LONG);
                stream.writeLong((Long) value);
            } else if(value instanceof Float) {
                stream.writeByte(TYPE_FLOAT);
                stream.writeFloat((Float) value);
            } else if(value instanceof String) {
                stream.writeByte(TYPE_STRING);
                stream.writeUTF((String) value);
            } else if(value instanceof byte[]) {
                stream.writeByte(TYPE_BINARY);
                stream.writeInt(((byte[]) value).length);
                stream.write((byte[]) value);
            }
        }
    }

    public <T> Optional<T> get(Class<T> type, String name) {
        Object value = values.get(name);
        return value != null && type.isAssignableFrom(value.getClass()) ? Optional.of(type.cast(value)) : Optional.empty();
    }

    public <T> T get(Class<T> type, String name, T def) {
        return get(type, name).orElse(def);
    }

    public void set(String name, Object value) {
        values.put(name, value);
    }

}
