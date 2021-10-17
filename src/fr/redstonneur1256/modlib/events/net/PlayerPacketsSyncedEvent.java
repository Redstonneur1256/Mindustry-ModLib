package fr.redstonneur1256.modlib.events.net;

import mindustry.gen.Player;

/**
 * Event fired when a player packets got synced with the server
 * Also fired when the game client packets got synced with {@link mindustry.Vars#player} as player
 */
public class PlayerPacketsSyncedEvent {

    public final Player player;

    public PlayerPacketsSyncedEvent(Player player) {
        this.player = player;
    }

}
