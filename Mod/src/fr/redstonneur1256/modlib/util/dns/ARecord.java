package fr.redstonneur1256.modlib.util.dns;

import java.nio.ByteBuffer;

public class ARecord extends DnsRecord {

    public final byte[] address;

    public ARecord(long ttl, ByteBuffer buffer) {
        this(ttl, new byte[4]);
        buffer.get(address);
    }

    public ARecord(long ttl, byte[] address) {
        super(SimpleDns.TYPE_A, ttl);
        this.address = address;
    }

    public String getAddressAsString() {
        return (address[0] & 0xFF) + "." + (address[1] & 0xFF) + "." + (address[2] & 0xFF) + "." + (address[3] & 0xFF);
    }

    @Override
    public String toString() {
        return "ARecord{" + getAddressAsString() + "}";
    }

}
