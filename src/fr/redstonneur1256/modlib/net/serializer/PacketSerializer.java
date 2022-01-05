package fr.redstonneur1256.modlib.net.serializer;

import arc.net.FrameworkMessage;
import arc.net.NetSerializer;
import arc.util.pooling.Pools;
import fr.redstonneur1256.modlib.net.PacketManager;
import mindustry.net.Packet;
import mindustry.net.Packets;

import java.nio.ByteBuffer;

public class PacketSerializer implements NetSerializer {

    public static void putExtended(ByteBuffer buffer, short value) {
        if(value > 127) {
            byte left = (byte) (((value >> 8) & 0b0111_1111) | 0b1000_0000);
            byte right = (byte) (value & 0b1111_1111);

            buffer.put(left);
            buffer.put(right);
        }else {
            buffer.put((byte) value);
        }
    }

    public static short getExtended(ByteBuffer buffer, short first) {
        if((first & 0b1000_0000) != 0) {
            byte right = buffer.get();

            return (short) ((first & 0b01111111) << 8 | right);
        }
        return first;
    }

    @Override
    public Object read(ByteBuffer buffer) {
        short id = buffer.get();
        if(id == -2) {
            return readFramework(buffer);
        }
        id = getExtended(buffer, id);

        ClassEntry<Packet> entry = PacketManager.getEntry(id);

        if(entry == null) {
            // Fake that the packet has been read
            buffer.position(buffer.limit());
            return null;
        }

        Packet packet = Pools.obtain(entry.type, entry.constructor);
        packet.read(buffer);
        return packet;
    }

    @Override
    public void write(ByteBuffer buffer, Object object) {
        if(object instanceof FrameworkMessage) {
            buffer.put((byte) -2); //code for framework message
            writeFramework(buffer, (FrameworkMessage) object);
            return;
        }

        if(!(object instanceof Packet)) {
            throw new RuntimeException("All sent objects must implement be Packets! Class: " + object.getClass());
        }
        short id = PacketManager.getID(object.getClass());

        if(id == -1) {
            // Make it look as connect confirm which won't have any effect since the player is already connected
            // this shouldn't be reached, but we never know

            buffer.put((byte) PacketManager.getID(Packets.InvokePacket.class));
            buffer.put((byte) 70); // Type
            buffer.put((byte) 0); // Priority
            buffer.putShort((short) 0); // Length
            return;
        }

        putExtended(buffer, id);

        if(object instanceof Packets.ConnectPacket) {
            ((Packets.ConnectPacket) object).color &= ~0xFF;
        }

        ((Packet) object).write(buffer);
    }

    @Override
    public int getLengthLength() {
        return 2;
    }

    @Override
    public void writeLength(ByteBuffer buffer, int length) {
        buffer.putShort((short) length);
    }

    @Override
    public int readLength(ByteBuffer buffer) {
        return buffer.getShort();
    }

    public void writeFramework(ByteBuffer buffer, FrameworkMessage message) {
        if(message instanceof FrameworkMessage.Ping) {
            FrameworkMessage.Ping ping = (FrameworkMessage.Ping) message;
            buffer.put((byte) 0);
            buffer.putInt(ping.id);
            buffer.put(ping.isReply ? 1 : (byte) 0);
        }else if(message instanceof FrameworkMessage.DiscoverHost) {
            buffer.put((byte) 1);
        }else if(message instanceof FrameworkMessage.KeepAlive) {
            buffer.put((byte) 2);
        }else if(message instanceof FrameworkMessage.RegisterUDP) {
            buffer.put((byte) 3);
            buffer.putInt(((FrameworkMessage.RegisterUDP) message).connectionID);
        }else if(message instanceof FrameworkMessage.RegisterTCP) {
            buffer.put((byte) 4);
            buffer.putInt(((FrameworkMessage.RegisterTCP) message).connectionID);
        }
    }

    public FrameworkMessage readFramework(ByteBuffer buffer) {
        byte id = buffer.get();

        if(id == 0) {
            FrameworkMessage.Ping ping = new FrameworkMessage.Ping();
            ping.id = buffer.getInt();
            ping.isReply = buffer.get() == 1;
            return ping;
        }else if(id == 1) {
            return new FrameworkMessage.DiscoverHost();
        }else if(id == 2) {
            return new FrameworkMessage.KeepAlive();
        }else if(id == 3) {
            FrameworkMessage.RegisterUDP register = new FrameworkMessage.RegisterUDP();
            register.connectionID = buffer.getInt();
            return register;
        }else if(id == 4) {
            FrameworkMessage.RegisterTCP register = new FrameworkMessage.RegisterTCP();
            register.connectionID = buffer.getInt();
            return register;
        }else {
            throw new RuntimeException("Unknown framework message!");
        }
    }


}
