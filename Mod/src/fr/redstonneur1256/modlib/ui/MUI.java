package fr.redstonneur1256.modlib.ui;

import arc.Core;
import arc.util.Log;
import fr.redstonneur1256.modlib.ModLibProperties;
import fr.redstonneur1256.modlib.launcher.ModLibLauncher;
import fr.redstonneur1256.modlib.ui.settings.LabelSetting;
import mindustry.Vars;
import mindustry.gen.Icon;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;

import static mindustry.Vars.ui;

public class MUI {

    public MUI() {
        Vars.ui.settings.addCategory("@modlib.settings", Icon.terminal, table -> {
            table.pref(new LabelSetting("Version: [royal]" + ModLibProperties.VERSION));
            table.pref(new LabelSetting("Build: [royal]" + ModLibProperties.BUILD));
            table.pref(new LabelSetting("Built: [royal]" + DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").format(LocalDateTime.ofInstant(ModLibProperties.BUILT, Calendar.getInstance().getTimeZone().toZoneId()))));

            table.checkPref("modlib.antialiasing", false, checked -> Vars.ui.settings.hidden(() -> ui.showInfoOnHidden("@modlib.settings.reload", () -> {
                ModLibLauncher.launcher.restartGame = true;
                Core.app.exit();
            })));
            table.checkPref("modlib.debug", false, checked -> Log.level = checked ? Log.LogLevel.debug : Log.LogLevel.info);
        });
    }

}
