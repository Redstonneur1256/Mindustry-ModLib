package fr.redstonneur1256.modlib.mixins.arc;

import arc.Application;
import arc.util.Log;
import arc.util.Timer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Timer.class)
public class TimerMixin {

    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Larc/Application;post(Ljava/lang/Runnable;)V"), require = 0)
    private void redirectUpdatePost(Application instance, Runnable runnable) {
        instance.post(() -> {
            try {
                runnable.run();
            } catch(Throwable throwable) {
                Log.err("Failed to dispatch timer task " + runnable, throwable);
            }
        });
    }

}
