package fr.redstonneur1256.modlib.patch;

import arc.struct.ObjectMap;
import mindustry.mod.Mods;

public interface PlatformLoader {

    ObjectMap<Mods.ModMeta, MultiClassLoader> createApplyModPatch() throws Exception;

}
