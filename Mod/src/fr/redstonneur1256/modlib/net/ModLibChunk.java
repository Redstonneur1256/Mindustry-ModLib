package fr.redstonneur1256.modlib.net;

import arc.Core;
import arc.Events;
import arc.func.Boolf;
import arc.struct.Seq;
import fr.redstonneur1256.modlib.MVars;
import fr.redstonneur1256.modlib.ModLib;
import fr.redstonneur1256.modlib.events.net.PlayerDataSyncedEvent;
import fr.redstonneur1256.modlib.net.io.MTypeIO;
import fr.redstonneur1256.modlib.net.packet.PacketManager;
import fr.redstonneur1256.modlib.net.packets.DataAckPacket;
import mindustry.Vars;
import mindustry.io.SaveFileReader;
import org.jetbrains.annotations.ApiStatus;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.BitSet;
import java.util.Objects;

@ApiStatus.Internal
public class ModLibChunk implements SaveFileReader.CustomChunk {

    @Override
    public boolean shouldWrite() {
        return Vars.net.server();
    }

    @Override
    public void write(DataOutput stream) throws IOException {
        stream.writeUTF(ModLib.getVersion());

        PacketManager.writeAvailablePackets(stream);
        MVars.net.getCallManager().writeMethods(stream);
        MTypeIO.writeSerializers(stream);
    }

    @Override
    public void read(DataInput stream) throws IOException {
        if(!Vars.net.client()) {
            // Even if shouldWrite is false when the server is not opened, the game state can be saved on the server which
            // would cause this section to be written, ignore it when this case happens
            return;
        }
        String version = stream.readUTF();

        if(!version.equals(ModLib.getVersion()) && Core.settings.getBool("modlib.versionWarning", true)) {
            Vars.ui.showErrorMessage(Core.bundle.format("modlib.version.mismatch", ModLib.getVersion(), version));
        }

        PacketManager.readAvailablePackets(stream);
        MVars.net.getCallManager().readMethods(stream);
        MTypeIO.readSerializers(stream);

        DataAckPacket packet = new DataAckPacket();
        packet.availablePackets = createBitSet(PacketManager.getActivePackets(), Objects::nonNull);
        packet.availableMethods = createBitSet(MVars.net.getCallManager().getActiveMethods(), Objects::nonNull);
        packet.availableCallClasses = MVars.net.getCallManager().getActiveClasses().map(Class::getName);
        Vars.net.send(packet, true);

        Events.fire(new PlayerDataSyncedEvent(null));
    }

    private static <T> BitSet createBitSet(Seq<T> seq, Boolf<T> predicate) {
        BitSet set = new BitSet();

        for(int i = 0; i < seq.size; i++) {
            if(predicate.get(seq.get(i))) {
                set.set(i);
            }
        }

        return set;
    }

}
