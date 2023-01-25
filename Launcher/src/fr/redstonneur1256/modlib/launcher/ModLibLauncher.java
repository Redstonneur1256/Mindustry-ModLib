package fr.redstonneur1256.modlib.launcher;

import com.google.gson.Gson;
import fr.redstonneur1256.modlib.common.ModLibProperties;
import fr.redstonneur1256.modlib.launcher.mixin.MixinClassLoader;
import fr.redstonneur1256.modlib.launcher.mixin.ModLibMixinService;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.service.MixinService;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ModLibLauncher {

    private static final String[] META_FILES = { "mod.json", "mod.hjson", "plugin.json", "plugin.hjson" };

    private static final String SERVER_MAIN_CLASS = "mindustry.server.ServerLauncher";
    private static final String MINDUSTRY_MAIN_CLASS = "mindustry.desktop.DesktopLauncher";

    public static MixinClassLoader loader;
    public static boolean restartGame = false;
    /**
     * Should we directly restart the launcher or the full game
     */
    public static boolean fastRestart = true;

    public static void main(String[] args) {
        if(args.length < 3) {
            System.exit(1);
        }
        try {
            File mindustryExecutable = new File(args[0]);
            File gameDirectory = new File(args[1]);
            boolean isServer = Boolean.parseBoolean(args[2]);

            File modsDirectory = new File(gameDirectory, "mods");

            System.out.println("  __  __           _ _      _ _     \n" +
                    " |  \\/  |         | | |    (_) |    \n" +
                    " | \\  / | ___   __| | |     _| |__  \n" +
                    " | |\\/| |/ _ \\ / _` | |    | | '_ \\ \n" +
                    " | |  | | (_) | (_| | |____| | |_) |\n" +
                    " |_|  |_|\\___/ \\__,_|______|_|_.__/");
            System.out.printf("Launcher version %s (build %s), %s days old%n", ModLibProperties.VERSION,
                    ModLibProperties.BUILD, ChronoUnit.DAYS.between(ModLibProperties.BUILT, Instant.now()));

            System.out.printf("Bootstrapping mixins%n");

            MixinBootstrap.init();
            ModLibMixinService service = (ModLibMixinService) MixinService.getService();
            MixinEnvironment.getCurrentEnvironment().setSide(isServer ? MixinEnvironment.Side.SERVER : MixinEnvironment.Side.CLIENT);

            loader = new MixinClassLoader(new URL[] { mindustryExecutable.toURI().toURL() }, ModLibLauncher.class.getClassLoader());

            // Add own configuration
            Mixins.addConfiguration("launcher.mixins.json");

            Gson gson = new Gson();
            List<String> extraArguments = new ArrayList<>(Arrays.asList(Arrays.copyOfRange(args, 3, args.length)));

            // Add mods to classloader and load mixins
            File[] modFiles = modsDirectory.listFiles();
            if(modFiles != null) {
                for(File file : modFiles) {
                    if(file.isDirectory()) {
                        continue;
                    }
                    try(ZipFile zip = new ZipFile(file)) {
                        loader.addURL(file.toURI().toURL());
                        Optional<ZipEntry> optional = Arrays.stream(META_FILES)
                                .map(zip::getEntry)
                                .filter(Objects::nonNull)
                                .findFirst();
                        if(!optional.isPresent()) {
                            continue;
                        }
                        try(Reader reader = new InputStreamReader(zip.getInputStream(optional.get()))) {
                            JsonObject metadata = JsonValue.readHjson(reader).asObject();

                            JsonValue gameArguments = metadata.get("gameArguments");
                            if(gameArguments != null && gameArguments.isArray()) {
                                extraArguments.addAll(gameArguments
                                        .asArray()
                                        .values()
                                        .stream()
                                        .map(JsonValue::asString)
                                        .collect(Collectors.toList()));
                            }

                            Object name = metadata.get("name").asString();
                            String mixinsPath = name + ".mixins.json";

                            if(zip.getEntry(mixinsPath) != null) {
                                Mixins.addConfiguration(mixinsPath);
                            }
                        }
                    } catch(IOException ignored) {
                        // Not a zipped mod, ignore
                    }
                }
            }

            service.onGameStart();

            System.out.printf("Attempting to launch the game with arguments %s%n", String.join(" ", extraArguments));

            String mainClassName = isServer ? SERVER_MAIN_CLASS : MINDUSTRY_MAIN_CLASS;
            Class<?> mainClass = loader.loadClass(mainClassName);
            Method mainMethod = mainClass.getDeclaredMethod("main", String[].class);

            mainMethod.invoke(null, (Object) extraArguments.toArray(new String[0]));

            if(!restartGame) {
                return;
            }

            System.out.println("Mindustry exited for restart, reopening the launcher.");

            // Do not reference the OS class directly as this would cause it to be loaded as the same time as this class
            // potentially causing some mixins not to apply
            boolean windows = (Boolean) loader.loadClass("arc.util.OS").getDeclaredField("isWindows").get(null);
            boolean mac = (Boolean) loader.loadClass("arc.util.OS").getDeclaredField("isMac").get(null);

            List<String> command = new ArrayList<>(8);
            command.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java" + (windows ? ".exe" : ""));
            if(mac) {
                command.add("-XstartOnFirstThread");
            }
            command.add("-jar");

            if(fastRestart) {
                command.add(new File(ModLibLauncher.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getAbsolutePath());
                command.addAll(Arrays.asList(args));
            } else {
                command.add(mindustryExecutable.getAbsolutePath());
            }

            Runtime.getRuntime().exec(command.toArray(new String[0]));
        } catch(Throwable throwable) {
            System.err.println("Failed to launch the game:");
            throwable.printStackTrace();
            System.exit(1);
        }
    }

}
