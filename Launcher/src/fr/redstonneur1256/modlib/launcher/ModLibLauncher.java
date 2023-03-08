package fr.redstonneur1256.modlib.launcher;

import fr.redstonneur1256.modlib.ModLibProperties;
import fr.redstonneur1256.modlib.launcher.file.DirectFileProvider;
import fr.redstonneur1256.modlib.launcher.file.FileProvider;
import fr.redstonneur1256.modlib.launcher.file.ZipFileProvider;
import fr.redstonneur1256.modlib.launcher.mixin.MixinClassLoader;
import fr.redstonneur1256.modlib.launcher.mixin.ModLibMixinService;
import fr.redstonneur1256.modlib.launcher.util.ArcSettings;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.hjson.ParseException;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.service.MixinService;

import javax.swing.*;
import java.io.*;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class ModLibLauncher {

    private static final String[] META_FILES = { "mod.json", "mod.hjson", "plugin.json", "plugin.hjson" };
    private static final String SERVER_MAIN_CLASS = "mindustry.server.ServerLauncher";
    private static final String MINDUSTRY_MAIN_CLASS = "mindustry.desktop.DesktopLauncher";

    public static ModLibLauncher launcher;

    public static void main(String[] args) {
        if(args.length < 4) {
            System.exit(1);
        }

        File mindustryExecutable = new File(args[0]);
        File gameDirectory = new File(args[1]);
        boolean isServer = Boolean.parseBoolean(args[2]);
        String version = args[3];

        try {
            List<String> extraArguments = new ArrayList<>(Arrays.asList(Arrays.copyOfRange(args, 4, args.length)));

            launcher = new ModLibLauncher(args, mindustryExecutable, gameDirectory, isServer, extraArguments);
            launcher.openGame();
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

    private final String[] rawArgs;
    private final File mindustryExecutable;
    private final File gameDirectory;
    public final boolean server;
    private final List<String> extraArguments;
    private final File modsDirectory;
    public final ArcSettings settings;
    private ModLibMixinService service;
    public MixinClassLoader loader;
    /**
     * Should the game automatically be restarted once it's closed
     * Does not apply for servers
     */
    public boolean restartGame = false;
    /**
     * Should we directly restart the launcher or the full game
     */
    public boolean fastRestart = true;

    private ModLibLauncher(String[] rawArgs, File mindustryExecutable, File gameDirectory, boolean server, List<String> extraArguments) {
        this.rawArgs = rawArgs;
        this.mindustryExecutable = mindustryExecutable;
        this.gameDirectory = gameDirectory;
        this.server = server;
        this.extraArguments = extraArguments;
        this.modsDirectory = new File(gameDirectory, "mods");
        this.settings = new ArcSettings();
    }

    private void openGame() throws Throwable {
        System.out.println("  __  __           _ _      _ _     \n" +
                " |  \\/  |         | | |    (_) |    \n" +
                " | \\  / | ___   __| | |     _| |__  \n" +
                " | |\\/| |/ _ \\ / _` | |    | | '_ \\ \n" +
                " | |  | | (_) | (_| | |____| | |_) |\n" +
                " |_|  |_|\\___/ \\__,_|______|_|_.__/");
        System.out.printf("Launcher version %s (build %s), %s days old%n", ModLibProperties.VERSION,
                ModLibProperties.BUILD, ChronoUnit.DAYS.between(ModLibProperties.BUILT, Instant.now()));

        loadSettings();
        initMixins();

        Mixins.addConfiguration("launcher.mixins.json");
        loadModMixins();

        service.onGameStart();

        System.out.printf("Attempting to launch the game with arguments %s%n", String.join(" ", extraArguments));

        String mainClassName = server ? SERVER_MAIN_CLASS : MINDUSTRY_MAIN_CLASS;
        Class<?> mainClass = loader.loadClass(mainClassName);
        Method mainMethod = mainClass.getDeclaredMethod("main", String[].class);

        mainMethod.invoke(null, (Object) extraArguments.toArray(new String[0]));

        if(server) {
            // HeadlessApplication is non-blocking causing the class loader to close.
            return;
        }

        loader.close();

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
            command.addAll(Arrays.asList(rawArgs));
        } else {
            command.add(mindustryExecutable.getAbsolutePath());
        }

        Runtime.getRuntime().exec(command.toArray(new String[0]));
    }

    private void loadSettings() throws IOException {
        System.out.printf("(Re)loading game settings%n");

        File settingsFile = new File(gameDirectory, "settings.bin");
        if(settingsFile.exists()) {
            settings.load(settingsFile);
        }
    }

    private void initMixins() throws MalformedURLException {
        System.out.printf("Bootstrapping mixins%n");

        MixinBootstrap.init();
        service = (ModLibMixinService) MixinService.getService();
        MixinEnvironment.getCurrentEnvironment().setSide(server ? MixinEnvironment.Side.SERVER : MixinEnvironment.Side.CLIENT);

        loader = new MixinClassLoader(new URL[] { mindustryExecutable.toURI().toURL() }, ModLibLauncher.class.getClassLoader());
    }

    private void loadModMixins() {
        // Add mods to classloader and load mixins
        File[] modFiles = modsDirectory.listFiles();
        if(modFiles == null) {
            return;
        }
        for(File file : modFiles) {
            try {
                loader.addURL(file.toURI().toURL());

                if(file.isDirectory()) {
                    loadMod(file.getName(), new DirectFileProvider(file));
                    continue;
                }

                try(ZipFile zip = new ZipFile(file)) {
                    loadMod(file.getName(), new ZipFileProvider(zip));
                } catch(ZipException exception) {
                    System.err.printf("Potentially corrupted mod file \"%s\"%n", file);
                    exception.printStackTrace();
                }
            } catch(IOException | ParseException exception) {
                System.err.println("Exception trying to read mod meta:");
                exception.printStackTrace();
            }
        }
    }

    private void loadMod(String pathName, FileProvider provider) throws IOException, ParseException {
        Optional<String> optional = Arrays.stream(META_FILES).filter(provider::exists).findFirst();
        if(!optional.isPresent()) {
            System.err.printf("Could not find a meta file for mod file \"%s\"%n", pathName);
            return;
        }

        try(Reader reader = new InputStreamReader(provider.getStream(optional.get()))) {
            JsonObject metadata = JsonValue.readHjson(reader).asObject();

            JsonValue gameArguments = metadata.get("gameArguments");
            if(gameArguments != null && gameArguments.isArray()) {
                extraArguments.addAll(gameArguments.asArray()
                        .values()
                        .stream()
                        .map(JsonValue::asString)
                        .collect(Collectors.toList()));
            }

            String name = metadata.get("name").asString();

            boolean modEnabled = settings.get(Boolean.class, "mod-" + name + "-enabled", true);
            String mixinsPath = name + ".mixins.json";
            if(modEnabled && provider.exists(mixinsPath)) {
                Mixins.addConfiguration(mixinsPath);
            }
        }
    }

    public boolean debug() {
        return Arrays.asList(rawArgs).contains("-debug");
    }

}
