package fr.redstonneur1256.modlib.util.dns;

import arc.func.Cons;
import arc.math.Rand;
import arc.net.dns.ArcDns;
import arc.struct.IntMap;
import arc.struct.Seq;
import arc.util.Log;
import fr.redstonneur1256.modlib.net.udp.UdpConnectionManager;
import fr.redstonneur1256.modlib.util.NetworkUtil;
import fr.redstonneur1256.modlib.util.VariableCache;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Non-blocking DNS Resolution utility class like {@link ArcDns}
 */
public class SimpleDns {

    public static final int TYPE_A = 1;
    public static final int TYPE_SRV = 33;

    private UdpConnectionManager manager;
    private Rand random;
    /**
     * ByteBuffer used for all the operations, it is safe to reuse it for all operations as they are all done on the
     * connection manager thread
     */
    private ByteBuffer buffer;
    private IntMap<VariableCache<String, Seq<DnsRecord>>> caches;
    private Seq<InetSocketAddress> nameServers;

    public SimpleDns(UdpConnectionManager manager) {
        this.manager = manager;
        this.random = new Rand();
        this.buffer = ByteBuffer.allocate(512);
        this.caches = new IntMap<>();
        this.nameServers = ArcDns.getNameservers(); // cache nameservers because arc makes a copy of the list every time

        Log.debug("Using nameservers @", nameServers);
    }

    public void resolveA(String domain, Cons<Seq<ARecord>> callback) {
        resolve(TYPE_A, domain, ARecord::new, callback);
    }

    public void resolveSRV(String domain, Cons<Seq<SrvRecord>> callback) {
        resolve(TYPE_SRV, domain, SrvRecord::new, callback);
    }

    @SuppressWarnings("unchecked")
    private <R extends DnsRecord> void resolve(int type, String domain, RecordReader<R> reader, Cons<Seq<R>> callback) {
        manager.submit(() -> {
            VariableCache<String, Seq<DnsRecord>> cache = caches.get(type, VariableCache::new);
            Seq<DnsRecord> cachedValues = cache.get(domain);
            if (cachedValues != null) {
                callback.get((Seq<R>) cachedValues);
                return;
            }

            resolveInternal(nameServers, 0, type, domain, reader, records -> {
                if (records.any()) {
                    // little race condition but not very important, checking for another current query of the same type
                    // on the same domain and adding the callback would add much more complexity than it's worth.
                    // It would also be very rare that the same domain would be looked up twice at the same time
                    cache.put(domain, (Seq<DnsRecord>) records, Duration.ofSeconds(records.min(record -> (float) record.ttl).ttl));
                }
                callback.get(records);
            });
        });
    }

    private <R extends DnsRecord> void resolveInternal(Seq<InetSocketAddress> servers, int serverIndex, int type, String domain,
                                                       RecordReader<R> reader, Cons<Seq<R>> callback) {
        if (serverIndex >= servers.size) {
            return;
        }
        Runnable failureHandler = () -> resolveInternal(servers, serverIndex + 1, type, domain, reader, callback);

        manager.connect(servers.get(serverIndex), 2000, buffer, connection -> {
            connection.setTimeoutHandler(failureHandler);

            short id = (short) random.nextInt(Short.MAX_VALUE);

            connection.setPacketHandler(buffer -> {
                connection.close();

                short responseId = buffer.getShort();
                if (responseId != id) {
                    resolveInternal(servers, serverIndex + 1, type, domain, reader, callback);
                    Log.warn("Invalid response from DNS server @", servers.get(serverIndex));
                    return;
                }

                buffer.getShort();
                buffer.getShort();
                int answers = buffer.getShort() & 0xFFFF;
                buffer.getShort();
                buffer.getShort();

                byte len;
                while ((len = buffer.get()) != 0) {
                    buffer.position(buffer.position() + len);
                }

                buffer.getShort();
                buffer.getShort();

                Seq<R> records = new Seq<>(answers);

                for (int i = 0; i < answers; i++) {
                    buffer.getShort();                           // OFFSET
                    int answerType = buffer.getShort() & 0xFFFF; // Type
                    buffer.getShort();                           // Class
                    long ttl = buffer.getInt() & 0xFFFFFFFFL;    // TTL
                    int length = buffer.getShort() & 0xFFFF;     // Data length

                    // Optionally CNAME results will be returned with the A results, skip those
                    if (answerType != type) {
                        buffer.position(buffer.position() + length);
                        continue;
                    }

                    int position = buffer.position();

                    records.add(reader.read(ttl, buffer));

                    buffer.position(position + length);
                }

                callback.get(records);
            });

            NetworkUtil.clear(buffer);
            buffer.putShort(id);             // Id
            buffer.putShort((short) 0x0100); // Flags (recursion enabled)
            buffer.putShort((short) 1);      // Questions
            buffer.putShort((short) 0);      // Answers
            buffer.putShort((short) 0);      // Authority
            buffer.putShort((short) 0);      // Additional

            // Domain
            for (String part : domain.split("\\.")) {
                buffer.put((byte) part.length());
                buffer.put(part.getBytes(StandardCharsets.UTF_8));
            }
            buffer.put((byte) 0);

            buffer.putShort((short) type);   // Type
            buffer.putShort((short) 1);      // Class (Internet)

            buffer.flip();
            connection.send(buffer);
        }, exception -> failureHandler.run());
    }

    public interface RecordReader<R extends DnsRecord> {

        R read(long ttl, ByteBuffer buffer);

    }

}
