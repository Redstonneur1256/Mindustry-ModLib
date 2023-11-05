package fr.redstonneur1256.modlib.mixins.arc;

import arc.Events;
import arc.func.Cons;
import arc.util.Log;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Events.class)
public class EventsMixin {

    @Redirect(method = "fire(Ljava/lang/Enum;)V", at = @At(value = "INVOKE", target = "Larc/func/Cons;get(Ljava/lang/Object;)V"), require = 0)
    private static <T> void redirectFireEnum(Cons<T> instance, T t) {
        try {
            instance.get(t);
        } catch (Throwable throwable) {
            Log.err("Failed to dispatch event listener " + instance, throwable);
        }
    }

    @Redirect(method = "fire(Ljava/lang/Class;Ljava/lang/Object;)V", at = @At(value = "INVOKE", target = "Larc/func/Cons;get(Ljava/lang/Object;)V"), require = 0)
    private static <T> void redirectFire(Cons<T> instance, T t) {
        try {
            instance.get(t);
        } catch (Throwable throwable) {
            Log.err("Failed to dispatch event listener " + instance, throwable);
        }
    }

}
