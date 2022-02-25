package fr.redstonneur1256.examplemod;

import mindustry.mod.Mod;

public class MainMod extends Mod {

    @Override
    public void init() {
        // Uncomment the demo you want to test

        // Send custom packets to the server/client
        // CustomPacketExample.init();

        // Send custom packet and wait for reply, client/server example
        // CustomReplyPacketExample.init();

        // Test if a String is foo using a custom Call implementation
        CustomCallExample.init();
    }

}
