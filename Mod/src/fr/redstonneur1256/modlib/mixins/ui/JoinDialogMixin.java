package fr.redstonneur1256.modlib.mixins.ui;

import arc.scene.ui.layout.Collapser;
import arc.scene.ui.layout.Table;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.JoinDialog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JoinDialog.class)
public abstract class JoinDialogMixin {

    @Inject(
            method = "*(Ljava/lang/String;ZLarc/scene/ui/layout/Collapser;Larc/scene/ui/layout/Table;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Larc/scene/ui/layout/Table;button(Larc/scene/style/Drawable;Larc/scene/ui/ImageButton$ImageButtonStyle;Ljava/lang/Runnable;)Larc/scene/ui/layout/Cell;",
                    ordinal = 1
            )
    )
    private void injectRefreshButton(String label, boolean eye, Collapser coll, Table name, CallbackInfo ci) {
        name.button(Icon.refresh, Styles.emptyi, () -> {
            switch (label) {
                case "@servers.local.steam":
                case "@servers.local":
                    refreshLocal();
                    break;
                case "@servers.remote":
                    refreshRemote();
                    break;
                case "@servers.global":
                    refreshCommunity();
                    break;
            }
        }).size(40f).right().padRight(3).tooltip("@modlib.servers.refresh");
    }

    @Shadow
    abstract void refreshLocal();

    @Shadow
    abstract void refreshRemote();

    @Shadow
    abstract void refreshCommunity();

}
