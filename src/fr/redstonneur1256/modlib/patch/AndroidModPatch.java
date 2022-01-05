package fr.redstonneur1256.modlib.patch;

import arc.Core;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Reflect;
import fr.redstonneur1256.modlib.ModLib;
import mindustry.Vars;
import mindustry.mod.Mod;
import mindustry.mod.Mods;

import java.io.File;
import java.lang.reflect.Constructor;

public class AndroidModPatch implements PlatformLoader {

    @Override
    public ObjectMap<Mods.ModMeta, MultiClassLoader> createApplyModPatch() throws Exception {
        String filesDir = ((File) Class.forName("android.content.Context")
                .getDeclaredMethod("getFilesDir")
                .invoke(Core.app))
                .getPath();

        Constructor<?> dexClassLoader = Class.forName("dalvik.system.DexClassLoader")
                .getDeclaredConstructor(String.class, String.class, String.class, ClassLoader.class);


        Mods.LoadedMod modLib = Vars.mods.getMod(ModLib.class);
        ClassLoader loader = modLib.loader;

        ObjectMap<Class<?>, Mods.ModMeta> metas = Reflect.get(Mods.class, Vars.mods, "metas");
        Seq<Mods.LoadedMod> mods = Vars.mods.list();

        ObjectMap<Mods.ModMeta, MultiClassLoader> loaders = new ObjectMap<>();

        for(int i = 0; i < mods.size; i++) {
            Mods.LoadedMod mod = mods.get(i);
            if(mod.loader == null || mod.state != Mods.ModState.enabled || mod == modLib) {
                continue;
            }

            metas.remove(mod.main.getClass());

            // Create a new classloader
            MultiClassLoader multiLoader = new MultiClassLoader(loader);

            ClassLoader newModLoader = (ClassLoader) dexClassLoader.newInstance(mod.file.file().getPath(), filesDir, null, multiLoader);
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
