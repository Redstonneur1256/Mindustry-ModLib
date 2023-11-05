package fr.redstonneur1256.examplemod.mixins;

import arc.scene.Element;
import arc.scene.ui.Label;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Table;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.ui.fragments.MenuFragment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Example mixins that injects in the main menu to make all the buttons rainbow
 */
@Mixin(MenuFragment.class)
public class ExampleMixin {

    @Inject(method = "buttons", at = @At("TAIL"))
    private void buttons(Table table, MenuFragment.MenuButton[] buttons, CallbackInfo ci) {
        for (Element child : table.getChildren()) {
            if (child instanceof TextButton) {
                Label label = ((TextButton) child).getLabel();
                String text = label.getText().toString();
                label.setText(() -> String.format("[#%06X]%s", Tmp.c1.fromHsv(Time.time, 1, 1).rgb888(), text));
            }
        }
    }

}
