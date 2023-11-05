package fr.redstonneur1256.examplemod.net.packet.direct;

import arc.Core;
import arc.Events;
import arc.util.Log;
import fr.redstonneur1256.examplemod.ExampleKeyBinds;
import fr.redstonneur1256.modlib.MVars;
import fr.redstonneur1256.modlib.events.net.PlayerDataSyncedEvent;
import fr.redstonneur1256.modlib.net.packet.MConnection;
import fr.redstonneur1256.modlib.net.packet.PacketManager;
import mindustry.Vars;
import mindustry.game.EventType;

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
        Vars.net.handleClient(MessagePacket.class, (packet) -> Vars.ui.announce("Received custom message '" + packet.message + "'"));


        // When a player using ModLib got the available content synced:
        Events.on(PlayerDataSyncedEvent.class, event -> {

            // When the data is synced on the client side the event is fired with no player, on this example
            // we only want to send the packet from the server side
            if (event.player == null) {
                return;
            }

            // Check if the player can handle that packet, you should always check before sending a packet
            // or else one of the following scenarios will happen
            // Player doesn't have ModLib: game closes the connection due to the unknown packet id
            // Player has ModLib but not the packet: it's discarded silently
            // Player has ModLib and the packet: it's handled normally
            if (((MConnection) event.player.con).supportsPacket(MessagePacket.class)) {

                // Send a message packet with the text "Hello <PlayerName>" to the player
                MessagePacket packet = new MessagePacket();
                packet.message = "[white]Welcome " + event.player.name + "[white]!";
                event.player.con.send(packet, true);

            }
        });

        // For the client side, every game tick:
        Events.run(EventType.Trigger.update, () -> {

            // Check if the key J has been tapped, and we are connected to a server
            if (Core.input.keyTap(ExampleKeyBinds.demo) && Vars.net.client()) {

                // Check if the server we are currently connected on support this packet
                // If you try to send the packet on a server where it's not supported it will be
                // silently discarded to avoid client being disconnected
                if (MVars.net.supportsPacket(MessagePacket.class)) {

                    // Send a simple packet to the server saying hello
                    MessagePacket packet = new MessagePacket();
                    packet.message = "Hello server";
                    Vars.net.send(packet, true);
                } else {
                    // If the packet isn't available to the server
                    Vars.ui.announce("[red]The server doesn't have the MessagePacket");
                }
            }
        });
    }

}
