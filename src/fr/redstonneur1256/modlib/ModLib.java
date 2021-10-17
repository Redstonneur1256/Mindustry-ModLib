package fr.redstonneur1256.modlib;

import arc.Events;
import arc.util.Log;
import fr.redstonneur1256.modlib.net.PacketManager;
import fr.redstonneur1256.modlib.patch.DesktopModPatch;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.mod.Mod;
import mindustry.mod.Mods;

public class ModLib extends Mod {

    private static boolean hasGameStarted;

    static {
        hasGameStarted = Vars.launchIDFile == null || !Vars.launchIDFile.exists();
        Events.run(EventType.ClientLoadEvent.class, () -> hasGameStarted = true);
    }

    /**
     * Initialize all modules from the library
     *
     * @see PacketManager#initialize()
     */
    public static void initialize() {
        PacketManager.initialize();
    }

    /**
     * @return if the game is fully started or still on loading screen
     */
    public static boolean hasGameStarted() {
        return hasGameStarted;
    }

    @Override
    public void init() {
        // This mod name starts with "!" so it should always be loaded first, so it can change the classloader
        // to allow other mods to interact with each other without creating a NoClassDefFoundError

        try {
            if(Vars.android) {
                // AndroidModPatch.applyPatch();
                Vars.ui.showErrorMessage("ModLibrary isn't available on mobile for now.");
                return;
            }else {
                DesktopModPatch.applyPatch();
            }
        }catch(Exception exception) {
            Log.err("Failed to apply class loader patch", exception);

            Vars.ui.showException("[red]Failed to apply class loader patch", exception);

            Mods.LoadedMod mod = Vars.mods.getMod(ModLib.class);
            mod.missingDependencies.add("[red]See game log for more information");
            mod.state = Mods.ModState.missingDependencies;
            return;
        }

        initialize();
    }

}
