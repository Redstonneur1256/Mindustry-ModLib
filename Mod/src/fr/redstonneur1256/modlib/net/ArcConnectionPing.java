package fr.redstonneur1256.modlib.net;

import arc.net.Connection;
import arc.net.DcReason;
import arc.net.NetListener;
import fr.redstonneur1256.modlib.MVars;
import org.jetbrains.annotations.ApiStatus;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@ApiStatus.Internal
public class ArcConnectionPing implements NetListener {

    private Connection connection;
    private Future<?> pingTask;

    public ArcConnectionPing(Connection connection) {
        this.connection = connection;
        this.pingTask = MVars.net.getScheduler().scheduleAtFixedRate(connection::updateReturnTripTime, 0, 2, TimeUnit.SECONDS);
    }

    @Override
    public void disconnected(Connection connection, DcReason reason) {
        pingTask.cancel(false);
    }

    public Connection getConnection() {
        return connection;
    }

}
