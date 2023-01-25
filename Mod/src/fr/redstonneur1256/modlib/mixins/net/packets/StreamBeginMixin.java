package fr.redstonneur1256.modlib.mixins.net.packets;

import arc.util.io.Reads;
import arc.util.io.Writes;
import fr.redstonneur1256.modlib.net.packet.PacketTypeAccessor;
import fr.redstonneur1256.modlib.util.NetworkUtil;
import mindustry.net.Packets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Packets.StreamBegin.class)
public class StreamBeginMixin implements PacketTypeAccessor {

    @Shadow
    public int id;
    @Shadow
    public int total;

    private int intType;

    /**
     * @author Redstonneur1256
     * @reason Read extended id when needed
     */
    @Overwrite
    public void read(Reads reads) {
        id = reads.i();
        total = reads.i();
        intType = NetworkUtil.readExtendedByte(reads::b);
    }

    /**
     * @author Redstonneur1256
     * @reason Write extended id when needed
     */
    @Overwrite
    public void write(Writes writes) {
        writes.i(id);
        writes.i(total);
        NetworkUtil.writeExtendedByte(writes::b, intType);
    }

    @Override
    public int getType() {
        return intType;
    }

    @Override
    public void setType(int type) {
        this.intType = type;
    }

}
