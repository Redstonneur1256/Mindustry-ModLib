package fr.redstonneur1256.modlib.net.io;

import arc.Events;
import arc.func.Cons;
import arc.func.Cons2;
import arc.func.Func;
import arc.func.Prov;
import arc.math.geom.Point2;
import arc.math.geom.Vec2;
import arc.struct.IntSeq;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.io.Reads;
import arc.util.io.Writes;
import arc.util.serialization.SerializationException;
import fr.redstonneur1256.modlib.events.net.server.PreServerHostEvent;
import fr.redstonneur1256.modlib.util.NetworkUtil;
import mindustry.Vars;
import mindustry.ctype.Content;
import mindustry.gen.Building;
import mindustry.gen.Unit;
import mindustry.io.TypeIO;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.BitSet;
import java.util.function.IntFunction;

/**
 * TypeIO like but supports registering more types for {@link #readObject(Reads)} and {@link #writeObject(Writes, Object)}
 * Objects written with {@link TypeIO} must not be read using this class and vice-versa
 * This class should not be used outside networking as the identifiers used for serialization will change
 * on every server connection to adapt to types available on both sides.
 */
public class MTypeIO {

    private static final ObjectMap<Class<?>, ObjectSerializer<?>> registeredSerializers = new ObjectMap<>();
    private static final Seq<ObjectSerializer<?>> activeSerializers = new Seq<>();

    static {
        Events.on(PreServerHostEvent.class, event -> resetSerializerIds());

        // Wrapper types
        registerSerializer(Boolean.class, Reads::bool, Writes::bool);
        registerSerializer(Integer.class, Reads::i, Writes::i);
        registerSerializer(Long.class, Reads::l, Writes::l);
        registerSerializer(Float.class, Reads::f, Writes::f);
        registerSerializer(Double.class, Reads::d, Writes::d);
        registerSerializer(String.class, TypeIO::readString, TypeIO::writeString);

        // Primitive arrays
        registerSerializer(byte[].class, TypeIO::readBytes, TypeIO::writeBytes);
        registerSerializer(boolean[].class, MTypeIO::readBooleanArray, MTypeIO::writeBooleanArray);
        registerSerializer(int[].class, MTypeIO::readIntArray, MTypeIO::writeIntArray);
        registerSerializer(long[].class, MTypeIO::readLongArray, MTypeIO::writeLongArray);
        registerSerializer(float[].class, MTypeIO::readFloatArray, MTypeIO::writeFloatArray);
        registerSerializer(double[].class, MTypeIO::readDoubleArray, MTypeIO::writeDoubleArray);
        registerArraySerializer(String[].class, String[]::new, Reads::str, Writes::str);

        // Java types:
        registerSerializer(Throwable.class, MTypeIO::readException, MTypeIO::writeException);

        // Basic Arc types
        registerSerializer(Point2.class, reads -> Point2.unpack(reads.i()), (writes, point) -> writes.i(point.pack()));
        registerArraySerializer(Point2[].class, Point2[]::new, reads -> Point2.unpack(reads.i()), (writes, point) -> writes.i(point.pack()));
        registerSerializer(Vec2.class, MTypeIO::readVec2, MTypeIO::writeVec2);
        registerArraySerializer(Vec2[].class, Vec2[]::new, MTypeIO::readVec2, MTypeIO::writeVec2);

        // Mindustry types:
        registerSerializer(Content.class, TypeIO::readContent, TypeIO::writeContent);
        registerSerializer(Unit.class, TypeIO::readUnit, TypeIO::writeUnit);
        registerSerializer(Building.class, TypeIO::readBuilding, TypeIO::writeBuilding);
    }

    public static <T> void registerArraySerializer(Class<T[]> type, IntFunction<T[]> constructor, Func<Reads, T> reader, Cons2<Writes, T> writer) {
        registerSerializer(type,
                reads -> readArray(reads, constructor, () -> reader.get(reads)),
                (writes, array) -> writeArray(writes, array, element -> writer.get(writes, element)));
    }

    public static <T> void registerSerializer(Class<T> type, Func<Reads, T> reader, Cons2<Writes, T> writer) {
        registerSerializer(type, new FunctionSerializer<>(reader, writer));
    }

    public static <T> void registerSerializer(Class<T> type, Serializer<T> serializer) {
        if(Vars.net.active()) {
            throw new IllegalStateException("Cannot register new type serializers while connected to a server or hosting a server");
        }
        if(registeredSerializers.get(type) != null) {
            throw new IllegalStateException("A serializer is already registered for the type " + type.getName());
        }
        registeredSerializers.put(type, new ObjectSerializer<>(type, serializer));
    }

