package fr.redstonneur1256.modlib.mixins;

import arc.KeyBinds;
import arc.input.InputDevice;
import arc.input.KeyCode;
import arc.struct.ObjectMap;
import fr.redstonneur1256.modlib.key.KeyBindAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

@Mixin(KeyBinds.class)
public class KeyBindsMixin implements KeyBindAccessor {

    @Shadow
    private ObjectMap<KeyBinds.KeyBind, ObjectMap<InputDevice.DeviceType, KeyBinds.Axis>> defaultCache;
    @Shadow
    private KeyBinds.KeyBind[] definitions;

    @Inject(method = "setDefaults", at = @At("RETURN"))
    private void setDefaults(KeyBinds.KeyBind[] defs, KeyBinds.Section[] sectionArr, CallbackInfo ci) {
        // Since setDefault is called with Binding.values() the array is an array of Binding inside which we cannot store
        // our own keybindings, this forces it to be a KeyBind array

        definitions = new KeyBinds.KeyBind[defs.length];
        System.arraycopy(defs, 0, definitions, 0, defs.length);
    }

    @Override
    public void registerKeyBinds(KeyBinds.KeyBind[] binds) {
        int previousLength = definitions.length;
        definitions = Arrays.copyOf(definitions, previousLength + binds.length);
        System.arraycopy(binds, 0, definitions, previousLength, binds.length);

        for(KeyBinds.KeyBind def : binds) {
            defaultCache.put(def, new ObjectMap<>());
            for(InputDevice.DeviceType type : InputDevice.DeviceType.values()) {
                KeyBinds.Axis axis = def.defaultValue(type) instanceof KeyBinds.Axis ?
                        (KeyBinds.Axis) def.defaultValue(type) :
                        new KeyBinds.Axis((KeyCode) def.defaultValue(type));

                defaultCache.get(def).put(type, axis);
            }
        }
    }

}
