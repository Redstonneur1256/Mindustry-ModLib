package fr.redstonneur1256.modlib.util.dns;

import arc.func.Cons;
import arc.math.Rand;
import arc.net.dns.ArcDns;
import arc.net.dns.SRVRecord;
import arc.struct.Seq;
import arc.util.Log;
import fr.redstonneur1256.modlib.net.udp.UdpConnectionManager;
import fr.redstonneur1256.modlib.util.NetworkUtil;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Non-blocking DNS Resolution utility class like {@link ArcDns}
 */
public class SimpleDns {

    private UdpConnectionManager manager;
    private Rand random;
    /**
     * ByteBuffer used for all the operations, it is safe to reuse it for all operations as they are all done on the
     * connection manager thread
     */
    private ByteBuffer buffer;

    public SimpleDns(UdpConnectionManager manager) {
        this.manager = manager;
        this.random = new Rand();
        this.buffer = ByteBuffer.allocate(512);
    }

    public void resolveSRV(String domain, Cons<Seq<SRVRecord>> callback) {
        resolveSRV(ArcDns.getNameservers(), 0, domain, callback);
    }

    private void resolveSRV(Seq<InetSocketAddress> servers, int serverIndex, String domain, Cons<Seq<SRVRecord>> callback) {
        if(serverIndex >= servers.size) {
            return;
        }
        manager.connect(servers.get(serverIndex), 2000, buffer, connection -> {
            connection.setTimeoutHandler(() -> resolveSRV(servers, serverIndex + 1, domain, callback));

            short id = (short) random.nextInt(Short.MAX_VALUE);

            connection.setPacketHandler(buffer -> {
                connection.close();

                short responseId = buffer.getShort();
                if(responseId != id) {
                    resolveSRV(servers, serverIndex + 1, domain, callback);
                    Log.warn("Invalid response from DNS server @", servers.get(serverIndex));
                    return;
                }

                buffer.getShort();
                buffer.getShort();
                int answers = buffer.getShort() & 0xFFFF;
                buffer.getShort();
                buffer.getShort();

                byte len;
                while((len = buffer.get()) != 0) {
                    buffer.position(buffer.position() + len);
                }

                buffer.getShort();
                buffer.getShort();

                Seq<SRVRecord> records = new Seq<>(answers);

                for(int i = 0; i < answers; i++) {
                    buffer.getShort();                         // OFFSET
                    buffer.getShort();                         // Type
                    buffer.getShort();                         // Class
                    long ttl = buffer.getInt() & 0xFFFFFFFFL;  // TTL
                    buffer.getShort();                         // Data length

                    int priority = buffer.getShort();
                    int weight = buffer.getShort();
                    int port = buffer.getShort();

                    StringBuilder builder = new StringBuilder();
                    while((len = buffer.get()) != 0) {
                        for(int j = 0; j < len; j++) {
                            builder.append((char) buffer.get());
                        }
                        builder.append('.');
                    }
                    builder.delete(builder.length() - 1, builder.length());

                    records.add(new SRVRecord(ttl, priority, weight, port, builder.toString()));
                }

                callback.get(records.sort());
            });

            NetworkUtil.clear(buffer);
            buffer.putShort(id);             // Id
            buffer.putShort((short) 0x0100); // Flags (recursion enabled)
            buffer.putShort((short) 1);      // Questions
            buffer.putShort((short) 0);      // Answers
            buffer.putShort((short) 0);      // Authority
            buffer.putShort((short) 0);      // Additional

            // Domain
            for(String part : domain.split("\\.")) {
                buffer.put((byte) part.length());
                buffer.put(part.getBytes(StandardCharsets.UTF_8));
            }
            buffer.put((byte) 0);

            buffer.putShort((short) 33);     // Type (SRV)
            buffer.putShort((short) 1);      // Class (Internet)

            buffer.flip();
            connection.send(buffer);
        });
    }

}
