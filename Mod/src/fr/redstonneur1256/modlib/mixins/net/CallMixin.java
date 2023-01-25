package fr.redstonneur1256.modlib.mixins.net;

import fr.redstonneur1256.modlib.net.packet.PacketManager;
import mindustry.gen.Call;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Call.class)
public class CallMixin {

    @Inject(method = "registerPackets", at = @At("RETURN"))
    private static void registerPackets(CallbackInfo ci) {
        PacketManager.finalizeRegistration();
    }

}
