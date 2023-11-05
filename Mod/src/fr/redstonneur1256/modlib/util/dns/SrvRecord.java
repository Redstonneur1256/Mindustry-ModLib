package fr.redstonneur1256.modlib.util.dns;

import java.nio.ByteBuffer;

public class SrvRecord extends DnsRecord implements Comparable<SrvRecord> {

    public final int priority;
    public final int weight;
    public final int port;
    public final String target;

    public SrvRecord(long ttl, ByteBuffer buffer) {
        this(ttl, buffer.getShort(), buffer.getShort(), buffer.getShort(), readTarget(buffer));
    }

    public SrvRecord(long ttl, int priority, int weight, int port, String target) {
        super(SimpleDns.TYPE_SRV, ttl);
        this.priority = priority;
        this.weight = weight;
        this.port = port;
        this.target = target;
    }

    @Override
    public int compareTo(SrvRecord o) {
        if (this.priority != o.priority) {
            return Integer.compare(this.priority, o.priority);
        } else {
            return Integer.compare(this.weight, o.weight);
        }
    }

    private static String readTarget(ByteBuffer buffer) {
        byte len;
        StringBuilder builder = new StringBuilder();
        while ((len = buffer.get()) != 0) {
            for (int j = 0; j < len; j++) {
                builder.append((char) buffer.get());
            }
            builder.append('.');
        }
        builder.setLength(builder.length() - 1);
        return builder.toString();
    }

}
