package fr.redstonneur1256.modlib;

import arc.Events;
import arc.struct.ObjectMap;
import arc.util.Log;
import fr.redstonneur1256.modlib.net.PacketManager;
import fr.redstonneur1256.modlib.patch.AndroidModPatch;
import fr.redstonneur1256.modlib.patch.DesktopModPatch;
import fr.redstonneur1256.modlib.patch.MultiClassLoader;
import fr.redstonneur1256.modlib.patch.PlatformLoader;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.mod.Mod;
import mindustry.mod.Mods;

public class ModLib extends Mod {

    private static boolean hasGameStarted;
    private static ObjectMap<Mods.ModMeta, MultiClassLoader> loaders;

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

    public static void addLibrary(ClassLoader loader) {
        for(MultiClassLoader classLoader : loaders.values()) {
            classLoader.addClassLoader(loader);
        }
    }

    @Override
    public void init() {
        // This mod name starts with "!" so it should always be loaded first, so it can change the classloader
        // to allow other mods to interact with each other without creating a NoClassDefFoundError

        try {
            PlatformLoader loader = Vars.android ? new AndroidModPatch() : new DesktopModPatch();
            loaders = loader.createApplyModPatch();
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
