package fr.redstonneur1256.modlib.net.packet;

import fr.redstonneur1256.modlib.net.packets.DataAckPacket;

public interface MPlayerConnection {

    void handleSyncPacket(DataAckPacket packet);

}
