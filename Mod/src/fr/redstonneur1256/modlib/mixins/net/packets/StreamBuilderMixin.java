package fr.redstonneur1256.modlib.mixins.net.packets;

import fr.redstonneur1256.modlib.net.ClassEntry;
import fr.redstonneur1256.modlib.net.packet.PacketManager;
import fr.redstonneur1256.modlib.net.packet.PacketTypeAccessor;
import mindustry.net.Packet;
import mindustry.net.Streamable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Mixin(Streamable.StreamBuilder.class)
public class StreamBuilderMixin implements PacketTypeAccessor {

    @Shadow
    @Final
    public ByteArrayOutputStream stream;

    private int extendedType;

    /**
     * @author Redstonneur1256
     * @reason use custom packet registry
     */
    @Overwrite
    public Streamable build() {
        ClassEntry<Packet> entry = PacketManager.getEntry(extendedType);
        assert entry != null : "This streamable builder was created from the received streamable so it has to exist.";
        Streamable streamable = (Streamable) entry.constructor.get();
        streamable.stream = new ByteArrayInputStream(stream.toByteArray());
        return streamable;
    }

    public int getType() {
        return extendedType;
    }

    public void setType(int extendedType) {
        this.extendedType = extendedType;
    }

}
