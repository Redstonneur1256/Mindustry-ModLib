package fr.redstonneur1256.examplemod.packet.direct;

import arc.Core;
import arc.Events;
import arc.input.KeyCode;
import arc.util.Log;
import fr.redstonneur1256.modlib.events.net.PlayerDataSyncedEvent;
import fr.redstonneur1256.modlib.net.PacketManager;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.net.Net;

public class CustomPacketExample {

    public static void init() {
        // Register a custom packet, for this example a simple packet containing a String
        PacketManager.registerPacket(MessagePacket.class, MessagePacket::new);

        /*
         * You can either unregister packet handlers by doing
         * 1:
         *  - MVars.net.unregisterClient(Class, Cons)
         *  - MVars.net.unregisterServer(Class, Cons2)
         * 2:
         *  By registering your listener with
         *   - MVars.net.registerClient(Class, Cons)
         *   - MVars.net.registerServer(Class, Cons2)
         *  instead of Vars.net.handleClient/Server
         *  and then calling unregister()
         *
         * Note that you can also register multiple listeners per packet type
         */

        // Handle that packet on the server side, here its just printing the packet content and the sender name
        Vars.net.handleServer(MessagePacket.class,
                (connection, packet) -> Log.info("Received custom message '@' from @", packet.message, connection.player.name));

        // Handle the packet on the client side, here it's announcing the message
        Vars.net.handleClient(MessagePacket.class,
                (packet) -> Vars.ui.announce("Received custom message '" + packet.message + "'"));


        // When a player successfully synced his packets:
        Events.on(PlayerDataSyncedEvent.class, event -> {

            // This is called on the client side with null player
            if(event.player == null) {
                return;
            }

            // Check if the player can handle that packet, you should always check before sending a packet
            // to avoid kicking the player if he is a vanilla player, note that even if the player is having
            // the mod library loaded he might not have the mod providing that packet
            if(PacketManager.isAvailableServer(event.player.con, MessagePacket.class)) {

                // Send a message packet with the text "Hello <PlayerName>" to the player
                MessagePacket packet = new MessagePacket();
                packet.message = "Welcome " + event.player.name + "[]";
                event.player.con.send(packet, Net.SendMode.tcp);

            }
        });

        // For the client side, every game tick:
        Events.run(EventType.Trigger.update, () -> {

            // Check if the key J has been tapped, and we are connected to a server
            if(Core.input.keyTap(KeyCode.j) && Vars.net.client()) {

                // Check if the server we are currently connected on support this packet
                // If you send the packet anyway it should not be sent internally but heh, still better to check
                if(PacketManager.isAvailableClient(MessagePacket.class)) {

                    // Send a simple packet to the server saying hello
                    MessagePacket packet = new MessagePacket();
                    packet.message = "Hello server";
                    Vars.net.send(packet, Net.SendMode.tcp);
                }else {
                    // If the packet isn't available to the server
                    Vars.ui.announce("[red]The server doesn't have the MessagePacket");
                }

            }
        });
    }

}
