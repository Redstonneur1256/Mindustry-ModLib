package fr.redstonneur1256.modlib.util.dns;

public class DnsRecord {

    public final int type;
    public final long ttl;

    public DnsRecord(int type, long ttl) {
        this.type = type;
        this.ttl = ttl;
    }

}
