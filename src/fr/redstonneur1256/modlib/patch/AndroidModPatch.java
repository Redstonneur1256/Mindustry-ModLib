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

public class AndroidModPatch {

    /**
     * Reload mods with classloaders with this mod classloader as parent
     * FIXME: Android crashes with NoClassDefFoundError
     */
    public static void applyPatch() throws Exception {
        Mods.LoadedMod modLibrary = Vars.mods.getMod(ModLib.class);
        ClassLoader libLoader = modLibrary.loader;

        ObjectMap<Class<?>, Mods.ModMeta> metas = Reflect.get(Mods.class, Vars.mods, "metas");

        Seq<Mods.LoadedMod> mods = Vars.mods.list();

        // String filesDir = ((Context) Core.app).getFilesDir().getPath();

        String filesDir =
                ((File) Class.forName("android.content.Context")
                        .getDeclaredMethod("getFilesDir")
                        .invoke(Core.app))
                        .getPath();

        Constructor<?> dexClassLoader = Class.forName("dalvik.system.DexClassLoader")
                .getDeclaredConstructor(String.class, String.class, String.class, ClassLoader.class);

        for(int i = 0; i < mods.size; i++) {
            Mods.LoadedMod mod = mods.get(i);
            if(mod.loader == null || mod.state != Mods.ModState.enabled || mod == modLibrary) {
                continue;
            }
            metas.remove(mod.main.getClass());

            // Don't use Vars.platform.loadJar() because the method signature changed

            ClassLoader loader = (ClassLoader) dexClassLoader.newInstance(mod.file.path(), filesDir, null, libLoader);
            // DexClassLoader loader = new DexClassLoader(mod.file.path(), filesDir, null, libLoader);

            mod.loader = loader;

            Mod main = (Mod) loader.loadClass(mod.meta.main).newInstance();
            mods.set(i, new Mods.LoadedMod(mod.file, mod.root, main, loader, mod.meta));

            metas.put(main.getClass(), mod.meta);
        }
    }

}
