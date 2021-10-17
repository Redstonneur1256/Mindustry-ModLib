package fr.redstonneur1256.modlib.patch;

import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Reflect;
import fr.redstonneur1256.modlib.ModLib;
import mindustry.Vars;
import mindustry.mod.Mod;
import mindustry.mod.Mods;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

public class DesktopModPatch {

    /**
     * Reload mods with a common classloader having this mod classloader as parent
     */
    public static void applyPatch() throws Exception {
        Mods.LoadedMod modLibrary = Vars.mods.getMod(ModLib.class);
        URLClassLoader libLoader = (URLClassLoader) modLibrary.loader;

        ObjectMap<Class<?>, Mods.ModMeta> metas = Reflect.get(Mods.class, Vars.mods, "metas");

        Seq<Mods.LoadedMod> mods = Vars.mods.list();
        Seq<URL> urls = new Seq<>();

        // Dispose all mods and get jar urls
        for(Mods.LoadedMod mod : mods) {
            if(mod.loader == null || mod.state != Mods.ModState.enabled || mod == modLibrary) {
                continue;
            }

            URLClassLoader otherLoader = (URLClassLoader) mod.loader;
            urls.addAll(otherLoader.getURLs());

            // Remove the actual meta
            metas.remove(mod.main.getClass());

            try {
                otherLoader.close();
            }catch(IOException exception) {
                Log.err("Failed to close mod class loader for mod " + mod.name, exception);
            }
        }

        // Load all mod with the new classloader
        URLClassLoader loader = new URLClassLoader(urls.toArray(URL.class), libLoader);

        for(int i = 0; i < mods.size; i++) {
            Mods.LoadedMod mod = mods.get(i);
            if(mod.loader == null || mod.state != Mods.ModState.enabled || mod == modLibrary) {
                continue;
            }

            Mod main = (Mod) loader.loadClass(mod.meta.main).newInstance();
            mods.set(i, new Mods.LoadedMod(mod.file, mod.root, main, loader, mod.meta));
            metas.put(main.getClass(), mod.meta);
        }
    }

}
