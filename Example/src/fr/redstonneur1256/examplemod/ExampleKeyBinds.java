package fr.redstonneur1256.examplemod;

import arc.KeyBinds;
import arc.input.InputDevice;
import arc.input.KeyCode;
import fr.redstonneur1256.modlib.key.KeyBindManager;

public enum ExampleKeyBinds implements KeyBinds.KeyBind {

    demo(KeyCode.j, "example-mod");

    private final KeyBinds.KeybindValue defaultValue;
    private final String category;

    ExampleKeyBinds(KeyBinds.KeybindValue defaultValue, String category){
        this.defaultValue = defaultValue;
        this.category = category;
    }

    ExampleKeyBinds(KeyBinds.KeybindValue defaultValue){
        this(defaultValue, null);
    }


    @Override
    public KeyBinds.KeybindValue defaultValue(InputDevice.DeviceType type) {
        return defaultValue;
    }

    @Override
    public String category() {
        return category;
    }

    public static void register() {
        KeyBindManager.registerKeyBinds(values());
    }

}
