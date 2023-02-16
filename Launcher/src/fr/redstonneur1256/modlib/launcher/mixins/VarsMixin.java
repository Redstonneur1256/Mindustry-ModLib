package fr.redstonneur1256.modlib.launcher.mixins;

import arc.Events;
import arc.struct.Seq;
import arc.struct.StringMap;
import arc.util.Log;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.game.EventType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Mixin(Vars.class)
public class VarsMixin {

    @Shadow
    public static boolean loadedLogger;

    @Shadow
    public static boolean headless;

    /**
     * @author Redstonneur1256
     * @reason fully replace the built-in logger by our custom one
     */
    @Overwrite
    public static void loadLogger() {
        if(loadedLogger) return;

        StringMap simpleClassNames = new StringMap();
        Seq<String> hiddenClasses = Seq.with("arc.util.Log");

        String[] levels = { "DEBUG", "INFO", "WARN", "ERROR", "NONE" };
        String[] colors = { "royal", "green", "yellow", "scarlet", "gray" };

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss");

        Seq<String> logBuffer = new Seq<>();
        Log.logger = (level, text) -> {
            synchronized(logBuffer) {
                StackTraceElement[] elements = Thread.currentThread().getStackTrace();
                String className;
                int index = 5;
                do {
                    className = elements[index++].getClassName();
                } while(hiddenClasses.contains(className));
                String name = className;
                String caller = Log.level == Log.LogLevel.debug ? name : simpleClassNames.get(name, () -> name.substring(name.lastIndexOf('.') + 1));

                int ordinal = level.ordinal();
                String levelName = levels[ordinal];

                String console = Strings.format("[@][@/@][]: @", colors[ordinal], levelName, caller, text);

                System.out.printf("[%s] [%s/%s]: %s%n", formatter.format(LocalDateTime.now()), levelName, caller, text);

                if(!headless) {
                    if(Vars.ui == null || Vars.ui.consolefrag == null) {
                        logBuffer.add(console);
                    } else {
                        Vars.ui.consolefrag.addMessage(console);
                    }
                }
            }
        };

        Events.run(EventType.ClientLoadEvent.class, () -> {
            logBuffer.each(Vars.ui.consolefrag::addMessage);
            logBuffer.clear();
        });

        loadedLogger = true;
    }

}
