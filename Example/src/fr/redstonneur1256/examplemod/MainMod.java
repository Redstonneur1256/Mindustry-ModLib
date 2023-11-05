package fr.redstonneur1256.examplemod;

import arc.util.CommandHandler;
import arc.util.Log;
import fr.redstonneur1256.examplemod.net.CustomCallExample;
import fr.redstonneur1256.modlib.net.NetworkDebuggable;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.mod.Mod;

public class MainMod extends Mod {

    @Override
    public void init() {
        // Register custom keybindings (needed by all the examples, do not comment)
        ExampleKeyBinds.register();

        // Uncomment the demo you want to test

        // Send custom packets to the server/client
        // CustomPacketExample.init();

        // Send custom packet and wait for reply, client/server example
        // CustomReplyPacketExample.init();

        // Test if a String is foo using a custom Call implementation
        CustomCallExample.init();
    }

    @Override
    public void registerServerCommands(CommandHandler handler) {
        handler.register("ping", "Display the ping of everyone on the server", args -> {
            Log.info("Ping of players:");
            for (Player player : Groups.player) {
                long ping = ((NetworkDebuggable) player).getPing();
                Log.info("- @: @ ms", player.name, ping);
            }
        });
    }

}
