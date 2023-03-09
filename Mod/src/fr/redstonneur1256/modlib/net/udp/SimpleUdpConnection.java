package fr.redstonneur1256.modlib.net.udp;

import arc.func.Cons;
import arc.util.io.Streams;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;

public class SimpleUdpConnection {

    private UdpConnectionManager manager;
    private InetSocketAddress address;
    private DatagramChannel channel;
    private SelectionKey key;
    private ByteBuffer receiveBuffer;
    private long idleTimeout;
    private long lastPacketTime;
    private Runnable timeoutHandler;
    private Cons<ByteBuffer> packetHandler;
    private boolean closed;

    public SimpleUdpConnection(UdpConnectionManager manager, InetSocketAddress address, DatagramChannel channel, SelectionKey key,
                               ByteBuffer receiveBuffer, long idleTimeout) {
        this.manager = manager;
        this.address = address;
        this.channel = channel;
        this.key = key;
        this.receiveBuffer = receiveBuffer;
        this.idleTimeout = idleTimeout;
        this.lastPacketTime = System.currentTimeMillis();
    }

    public void send(ByteBuffer buffer) {
        checkClosed();

        lastPacketTime = System.currentTimeMillis();
        manager.reorder(this);

        try {
            channel.send(buffer, address);
        } catch(IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void close() {
        close(false);
    }

    protected void close(boolean timeout) {
        checkClosed();
        closed = true;

        manager.submit(() -> {
            key.cancel();
            Streams.close(channel);
            manager.removeConnection(this);

            if(timeout && timeoutHandler != null) {
                timeoutHandler.run();
            }
        });
    }

    protected void handle(ByteBuffer buffer) {
        lastPacketTime = System.currentTimeMillis();
        manager.reorder(this);

        if(packetHandler != null) {
            packetHandler.get(buffer);
        }
    }

    private void checkClosed() {
        if(closed) {
            throw new IllegalStateException("This connection is closed");
        }
    }

    public long getRemainingTime() {
        return (lastPacketTime + idleTimeout) - System.currentTimeMillis();
    }

    public boolean hasTimeout() {
        return getRemainingTime() < 0;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public DatagramChannel getChannel() {
        return channel;
    }

    public ByteBuffer getReceiveBuffer() {
        return receiveBuffer;
    }

    public long getIdleTimeout() {
        return idleTimeout;
    }

    public long getLastPacketTime() {
        return lastPacketTime;
    }

    public Runnable getTimeoutHandler() {
        return timeoutHandler;
    }

    public void setTimeoutHandler(Runnable timeoutHandler) {
        this.timeoutHandler = timeoutHandler;
    }

    public Cons<ByteBuffer> getPacketHandler() {
        return packetHandler;
    }

    public void setPacketHandler(Cons<ByteBuffer> packetHandler) {
        this.packetHandler = packetHandler;
    }

    public boolean isClosed() {
        return closed;
    }

}
