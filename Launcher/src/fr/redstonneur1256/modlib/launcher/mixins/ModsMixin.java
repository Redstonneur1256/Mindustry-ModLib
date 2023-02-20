package fr.redstonneur1256.modlib.launcher.mixins;

import arc.Core;
import arc.files.Fi;
import arc.files.ZipFi;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Time;
import fr.redstonneur1256.modlib.launcher.ModLibLauncher;
import mindustry.Vars;
import mindustry.core.Version;
import mindustry.mod.Mod;
import mindustry.mod.Mods;
import mindustry.mod.Plugin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

import static mindustry.Vars.headless;

@Mixin(Mods.class)
public abstract class ModsMixin {

    @Shadow
    Seq<Mods.LoadedMod> mods;
    @Shadow
    private ObjectMap<Class<?>, Mods.ModMeta> metas;

    @Shadow
    public abstract Mods.ModMeta findMeta(Fi file);

    @Shadow
    public abstract boolean skipModLoading();

    @Shadow
    private boolean requiresReload;

    /**
     * @author Redstonneur1256
     * @reason Use custom classloader
     */
    @Overwrite
    public ClassLoader mainLoader() {
        return ModLibLauncher.loader;
    }

    /**
     * @author Redstonneur1256
     * @reason load the mods using our existing classloader
     */
    @Overwrite
    private Mods.LoadedMod loadMod(Fi sourceFile, boolean overwrite) throws Exception {
        ZipFi zip = null;
        try {
            long start = Time.millis();

            Fi directory = sourceFile.isDirectory() ? sourceFile : (zip = new ZipFi(sourceFile));
            if(directory.list().length == 1 && directory.list()[0].isDirectory()) {
                directory = directory.list()[0];
            }
            Mods.ModMeta meta = findMeta(directory);

            if(meta == null) {
                Log.warn("Mod @ doesn't have a '[mod/plugin].[h]json' file, skipping.", zip);
                throw new Mods.ModLoadException("Invalid file: No mod.json found.");
            }

            String camelized = meta.name.replace(" ", "");
            String mainClass = meta.main == null ? camelized.toLowerCase(Locale.ROOT) + "." + camelized + "Mod" : meta.main;
            String baseName = meta.name.toLowerCase(Locale.ROOT).replace(" ", "-");

            Mods.LoadedMod other = mods.find(mod -> mod.name.equals(baseName));

            if(other != null) {
                //steam mods can't really be deleted, they need to be unsubscribed
                if(overwrite && !other.hasSteamID()) {
                    //close zip file
                    if(other.root instanceof ZipFi) {
                        other.root.delete();
                    }
                    //delete the old mod directory
                    if(other.file.isDirectory()) {
                        other.file.deleteDirectory();
                    } else {
                        other.file.delete();
                    }
                    //unload
                    mods.remove(other);
                } else {
                    throw new Mods.ModLoadException("A mod with the name '" + baseName + "' is already imported.");
                }
            }

            Mod mainMod;
            Fi mainFile = directory;

            // Apparently needed for zip/jar mods, sorcery !
            for(String child : (mainClass.replace('.', '/') + ".class").split("/")) {
                if(!child.isEmpty()) {
                    mainFile = mainFile.child(child);
                }
            }

            //make sure the main class exists before loading it; if it doesn't just don't put it there
            //if the mod is explicitly marked as java, try loading it anyway
            if((mainFile.exists() || meta.java) &&
                    !skipModLoading() &&
                    Core.settings.getBool("mod-" + baseName + "-enabled", true) &&
                    Version.isAtLeast(meta.minGameVersion) &&
                    (meta.getMinMajor() >= 136 || headless)) {

                Class<?> main;

                try {
                    main = Class.forName(mainClass, true, ModLibLauncher.loader);
                } catch(ClassNotFoundException exception) {
                    // Mod might be imported right now so is not present
                    ModLibLauncher.loader.addURL(sourceFile.file().toURI().toURL());
                    main = Class.forName(mainClass, true, ModLibLauncher.loader);
                }


                //detect mods that incorrectly package mindustry in the jar
                if((main.getSuperclass().getName().equals("mindustry.mod.Plugin") || main.getSuperclass().getName().equals("mindustry.mod.Mod")) &&
                        main.getSuperclass().getClassLoader() != Mod.class.getClassLoader()) {
                    throw new Mods.ModLoadException(
                            "This mod/plugin has loaded Mindustry dependencies from its own class loader. " +
                                    "You are incorrectly including Mindustry dependencies in the mod JAR - " +
                                    "make sure Mindustry is declared as `compileOnly` in Gradle, and that the JAR is created with `runtimeClasspath`!"
                    );
                }

                metas.put(main, meta);
                mainMod = (Mod) main.getDeclaredConstructor().newInstance();
            } else {
                mainMod = null;
            }

            //all plugins are hidden implicitly
            if(mainMod instanceof Plugin) {
                meta.hidden = true;
            }

            //disallow putting a description after the version
            if(meta.version != null) {
                int line = meta.version.indexOf('\n');
                if(line != -1) {
                    meta.version = meta.version.substring(0, line);
                }
            }

            //skip mod loading if it failed
            if(skipModLoading()) {
                Core.settings.put("mod-" + baseName + "-enabled", false);
            }

            if(!headless && Core.settings.getBool("mod-" + baseName + "-enabled", true)) {
                Log.info("Loaded mod '@' in @ms", meta.name, Time.timeSinceMillis(start));
            }

            return new Mods.LoadedMod(sourceFile, directory, mainMod, ModLibLauncher.loader, meta);
        } catch(Throwable throwable) {
            if(zip != null) {
                zip.delete();
            }
            throw throwable;
        }
    }

    @Inject(method = "removeMod", at = @At("HEAD"), cancellable = true)
    public void removeMod(Mods.LoadedMod mod, CallbackInfo ci) {
        ci.cancel();

        if(mod.loader != null) {
            Vars.ui.showErrorMessage("@ui.mods.mod.deleteLoaded");
            return;
        }

        if(mod.root instanceof ZipFi) {
            mod.root.delete(); // this actually closes the file handle
        }

        mod.file.deleteDirectory();

        // When a mod is deleted the game is automatically restarted afterward, use the new process to delete
        // the files once we are sure they are closed
        ModLibLauncher.filesToDelete.add(mod.file.file());

        mods.remove(mod);
        mod.dispose();
        requiresReload = true;
    }

}
