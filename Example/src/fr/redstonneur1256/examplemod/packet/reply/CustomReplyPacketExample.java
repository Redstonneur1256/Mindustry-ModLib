package fr.redstonneur1256.examplemod.packet.reply;

import arc.Core;
import arc.Events;
import arc.input.KeyCode;
import arc.util.Log;
import fr.redstonneur1256.modlib.MVars;
import fr.redstonneur1256.modlib.net.PacketManager;
import fr.redstonneur1256.modlib.net.provider.MArcConnection;
import mindustry.Vars;
import mindustry.game.EventType;

/**
 * See {@link fr.redstonneur1256.examplemod.packet.direct.CustomPacketExample} for more information about packet handling
 */
public class CustomReplyPacketExample {

    public static void init() {
        // Register the custom packet
        PacketManager.registerPacket(ReplyMessagePacket.class, ReplyMessagePacket::new);

        // Handle the packet on server side
        MVars.net.handleServer(ReplyMessagePacket.class, (conn, packet) -> {
            // Cast to MArcConnection to get new methods, the connection will always be MArcConnection unless the library fails to load
            MArcConnection connection = (MArcConnection) conn;

            // If the message is Hello from Client then send a reply
            if(packet.message.equals("Hello from Client")) {
                // Reply to the packet with a new message saying Hello from server and expect a reply of message packet
                // and wait for maximum 500 milliseconds
                connection.sendReply(packet, new ReplyMessagePacket("Hello from server"), ReplyMessagePacket.class,
                        reply -> Log.info("Received last message '@' from @", reply.message, connection.player.name),
                        () -> Log.info("Didn't received last message in 500 milliseconds"), 500);
            }
        });

        // For the client side, every game tick:
        Events.run(EventType.Trigger.update, () -> {

            // Check if the key J has been tapped, and we are connected to a server
            if(Core.input.keyTap(KeyCode.j) && Vars.net.client()) {

                // Check if the server we are currently connected on support this packet
                // If you send the packet anyway it should not be sent internally but heh, still better to check
                if(PacketManager.isAvailableClient(ReplyMessagePacket.class)) {

                    MVars.net.sendPacket(new ReplyMessagePacket("Hello from Client"), ReplyMessagePacket.class, reply -> {
                        assert reply.message.equals("Hello from server");

                        MVars.net.sendReply(reply, new ReplyMessagePacket("Last message"));
                    });

                }else {
                    // If the packet isn't available to the server
                    Vars.ui.announce("[red]The server doesn't have the MessagePacket");
                }
            }
        });
    }

}
