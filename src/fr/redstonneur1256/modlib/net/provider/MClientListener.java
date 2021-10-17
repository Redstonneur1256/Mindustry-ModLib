package fr.redstonneur1256.modlib.net.provider;

import arc.Core;
import arc.net.Connection;
import arc.net.DcReason;
import arc.net.FrameworkMessage;
import arc.net.NetListener;
import mindustry.Vars;
import mindustry.net.Packets;

public class MClientListener implements NetListener {

    @Override
    public void connected(Connection connection) {
        Packets.Connect packet = new Packets.Connect();
        packet.addressTCP = connection.getRemoteAddressTCP().getAddress().getHostAddress();
        if(connection.getRemoteAddressTCP() != null) {
            packet.addressTCP = connection.getRemoteAddressTCP().toString();
        }

        Core.app.post(() -> Vars.net.handleClientReceived(packet));
    }

    @Override
    public void disconnected(Connection connection, DcReason reason) {
        if(connection.getLastProtocolError() != null) {
            Vars.netClient.setQuiet();
        }
        Packets.Disconnect packet = new Packets.Disconnect();
        packet.reason = reason.toString();
        Core.app.post(() -> Vars.net.handleClientReceived(packet));
    }

    @Override
    public void received(Connection connection, Object object) {
        if(object instanceof FrameworkMessage) {
            return;
        }

        Core.app.post(() -> {
            try {
                Vars.net.handleClientReceived(object);
            }catch(Throwable throwable) {
                Vars.net.handleException(throwable);
            }
        });
    }

}
