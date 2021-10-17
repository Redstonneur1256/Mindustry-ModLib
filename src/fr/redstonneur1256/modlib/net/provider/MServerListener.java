package fr.redstonneur1256.modlib.net.provider;

import arc.Core;
import arc.net.Connection;
import arc.net.DcReason;
import arc.net.FrameworkMessage;
import arc.net.NetListener;
import arc.util.Log;
import mindustry.Vars;
import mindustry.net.Packets;

public class MServerListener implements NetListener {

    private MProvider provider;

    public MServerListener(MProvider provider) {
        this.provider = provider;
    }

    @Override
    public void connected(Connection connection) {
        String ip = connection.getRemoteAddressTCP().getAddress().getHostAddress();

        MArcConnection arcConnection = new MArcConnection(ip, provider, connection);

        Packets.Connect packet = new Packets.Connect();
        packet.addressTCP = ip;

        Log.debug("&bReceived connection: @", packet.addressTCP);

        provider.connections.add(arcConnection);
        Core.app.post(() -> Vars.net.handleServerReceived(arcConnection, packet));
    }

    @Override
    public void disconnected(Connection connection, DcReason reason) {
        MArcConnection arcConnection = provider.getByArcID(connection.getID());
        if(arcConnection == null) return;

        Packets.Disconnect packet = new Packets.Disconnect();
        packet.reason = reason.toString();

        Core.app.post(() -> {
            Vars.net.handleServerReceived(arcConnection, packet);
            provider.connections.remove(arcConnection);
        });
    }

    @Override
    public void received(Connection connection, Object object) {
        MArcConnection arcConnection;
        if(object instanceof FrameworkMessage || (arcConnection = provider.getByArcID(connection.getID())) == null) {
            return;
        }

        Core.app.post(() -> {
            try {
                Vars.net.handleServerReceived(arcConnection, object);
            }catch(Throwable throwable) {
                throwable.printStackTrace();
            }
        });
    }


}
