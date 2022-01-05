package fr.redstonneur1256.modlib.patch;

import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Reflect;
import fr.redstonneur1256.modlib.ModLib;
import mindustry.Vars;
import mindustry.mod.Mod;
import mindustry.mod.Mods;

import java.net.URL;
import java.net.URLClassLoader;

public class DesktopModPatch implements PlatformLoader {

    @Override
    public ObjectMap<Mods.ModMeta, MultiClassLoader> createApplyModPatch() throws Exception {
        Mods.LoadedMod modLib = Vars.mods.getMod(ModLib.class);
        URLClassLoader loader = (URLClassLoader) modLib.loader;

        ObjectMap<Class<?>, Mods.ModMeta> metas = Reflect.get(Mods.class, Vars.mods, "metas");
        Seq<Mods.LoadedMod> mods = Vars.mods.list();

        ObjectMap<Mods.ModMeta, MultiClassLoader> loaders = new ObjectMap<>();

        for(int i = 0; i < mods.size; i++) {
            Mods.LoadedMod mod = mods.get(i);
            if(mod.loader == null || mod.state != Mods.ModState.enabled || mod == modLib) {
                continue;
            }

            metas.remove(mod.main.getClass());

            // Get the current mod loader and dispose it
            URLClassLoader oldModLoader = (URLClassLoader) mod.loader;
            URL[] urls = oldModLoader.getURLs();
            oldModLoader.close();

            // Create a new classloader
            MultiClassLoader multiLoader = new MultiClassLoader(loader);

            URLClassLoader newModLoader = new URLClassLoader(urls, multiLoader);
            loaders.put(mod.meta, multiLoader);

            // Do not load main class here because resolving it may cause it to try to load classes from other mods
            // before they are added to the multi classloader and available to use
            mod.loader = newModLoader;
        }

        for(ObjectMap.Entry<Mods.ModMeta, MultiClassLoader> entry : loaders) {
            // If the mod is not the one from this MultiClassLoader add his classloader to the MultiClassLoader
            mods.each(mod -> mod.meta != entry.key, mod -> entry.value.addClassLoader(mod.loader));
        }

        for(int i = 0; i < mods.size; i++) {
            Mods.LoadedMod mod = mods.get(i);
            if(mod.loader == null || mod.state != Mods.ModState.enabled || mod == modLib) {
                continue;
            }
            Mod main = (Mod) mod.loader.loadClass(mod.meta.main).newInstance();
            mods.set(i, new Mods.LoadedMod(mod.file, mod.root, main, mod.loader, mod.meta)); // Update the main class instance
            metas.put(main.getClass(), mod.meta);
        }

        return loaders;
    }

}