    public static void init() {
        Log.debug("No-op static initialization");
    }

    private static void resetSerializerIds() {
        activeSerializers.clear();
        activeSerializers.ensureCapacity(registeredSerializers.size);

        // null is hardcoded with ID 0
        activeSerializers.add((ObjectSerializer<?>) null);

        for(ObjectSerializer<?> serializer : registeredSerializers.values()) {
            serializer.setId(activeSerializers.size);
            activeSerializers.add(serializer);
        }
    }

    public static void writeSerializers(DataOutput stream) throws IOException {
        // Skip the first element because it's the null serializer
        stream.writeInt(activeSerializers.size - 1);

        for(ObjectSerializer<?> serializer : activeSerializers) {
            if(serializer == null) {
                continue;
            }
            stream.writeInt(serializer.getId());
            stream.writeUTF(serializer.getType().getName());
        }
    }

    public static void readSerializers(DataInput stream) throws IOException {
        int serializerCount = stream.readInt();

        activeSerializers.clear();
        activeSerializers.ensureCapacity(serializerCount + 1);

        // null is hardcoded with ID 0
        activeSerializers.add((ObjectSerializer<?>) null);

        for(ObjectSerializer<?> serializer : registeredSerializers.values()) {
            serializer.setId(-1);
        }

        for(int i = 0; i < serializerCount; i++) {
            int id = stream.readInt();
            String name = stream.readUTF();
            try {
                Class<?> type = getType(name);
                ObjectSerializer<?> serializer = registeredSerializers.get(type);

                while(activeSerializers.size <= id) {
                    activeSerializers.add((ObjectSerializer<?>) null);
                }
                activeSerializers.set(id, serializer);
                if(serializer == null) {
                    Log.warn("No serializer registered on client for type @", name);
                    continue;
                }
                serializer.setId(id);
            } catch(ClassNotFoundException exception) {
                Log.warn("Could not resolve TypeIO serializer class @", name);
            }
        }
    }

    public static Object readObject(Reads reads) {
        int id = NetworkUtil.readExtendedByte(reads::b);
        if(id == 0) {
            return null;
        }

        ObjectSerializer<?> serializer = activeSerializers.get(id);
        if(serializer == null) {
            throw new SerializationException("Trying to read object with type " + id + " but not serializer is associated with this id");
        }
        return serializer.read(reads);
    }

    @SuppressWarnings("unchecked")
    public static void writeObject(Writes writes, Object object) {
        if(object == null) {
            NetworkUtil.writeExtendedByte(writes::b, 0);
            return;
        }

        Class<?> type = object.getClass();
        ObjectSerializer<?> serializer;
        do {
            serializer = registeredSerializers.get(type);
        } while(serializer == null && (type = type.getSuperclass()) != null);

        if(serializer == null || serializer.getId() == -1) {
            throw new SerializationException("Could not find a type serializer for the object " + object + " (" + object.getClass().getName() + ")");
        }

        NetworkUtil.writeExtendedByte(writes::b, serializer.getId());
        ((ObjectSerializer<Object>) serializer).write(writes, object);
    }

    public static IntSeq readIntSeq(Reads read) {
        int count = read.i();
        IntSeq seq = new IntSeq(count);
        for(int i = 0; i < count; i++) {
            seq.add(read.i());
        }
        return seq;
    }

    public static void writeIntSeq(Writes write, IntSeq seq) {
        write.s((short) seq.size);
        for(int i = 0; i < seq.size; i++) {
            write.i(seq.get(i));
        }
    }

    public static <T> T[] readArray(Reads reads, IntFunction<T[]> constructor, Prov<T> reader) {
        int count = reads.i();
        T[] array = constructor.apply(count);
        for(int i = 0; i < count; i++) {
            array[i] = reader.get();
        }
        return array;
    }

    public static <T> void writeArray(Writes writes, T[] array, Cons<T> writer) {
        writes.i(array.length);
        for(T t : array) {
            writer.get(t);
        }
    }

    public static boolean[] readBooleanArray(Reads reads) {
        boolean[] booleans = new boolean[reads.i()];
        for(int i = 0; i < booleans.length; i++) {
            booleans[i] = reads.bool();
        }
        return booleans;
    }

    public static void writeBooleanArray(Writes writes, boolean[] booleans) {
        writes.i(booleans.length);
        for(boolean bool : booleans) {
            writes.bool(bool);
        }
    }

