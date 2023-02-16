package fr.redstonneur1256.modlib.net.packet;

import fr.redstonneur1256.modlib.net.packets.DataAckPacket;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface MPlayerConnection {

    void handleSyncPacket(DataAckPacket packet);

}
