package fr.redstonneur1256.modlib.net;

import arc.Core;
import arc.func.Cons;
import arc.util.Time;
import fr.redstonneur1256.modlib.net.udp.UdpConnectionManager;
import fr.redstonneur1256.modlib.util.dns.ARecord;
import fr.redstonneur1256.modlib.util.dns.SimpleDns;
import fr.redstonneur1256.modlib.util.dns.SrvRecord;
import mindustry.net.Host;
import mindustry.net.NetworkIO;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

public class ServerPing {

    private static final ByteBuffer PING_HEADER = ByteBuffer.wrap(new byte[] { -2, 1 });
    private static final Pattern IP_PATTERN = Pattern.compile("^(\\b25[0-5]|\\b2[0-4][0-9]|\\b[01]?[0-9][0-9]?)(\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)){3}$");

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
            SrvRecord record = records.min(SrvRecord::compareTo); // Do not sort the whole list, just get the best one
            pingInternal(record.target, record.port, callback, () -> Core.app.post(() -> failure.get(new TimeoutException())));
        }));
    }

    private void pingInternal(String address, int port, Cons<Host> callback, Runnable failure) {
        long start = Time.millis();

        if(!IP_PATTERN.matcher(address).matches()) {
            dns.resolveA(address, records -> {
                if(records.isEmpty()) {
                    failure.run();
                    return;
                }
                ARecord record = records.get(0);
                pingInternal(record.getAddressAsString(), port, callback, failure);
            });
            return;
        }

        // Safe to create, no resolution since it has to be an IP
        InetSocketAddress socketAddress = new InetSocketAddress(address, port);

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
