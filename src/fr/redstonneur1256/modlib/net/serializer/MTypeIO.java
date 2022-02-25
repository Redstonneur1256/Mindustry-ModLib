package fr.redstonneur1256.modlib.net.serializer;

import arc.func.Cons;
import arc.func.Cons2;
import arc.func.Func;
import arc.func.Prov;
import arc.math.geom.Point2;
import arc.struct.IntSeq;
import arc.struct.ObjectIntMap;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.io.Reads;
import arc.util.io.Writes;
import fr.redstonneur1256.modlib.func.FuncI;
import fr.redstonneur1256.modlib.util.NetworkUtil;
import mindustry.Vars;
import mindustry.content.TechTree;
import mindustry.ctype.Content;
import mindustry.ctype.ContentType;
import mindustry.entities.units.UnitCommand;
import mindustry.gen.Building;
import mindustry.io.TypeIO;
import mindustry.logic.LAccess;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static mindustry.Vars.content;

/**
 * TypeIO compatible but support registering new types
 * SHOULD BE USED ONLY FOR NETWORKING, IDENTIFIERS CHANGE ON CONNECTION
 */
@SuppressWarnings("unchecked")
public class MTypeIO {

    private static final ObjectMap<Class<?>, Serializer<?>> registeredSerializers = new ObjectMap<>();

    private static final ObjectIntMap<Class<?>> typeToID = new ObjectIntMap<>();
    private static final Seq<Serializer<?>> serializers = new Seq<>();

    static {
        // Register TypeIO serializers:
        registerSerializer(void.class, null, null); // No null keys :(
        registerSerializer(Integer.class, Reads::i, Writes::i);
        registerSerializer(Long.class, Reads::l, Writes::l);
        registerSerializer(Float.class, Reads::f, Writes::f);
        registerSerializer(String.class, TypeIO::readString, TypeIO::writeString);
        registerSerializer(Content.class, TypeIO::readContent, TypeIO::writeContent);
        registerSerializer(IntSeq.class, MTypeIO::readIntSeq, MTypeIO::writeIntSeq);
        registerSerializer(Point2.class, reads -> new Point2(reads.i(), reads.i()),
                (writes, point) -> writes.l((long) point.x << 32 | point.y));
        registerSerializer(Point2[].class, reads -> readArray(reads, Point2[]::new, () -> Point2.unpack(reads.i())),
                (writes, points) -> writeArray(writes, points, point -> writes.i(point.pack())));
        registerSerializer(TechTree.TechNode.class, reads -> TechTree.getNotNull(content.getByID(ContentType.all[reads.b()], reads.s())),
                (writes, node) -> {
                    writes.b(node.content.getContentType().ordinal());
                    writes.s(node.content.id);
                });
        registerSerializer(Boolean.class, Reads::bool, Writes::bool);
        registerSerializer(Double.class, Reads::d, Writes::d);
        registerSerializer(Building.class, reads -> Vars.world.build(reads.i()), (writes, building) -> writes.i(building.pos()));
        registerSerializer(LAccess.class, reads -> LAccess.all[reads.s()], (writes, access) -> writes.s(access.ordinal()));
        registerSerializer(byte[].class, TypeIO::readBytes, TypeIO::writeBytes);
        registerSerializer(UnitCommand.class, reads -> UnitCommand.all[reads.b()], (writes, command) -> writes.b(command.ordinal()));
    }

    public static <T> void registerSerializer(Class<T> type, Func<Reads, T> reader, Cons2<Writes, T> writer) {
        registerSerializer(type, new FunctionSerializer<>(reader, writer));
    }

    public static <T> void registerSerializer(Class<T> type, Serializer<T> serializer) {
        if(Vars.net.active()) {
            throw new IllegalStateException("Cannot register new types while network is active");
        }

        if(registeredSerializers.get(type) != null) {
            throw new IllegalStateException("A serializer is already registered for the type " + type.getName());
        }
        registeredSerializers.put(type, serializer);
    }

    public static void updateSerializerIDs() {
        serializers.clear();
        typeToID.clear();
        for(ObjectMap.Entry<Class<?>, Serializer<?>> entry : registeredSerializers) {
            int id = serializers.size;
            typeToID.put(entry.key, id);
            serializers.add(entry.value);
        }
    }

    public static void writeSerializerIDs(DataOutputStream stream) throws IOException {
        stream.writeInt(serializers.size);
        for(ObjectIntMap.Entry<Class<?>> entry : typeToID) {
            stream.writeInt(entry.value);
            stream.writeUTF(entry.key.getName());
        }
    }

    public static void readSerializerTypes(DataInputStream stream) throws IOException {
        int count = stream.readInt();

        serializers.clear();
        typeToID.clear();

        for(int i = 0; i < count; i++) {
            int id = stream.readInt();
            String className = stream.readUTF();
            try {
                Class<?> type = Class.forName(className);
                Serializer<?> serializer = registeredSerializers.get(type);
                if(serializer == null) {
                    throw new NullPointerException();
                }

                while(serializers.size <= id) {
                    serializers.add(null);
                }

                typeToID.put(type, id);
                serializers.set(id, serializer);
            } catch(Exception exception) {
                Log.warn("No registered serializer for type @ but present on the server", className);
            }
        }
    }

    public static <T> void writeObject(Writes writes, T value) {
        if(value == null) {
            NetworkUtil.writeExtendedByte(writes::b, 0);
            return;
        }
        int id = typeToID.get(value.getClass(), -1);
        if(id == -1) {
            throw new IllegalArgumentException("Unable to serialize object " + value + " (" + value.getClass().getName() + ")");
        }
        Serializer<T> serializer = (Serializer<T>) serializers.get(id);

        NetworkUtil.writeExtendedByte(writes::b, id);
        serializer.write(writes, value);
    }

    public static Object readObject(Reads reads) {
        int type = (int) NetworkUtil.readExtendedByte(reads::b);
        if(type == 0) {
            return null;
        }
        Serializer<?> serializer = serializers.get(type);
        if(serializer == null) {
            throw new IllegalArgumentException("Unable to deserialize object with type " + type);
        }
        return serializer.read(reads);
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

    public static <T> T[] readArray(Reads reads, FuncI<T[]> constructor, Prov<T> reader) {
        int count = reads.i();
        T[] array = constructor.get(count);
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

    public static Throwable readException(Reads reads) {
        if(reads.b() == 0) {
            return null;
        }
        String exceptionClass = reads.str();
        String message = reads.str();

        Throwable throwable;
        try {
            Class<?> clazz = Class.forName(exceptionClass);

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

}
