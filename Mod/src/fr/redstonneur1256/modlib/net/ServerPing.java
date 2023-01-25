package fr.redstonneur1256.modlib.net;

import arc.Core;
import arc.func.Cons;
import arc.net.dns.SRVRecord;
import arc.util.Time;
import fr.redstonneur1256.modlib.net.udp.UdpConnectionManager;
import fr.redstonneur1256.modlib.util.dns.SimpleDns;
import mindustry.net.Host;
import mindustry.net.NetworkIO;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeoutException;

public class ServerPing {

    private static final ByteBuffer PING_HEADER = ByteBuffer.wrap(new byte[] { -2, 1 });

    private UdpConnectionManager connectionManager;
    private SimpleDns dns;
    private ByteBuffer buffer;

    public ServerPing(UdpConnectionManager connectionManager, SimpleDns dns) {
        this.connectionManager = connectionManager;
        this.dns = dns;
        this.buffer = ByteBuffer.allocate(512);
    }

    public void ping(String address, int port, Cons<Host> callback, Cons<Exception> failure) {
        pingInternal(address, port, callback, () -> dns.resolveSRV("_mindustry._tcp." + address, records -> {
            if(records.isEmpty()) {
                Core.app.post(() -> failure.get(new TimeoutException()));
                return;
            }
            SRVRecord record = records.get(0);
            pingInternal(record.target, record.port, callback, () -> Core.app.post(() -> failure.get(new TimeoutException())));
        }));
    }

    private void pingInternal(String address, int port, Cons<Host> callback, Runnable failure) {
        long start = Time.millis();

        InetSocketAddress socketAddress = new InetSocketAddress(address, port);
        if(socketAddress.isUnresolved()) {
            failure.run();
            return;
        }

        connectionManager.connect(socketAddress, 2000, buffer, connection -> {
            connection.setTimeoutHandler(failure);
            connection.setPacketHandler(buffer -> {
                connection.close();

                int ping = (int) Time.timeSinceMillis(start);

                Host host = NetworkIO.readServerData(ping, address, buffer);
                host.port = port;
                Core.app.post(() -> callback.get(host));
            });
            PING_HEADER.position(0);
            connection.send(PING_HEADER);
        });
    }

}
