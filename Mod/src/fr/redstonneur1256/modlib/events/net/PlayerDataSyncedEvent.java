package fr.redstonneur1256.modlib.events.net;

import mindustry.gen.Player;

public class PlayerDataSyncedEvent {

    public final Player player;

    public PlayerDataSyncedEvent(Player player) {
        this.player = player;
    }

}
