package fr.redstonneur1256.modlib.launcher;

import com.google.gson.Gson;
import fr.redstonneur1256.modlib.ModLibProperties;
import fr.redstonneur1256.modlib.function.ThrowingBiConsumer;
import fr.redstonneur1256.modlib.launcher.mixin.MixinClassLoader;
import fr.redstonneur1256.modlib.launcher.mixin.ModLibMixinService;
import fr.redstonneur1256.modlib.launcher.util.ArcSettings;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.hjson.ParseException;
import org.jetbrains.annotations.ApiStatus;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.service.MixinService;

import javax.swing.*;
import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class ModLibLauncher {

    private static final String[] META_FILES = { "mod.json", "mod.hjson", "plugin.json", "plugin.hjson" };

    private static final String SERVER_MAIN_CLASS = "mindustry.server.ServerLauncher";
    private static final String MINDUSTRY_MAIN_CLASS = "mindustry.desktop.DesktopLauncher";

    public static MixinClassLoader loader;
    @ApiStatus.Internal
    public static List<File> filesToDelete = new ArrayList<>();
    public static boolean restartGame = false;
    /**
     * Should we directly restart the launcher or the full game
     */
    public static boolean fastRestart = true;

    public static void main(String[] args) {
        if(args.length < 4) {
            System.exit(1);
        }
        File mindustryExecutable = new File(args[0]);
        File gameDirectory = new File(args[1]);
        boolean isServer = Boolean.parseBoolean(args[2]);
        String version = args[3];
        try {
            File modsDirectory = new File(gameDirectory, "mods");

            System.out.println("  __  __           _ _      _ _     \n" +
                    " |  \\/  |         | | |    (_) |    \n" +
                    " | \\  / | ___   __| | |     _| |__  \n" +
                    " | |\\/| |/ _ \\ / _` | |    | | '_ \\ \n" +
                    " | |  | | (_) | (_| | |____| | |_) |\n" +
                    " |_|  |_|\\___/ \\__,_|______|_|_.__/");
            System.out.printf("Launcher version %s (build %s), %s days old%n", ModLibProperties.VERSION,
                    ModLibProperties.BUILD, ChronoUnit.DAYS.between(ModLibProperties.BUILT, Instant.now()));

            System.out.printf("Loading game settings%n");
            ArcSettings settings = new ArcSettings();
            File settingsFile = new File(gameDirectory, "settings.bin");
            if(settingsFile.exists()) {
                settings.load(settingsFile);
            }

            System.out.printf("Bootstrapping mixins%n");

            MixinBootstrap.init();
            ModLibMixinService service = (ModLibMixinService) MixinService.getService();
            MixinEnvironment.getCurrentEnvironment().setSide(isServer ? MixinEnvironment.Side.SERVER : MixinEnvironment.Side.CLIENT);

            loader = new MixinClassLoader(new URL[] { mindustryExecutable.toURI().toURL() }, ModLibLauncher.class.getClassLoader());

            // Add own configuration
            Mixins.addConfiguration("launcher.mixins.json");

            Gson gson = new Gson();
            List<String> extraArguments = new ArrayList<>(Arrays.asList(Arrays.copyOfRange(args, 4, args.length)));

            ThrowingBiConsumer<InputStream, Predicate<String>, IOException> modHandler = (stream, predicate) -> {
                try(Reader reader = new InputStreamReader(stream)) {
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

                    String name = metadata.get("name").asString();

                    boolean modEnabled = settings.get(Boolean.class, "mod-" + name + "-enabled", true);
                    String mixinsPath = name + ".mixins.json";
                    if(modEnabled && predicate.test(mixinsPath)) {
                        Mixins.addConfiguration(mixinsPath);
                    }
                }
            };

            // Add mods to classloader and load mixins
            File[] modFiles = modsDirectory.listFiles();
            if(modFiles != null) {
                for(File file : modFiles) {
                    loader.addURL(file.toURI().toURL());

                    try {
                        if(file.isDirectory()) {
                            Optional<File> optional = Arrays.stream(META_FILES)
                                    .map(meta -> new File(file, meta))
                                    .filter(File::exists)
                                    .findFirst();
                            if(optional.isPresent()) {
                                modHandler.accept(Files.newInputStream(optional.get().toPath()), name -> new File(file, name).exists());
                            }
                            continue;
                        }

                        try(ZipFile zip = new ZipFile(file)) {
                            Optional<ZipEntry> optional = Arrays.stream(META_FILES)
                                    .map(zip::getEntry)
                                    .filter(Objects::nonNull)
                                    .findFirst();
                            if(optional.isPresent()) {
                                modHandler.accept(zip.getInputStream(optional.get()), name -> zip.getEntry(name) != null);
                            }
                        } catch(ZipException exception) {
                            System.err.println("Potentially corrupted jar file " + file);
                            exception.printStackTrace();
                        }
                    } catch(IOException | ParseException exception) {
                        System.err.println("Exception trying to read mod meta:");
                        exception.printStackTrace();
                    }
                }
            }

            service.onGameStart();

            System.out.printf("Attempting to launch the game with arguments %s%n", String.join(" ", extraArguments));

            String mainClassName = isServer ? SERVER_MAIN_CLASS : MINDUSTRY_MAIN_CLASS;
            Class<?> mainClass = loader.loadClass(mainClassName);
            Method mainMethod = mainClass.getDeclaredMethod("main", String[].class);

            mainMethod.invoke(null, (Object) extraArguments.toArray(new String[0]));

            if(isServer) {
                // HeadlessApplication is non-blocking causing the class loader to close.
                // Does this even need a fix knowing that filesToDelete should be used on client side when mods
                // are being deleted.
                return;
            }

            loader.close();

            deleteDirectory(filesToDelete);

            if(!restartGame) {
                return;
            }

            System.out.println("Mindustry exited for restart, reopening the launcher.");

            boolean windows = System.getProperty("os.name", "").contains("Windows");
            boolean mac = System.getProperty("os.name", "").contains("Mac");

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
            System.err.println("Mindustry has crashed:");
            throwable.printStackTrace();

            boolean needsJavaUpdate = throwable instanceof UnsupportedClassVersionError;

            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

                StringWriter writer = new StringWriter();
                throwable.printStackTrace(new PrintWriter(writer));
                JOptionPane.showMessageDialog(null, writer.toString(), "Mindustry has crashed", JOptionPane.ERROR_MESSAGE);
            } catch(Throwable ignored) {
            }

            File crashReportDirectory = new File(gameDirectory, "crashes");
            if(!crashReportDirectory.exists() && !crashReportDirectory.mkdirs()) {
                System.err.println("Unable to create crash report directory");
            }

            String date = DateTimeFormatter.ofPattern("MM_dd_yyyy_HH_mm_ss").format(LocalDateTime.now());
            File crashReportFile = new File(crashReportDirectory, "modlib-crash-" + date + ".txt");

            try(PrintStream writer = new PrintStream(crashReportFile.getAbsolutePath())) {
                writer.printf("Mindustry has crashed.%n");
                writer.println();
                writer.printf("Version: %s%n", version);
                writer.printf("OS: %s %s (%s)%n", System.getProperty("os.name"), System.getProperty("sun.arch.data.model"), System.getProperty("os.arch"));
                writer.printf("Java: %s%n", System.getProperty("java.version"));
                writer.println();
                throwable.printStackTrace(writer);
            } catch(IOException exception) {
                System.err.println("Unable to create crash report file");
                exception.printStackTrace();
            }
        }
    }

    private static void deleteDirectory(File[] files) {
        if(files != null) {
            deleteDirectory(Arrays.asList(files));
        }
    }

    private static void deleteDirectory(Iterable<File> files) {
        for(File file : files) {
            if(file.isDirectory()) {
                deleteDirectory(file.listFiles());
                if(!file.delete()) {
                    file.deleteOnExit();
                }
                continue;
            }
            if(!file.delete()) {
                file.deleteOnExit();
            }
        }
    }

}
