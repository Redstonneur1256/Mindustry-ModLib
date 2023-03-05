package fr.redstonneur1256.modlib.mixins.ui;

import arc.graphics.Color;
import arc.scene.ui.layout.Table;
import fr.redstonneur1256.modlib.launcher.ModLibLauncher;
import mindustry.mod.Mods;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.ui.dialogs.ModsDialog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModsDialog.class)
public class ModsDialogMixin extends BaseDialog {

    public ModsDialogMixin(String title) {
        super(title);
    }

    @Inject(method = "reload", at = @At("HEAD"))
    public void reload(CallbackInfo ci) {
        ModLibLauncher.launcher.restartGame = true;
        ModLibLauncher.launcher.fastRestart = false;
    }

    @Inject(
            method = "*(Lmindustry/mod/Mods$LoadedMod;Larc/scene/ui/layout/Table;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lmindustry/ui/dialogs/ModsDialog;getStateDetails(Lmindustry/mod/Mods$LoadedMod;)Ljava/lang/String;",
                    ordinal = 0,
                    shift = At.Shift.BEFORE
            )
    )
    public void showMod(Mods.LoadedMod mod, Table desc, CallbackInfo ci) {
        desc.add("@ui.mods.mod.version").padRight(10).color(Color.gray).top();
        desc.row();
        desc.add(mod.meta.version).growX().wrap().padTop(2);
        desc.row();
    }

}
