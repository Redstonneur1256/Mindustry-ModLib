package fr.redstonneur1256.modlib.mixins.ui;

import mindustry.gen.Icon;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.ui.dialogs.JoinDialog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JoinDialog.class)
public abstract class JoinDialogMixin extends BaseDialog {

    @Shadow
    int refreshes;

    public JoinDialogMixin(String title) {
        super(title);
    }

    @Inject(method = "setup", at = @At("TAIL"))
    private void setup(CallbackInfo ci) {
        cont.button(Icon.refresh, () -> {
            refreshes++;
            refreshRemote();
            refreshCommunity();
        }).size(80);
    }

    @Shadow
    abstract void refreshRemote();

    @Shadow
    abstract void refreshCommunity();

}
