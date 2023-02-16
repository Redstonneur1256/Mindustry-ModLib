package fr.redstonneur1256.modlib.launcher;

import arc.Core;
import arc.files.Fi;
import arc.util.Log;
import arc.util.OS;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.core.Version;
import org.jetbrains.annotations.ApiStatus;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@ApiStatus.Internal
public class LauncherInitializer {

    public static boolean isInitialized() {
        try {
            Class.forName("fr.redstonneur1256.modlib.launcher.ModLibLauncher");
            return true;
        } catch(ClassNotFoundException ignored) {
            return false;
        }
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
            }
            launcherFile.write(stream, false);

            // Simulate the launch has ended before restarting the game to avoid all mods being disabled
            Vars.finishLaunch();

            List<String> command = new ArrayList<>(8);
            command.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java" + (OS.isWindows ? ".exe" : ""));
            if(OS.isMac) {
                command.add("-XstartOnFirstThread");
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
            Vars.ui.showException("ModLib failed to initialize, mods depending on it will not work properly.", throwable);
        }
    }

}
