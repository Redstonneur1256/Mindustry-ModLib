package fr.redstonneur1256.modlib;

import fr.redstonneur1256.modlib.launcher.LauncherInitializer;
import fr.redstonneur1256.modlib.net.MNet;
import fr.redstonneur1256.modlib.ui.MUI;
import mindustry.Vars;
import mindustry.mod.Mod;

public class ModLib extends Mod {

    public ModLib() {
        LauncherInitializer.initialize();
    }

    @Override
    public void init() {
        if (!LauncherInitializer.isInitialized()) {
            return;
        }

        MVars.net = new MNet();
        if (!Vars.headless) {
            MVars.ui = new MUI();
        }
    }

    public static String getVersion() {
        return Vars.mods.getMod(ModLib.class).meta.version;
    }

}
