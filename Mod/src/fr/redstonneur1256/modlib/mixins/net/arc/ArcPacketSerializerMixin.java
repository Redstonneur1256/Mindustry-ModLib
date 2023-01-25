package fr.redstonneur1256.modlib.mixins.net.arc;

import arc.net.FrameworkMessage;
import arc.util.Log;
import arc.util.io.Reads;
import arc.util.io.Writes;
import fr.redstonneur1256.modlib.net.ClassEntry;
import fr.redstonneur1256.modlib.net.MNet;
import fr.redstonneur1256.modlib.net.packet.PacketManager;
import mindustry.net.ArcNetProvider;
import mindustry.net.Packet;
import mindustry.net.Packets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.nio.ByteBuffer;

@Mixin(ArcNetProvider.PacketSerializer.class)
public abstract class ArcPacketSerializerMixin {

    @Shadow
    ThreadLocal<ByteBuffer> decompressBuffer;
    @Shadow
    ThreadLocal<Reads> reads;
    @Shadow
    ThreadLocal<Writes> writes;

    /**
     * @author Redstonneur1256
     * @reason write packets from synced registry with extended id
     */
    @Overwrite
    public void write(ByteBuffer buffer, Object object) {
        if(object instanceof ByteBuffer) {
            buffer.put((ByteBuffer) object);
            return;
        }
        if(object instanceof FrameworkMessage) {
            buffer.put((byte) -2);
            writeFramework(buffer, (FrameworkMessage) object);
            return;
        }
        if(object instanceof Packet) {
            int id = PacketManager.getId(object.getClass());
            if(id == -1) {
                // Client side only, on server all packets are always available
                // A mod tried to send a packet that is not supported by the current server we are on
                // Vanilla server will close the connection upon reading an unknown packet and at this point we have no
                // way of stopping the buffer from being sent, we need to make it a valid packet with no action or a
                // framework message, keepAlive does exactly that
                buffer.put((byte) -2);
                writeFramework(buffer, FrameworkMessage.keepAlive);

                Log.warn("Tried to send a packet that is unavailable on the server: @", object);
                return;
            }

            if(id <= 0x7F) {
                buffer.put((byte) id);
            } else {
                buffer.put((byte) (id >> 8 & 0x7F | 0x80));
                buffer.put((byte) (id & 0xFF));
            }

            ByteBuffer packetBuffer = decompressBuffer.get();
            packetBuffer.position(0);
            packetBuffer.limit(packetBuffer.capacity());
            ((Packet) object).write(writes.get());

            short length = (short) packetBuffer.position();

            //write length, uncompressed
            buffer.putShort(length);

            if(length < 36 || object instanceof Packets.StreamChunk) {
                buffer.put((byte) 0);
                buffer.put(packetBuffer.array(), 0, length);
            } else {
                buffer.put((byte) 1);
                //write compressed data; this does not modify position!
                int written = MNet.compressor.compress(packetBuffer, 0, packetBuffer.position(), buffer, buffer.position(), buffer.remaining());
                //skip to indicate the written, compressed data
                buffer.position(buffer.position() + written);
            }

            return;
        }

        throw new IllegalStateException("Unable to send object " + object);
    }

    /**
     * @author Redstonneur1256
     * @reason read packets from synced registry with extended id
     */
    @Overwrite
    public Object read(ByteBuffer buffer) {
        int id = buffer.get();
        if(id == -2) {
            return readFramework(buffer);
        }

        if((id & 0x80) != 0) {
            id = (id & 0x7F) << 8 | buffer.get();
        }

        ByteBuffer packetBuffer = decompressBuffer.get();
        int length = buffer.getShort() & 0xffff;
        byte compression = buffer.get();
        int finalLength = length;

        if(compression == 0) {
            packetBuffer.position(0);
            packetBuffer.put(buffer.array(), buffer.position(), length);
        } else {
            finalLength = MNet.decompressor.decompress(buffer, buffer.position(), packetBuffer, 0, length);
        }
        buffer.position(buffer.position() + finalLength); // simulate packet has been read

        ClassEntry<Packet> entry = PacketManager.getEntry(id);
        if(entry == null) {
            Log.warn("Received unknown packet with id @", id);
            return null;
        }

        Packet packet = entry.constructor.get();
        packetBuffer.position(0);
        packet.read(reads.get(), length);

        return packet;
    }

    @Shadow
    public abstract FrameworkMessage readFramework(ByteBuffer buffer);

    @Shadow
    public abstract void writeFramework(ByteBuffer buffer, FrameworkMessage message);

}
