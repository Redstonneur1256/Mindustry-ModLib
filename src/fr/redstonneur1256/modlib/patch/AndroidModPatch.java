package fr.redstonneur1256.modlib.patch;

import android.content.Context;
import arc.Core;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Reflect;
import dalvik.system.DexClassLoader;
import fr.redstonneur1256.modlib.ModLib;
import mindustry.Vars;
import mindustry.mod.Mod;
import mindustry.mod.Mods;

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

        String filesDir = ((Context) Core.app).getFilesDir().getPath();

        for(int i = 0; i < mods.size; i++) {
            Mods.LoadedMod mod = mods.get(i);
            if(mod.loader == null || mod.state != Mods.ModState.enabled || mod == modLibrary) {
                continue;
            }
            metas.remove(mod.main.getClass());

            // Don't use Vars.platform.loadJar() because the method signature changed

            DexClassLoader loader = new DexClassLoader(mod.file.path(), filesDir, null, libLoader);

            mod.loader = loader;

            Mod main = (Mod) loader.loadClass(mod.meta.main).newInstance();
            mods.set(i, new Mods.LoadedMod(mod.file, mod.root, main, loader, mod.meta));

            metas.put(main.getClass(), mod.meta);
        }
    }

}
