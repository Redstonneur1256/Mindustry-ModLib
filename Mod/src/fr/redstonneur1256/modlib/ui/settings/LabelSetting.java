package fr.redstonneur1256.modlib.ui.settings;

import mindustry.ui.dialogs.SettingsMenuDialog;

public class LabelSetting extends SettingsMenuDialog.SettingsTable.Setting {

    public LabelSetting(String name) {
        super(name);
    }

    @Override
    public void add(SettingsMenuDialog.SettingsTable table) {
        table.add(name).left().padTop(3f).row();
    }

}
