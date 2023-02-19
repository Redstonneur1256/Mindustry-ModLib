package fr.redstonneur1256.modlib.mixins.ui;

import arc.scene.ui.Dialog;
import mindustry.ui.dialogs.KeybindDialog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(KeybindDialog.class)
public abstract class KeyBindDialogMixin extends Dialog {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(CallbackInfo ci) {
        shown(this::setup);
    }

    @Shadow
    protected abstract void setup();

}

