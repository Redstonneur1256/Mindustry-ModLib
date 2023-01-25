package fr.redstonneur1256.modlib.mixins.ui;

import fr.redstonneur1256.modlib.launcher.ModLibLauncher;
import mindustry.ui.dialogs.ModsDialog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModsDialog.class)
public class ModsDialogMixin {

    @Inject(method = "reload", at = @At("HEAD"))
    public void reload(CallbackInfo ci) {
        ModLibLauncher.restartGame = true;
        ModLibLauncher.fastRestart = false;
    }

}
