package fr.redstonneur1256.modlib.mixins.gen;

import arc.audio.Sound;
import arc.struct.IntMap;
import arc.struct.ObjectIntMap;
import mindustry.gen.Sounds;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Sounds.class)
public class SoundsMixin {

    @Shadow
    private static IntMap<Sound> idToSound;
    @Shadow
    private static ObjectIntMap<Sound> soundToId;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void init(CallbackInfo ci) {
        // idToSound is generated upon loading the sounds and is missing on the server, generate it anyway

        for(ObjectIntMap.Entry<Sound> entry : soundToId) {
            idToSound.put(entry.value, entry.key);
        }
    }

}
