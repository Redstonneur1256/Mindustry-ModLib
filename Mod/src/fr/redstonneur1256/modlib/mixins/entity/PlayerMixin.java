package fr.redstonneur1256.modlib.mixins.entity;

import fr.redstonneur1256.modlib.net.NetworkDebuggable;
import mindustry.gen.Player;
import mindustry.net.NetConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Player.class)
public class PlayerMixin implements NetworkDebuggable {

    @Shadow
    public transient NetConnection con;

    @Override
    public long getPing() {
        return con instanceof NetworkDebuggable ? ((NetworkDebuggable) con).getPing() : -1;
    }

}