    public static int[] readIntArray(Reads reads) {
        int[] ints = new int[reads.i()];
        for(int i = 0; i < ints.length; i++) {
            ints[i] = reads.i();
        }
        return ints;
    }

    public static void writeIntArray(Writes writes, int[] ints) {
        writes.i(ints.length);
        for(int i : ints) {
            writes.i(i);
        }
    }

    public static long[] readLongArray(Reads reads) {
        long[] longs = new long[reads.i()];
        for(int i = 0; i < longs.length; i++) {
            longs[i] = reads.l();
        }
        return longs;
    }

    public static void writeLongArray(Writes writes, long[] longs) {
        writes.i(longs.length);
        for(long l : longs) {
            writes.l(l);
        }
    }

    public static float[] readFloatArray(Reads reads) {
        float[] floats = new float[reads.i()];
        for(int i = 0; i < floats.length; i++) {
            floats[i] = reads.f();
        }
        return floats;
    }

    public static void writeFloatArray(Writes writes, float[] floats) {
        writes.i(floats.length);
        for(float f : floats) {
            writes.f(f);
        }
    }

    public static double[] readDoubleArray(Reads reads) {
        double[] doubles = new double[reads.i()];
        for(int i = 0; i < doubles.length; i++) {
            doubles[i] = reads.d();
        }
        return doubles;
    }

    public static void writeDoubleArray(Writes writes, double[] doubles) {
        writes.i(doubles.length);
        for(double d : doubles) {
            writes.d(d);
        }
    }

    public static Vec2 readVec2(Reads reads) {
        return new Vec2(reads.f(), reads.f());
    }

    public static void writeVec2(Writes writes, Vec2 vec) {
        writes.f(vec.x);
        writes.f(vec.y);
    }

    public static Throwable readException(Reads reads) {
        if(reads.b() == 0) {
            return null;
        }
        String exceptionClass = reads.str();
        String message = reads.str();

        Throwable throwable;
        try {
            Class<?> clazz = getType(exceptionClass);

            throwable = (Throwable) clazz.getDeclaredConstructor(String.class).newInstance(message);
        } catch(Exception exception) {
            Log.warn("Failed to construct throwable for class @", exceptionClass);
            throwable = new Throwable(message);
        }

        StackTraceElement[] elements = new StackTraceElement[reads.i()];
        for(int i = 0; i < elements.length; i++) {
            String declaringGlass = reads.str();
            String methodName = reads.str();
            String fileName = reads.str();
            int lineNumber = reads.i();
            elements[i] = new StackTraceElement(declaringGlass, methodName, fileName, lineNumber);
        }
        throwable.setStackTrace(elements);

        int suppressedCount = reads.i();
        for(int i = 0; i < suppressedCount; i++) {
            Throwable suppressed = readException(reads);
            if(suppressed != null) { // Should not happen
                throwable.addSuppressed(suppressed);
            }
        }

        return throwable;
    }

    /**
     * Writes an exception
     * note: the exception class must have a constructor needing the message exception, else it will
     * be reconstructed with the default class Throwable
     */
    public static void writeException(Writes writes, Throwable throwable) {
        if(throwable == null) {
            writes.b(0);
            return;
        }
        writes.b(1);

        writes.str(throwable.getClass().getName());
        writes.str(throwable.getMessage() == null ? "" : throwable.getMessage());

        StackTraceElement[] elements = throwable.getStackTrace();
        writes.i(elements.length);
        for(StackTraceElement element : elements) {
            writes.str(element.getClassName());
            writes.str(element.getMethodName());
            writes.str(element.getFileName() == null ? "" : element.getFileName());
            writes.i(element.getLineNumber());
        }

        Throwable[] suppressedList = throwable.getSuppressed();
        writes.i(suppressedList.length);
        for(Throwable suppressed : suppressedList) {
            writeException(writes, suppressed);
        }
    }

    public static BitSet readBitSet(Reads reads) {
        return BitSet.valueOf(reads.b(reads.i()));
    }

    public static void writeBitSet(Writes writes, BitSet bitSet) {
        byte[] bytes = bitSet.toByteArray();
        writes.i(bytes.length);
        writes.b(bytes);
    }

    public static Class<?> getType(String name) throws ClassNotFoundException {
        switch(name) {
            case "void":
                return void.class;
            case "boolean":
                return boolean.class;
            case "char":
                return char.class;
            case "byte":
                return byte.class;
            case "short":
                return short.class;
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "float":
                return float.class;
            case "double":
                return double.class;
            default:
                return Class.forName(name);
        }
    }

}
