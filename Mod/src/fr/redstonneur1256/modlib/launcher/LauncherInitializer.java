package fr.redstonneur1256.modlib.launcher;

import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.util.Log;
import arc.util.OS;
import arc.util.Reflect;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.core.Version;
import mindustry.game.EventType;
import org.jetbrains.annotations.ApiStatus;

import java.io.File;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ApiStatus.Internal
public class LauncherInitializer {

    public static boolean isInitialized() {
        return doesClassExist("fr.redstonneur1256.modlib.launcher.ModLibLauncher");
    }

    public static boolean isBundledJVM() {
        // Bundled JVM is stripped down and only contains java.base, this might not be the best way to detect it,
        // but it's a working way
        return !doesClassExist("java.util.logging.Logger");
    }

    public static void initialize() {
        if(isInitialized()) {
            return;
        }

        Log.info("[ModLib] Restarting game with ModLib launcher.");

        Vars.tmpDirectory.mkdirs();
        Fi launcherFile = Vars.tmpDirectory.child("ModLib-launcher.jar");

        try {
            Log.info("[ModLib] Extracting ModLib launcher...");
            InputStream stream = LauncherInitializer.class.getResourceAsStream("/ModLib-launcher.jar");
            if(stream == null) {
                Log.warn("[ModLib] Missing internal launcher file");
                throw new RuntimeException("Missing internal launcher file");
            }
            launcherFile.write(stream, false);

            // Simulate the launch has ended before restarting the game to avoid all mods being disabled
            Vars.finishLaunch();

            // Save the settings and disable auto-saving before the new process is launched to avoid them being saved at
            // the same time they are loaded by the launcher possibly causing an EOF on some machines
            Core.settings.autosave();
            Core.settings.setAutosave(false);

            List<String> command = new ArrayList<>(8);
            command.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java" + (OS.isWindows ? ".exe" : ""));

            if(isBundledJVM()) {
                command.add("-Dhttps.protocols=TLSv1.2,TLSv1.1,TLSv1");
            }

            if(OS.isMac) {
                command.add("-XstartOnFirstThread");
            }
            try {
                command.addAll(ManagementFactory.getRuntimeMXBean().getInputArguments());
            } catch(Throwable exception) {
                Log.err("Unable to add current java arguments", exception);
            }

            command.add("-jar");
            command.add(launcherFile.absolutePath());

            command.add(new File(Vars.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getAbsolutePath());
            command.add(Vars.dataDirectory.absolutePath());
            command.add(String.valueOf(Vars.headless));
            command.add(Version.combined());

            if(Core.settings.getBool("modlib.antialiasing", false)) {
                command.add("-antialias");
            }
            if(Core.settings.getBool("modlib.debug", false)) {
                command.add("-debug");
            }

            if(Vars.headless) {
                try {
                    Class<?> serverLauncher = Class.forName("mindustry.server.ServerLauncher");
                    String[] args = Reflect.get(serverLauncher, "args");
                    command.addAll(Arrays.asList(args));
                } catch(ClassNotFoundException | RuntimeException exception) {
                    Log.err("Could not get servers arguments", exception);
                }
            }

            Log.info("[ModLib] Running command '@'", Strings.join("' '", command));

            ProcessBuilder builder = new ProcessBuilder(command);

            boolean inherit = Vars.headless || Boolean.getBoolean("modlib.inherit");

            if(inherit) {
                builder.inheritIO();
            }

            Process process = builder.start();
            if(inherit) {
                process.waitFor();

                // load the settings that were saved from the new launched process because calling Core.app.exit() will
                // save them causing the new settings to be overwritten
                Core.settings.load();
            }

            if(Vars.headless) {
                System.exit(0);
            }
            Core.app.exit();
        } catch(Throwable throwable) {
            // Startup failed, revert changes that could cause problems
            Core.settings.setAutosave(true);

            if(Vars.headless) {
                Log.err("ModLib failed to initialize", throwable);
                return;
            }
            Events.run(EventType.ClientLoadEvent.class, () -> Vars.ui.showException("ModLib failed to initialize, mods depending on it will not work properly.", throwable));
        }
    }

    private static boolean doesClassExist(String name) {
        try {
            Class.forName(name);
            return true;
        } catch(ClassNotFoundException ignored) {
            return false;
        }
    }

}
