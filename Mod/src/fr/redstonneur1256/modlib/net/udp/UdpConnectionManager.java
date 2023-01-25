package fr.redstonneur1256.modlib.net.udp;

import arc.func.Cons;
import arc.util.Log;
import arc.util.Threads;
import arc.util.UnsafeRunnable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * A simple class managing multiple UDP connections from a single thread
 */
public class UdpConnectionManager {

    /**
     * Does not need to be concurrent as all the operations are being run from the network thread
     */
    private PriorityQueue<SimpleUdpConnection> connections;
    private Queue<UnsafeRunnable> taskQueue;
    private Selector selector;
    private Thread thread;
    private boolean shutdown;

    public UdpConnectionManager() {
        try {
            this.connections = new PriorityQueue<>(Comparator.comparingLong(SimpleUdpConnection::getRemainingTime));
            this.taskQueue = new ConcurrentLinkedDeque<>();
            this.selector = Selector.open();
            this.thread = Threads.daemon(getClass().getSimpleName(), this::run);
            this.shutdown = false;
        } catch(IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void shutdown() {
        shutdown = true;
        selector.wakeup();
    }

    public void connect(InetSocketAddress address, int idleTimeout, ByteBuffer receiveBuffer, Cons<SimpleUdpConnection> onConnect) {
        if(idleTimeout <= 0) {
            throw new IllegalStateException("Connection idle timeout cannot be <= 0");
        }

        checkShutdown();
        submit(() -> {
            if(address.isUnresolved()) {
                Log.warn("Attempted to connect to an unresolved address @", address);
                return;
            }
            DatagramChannel channel = selector.provider().openDatagramChannel();
            channel.connect(address);

            channel.configureBlocking(false);
            SelectionKey key = channel.register(selector, SelectionKey.OP_READ);

            SimpleUdpConnection connection = new SimpleUdpConnection(this, address, channel, key, receiveBuffer, idleTimeout);
            key.attach(connection);
            connections.offer(connection);

            onConnect.get(connection);
        });
    }

    private void run() {
        int emptySelects = 0;

        while(!shutdown) {
            try {
                long nextTimeout = connections.isEmpty() ? 0 : Math.max(0, connections.peek().getRemainingTime());
                int selectedCount = selector.select(nextTimeout);

                while(!taskQueue.isEmpty()) {
                    UnsafeRunnable runnable = taskQueue.poll();
                    try {
                        runnable.run();
                    } catch(Throwable throwable) {
                        Log.err("Failed to execute task @", runnable);
                        Log.err(throwable);
                    }
                }

                for(Iterator<SimpleUdpConnection> iterator = connections.iterator(); iterator.hasNext(); ) {
                    SimpleUdpConnection connection = iterator.next();
                    if(connection.hasTimeout()) {
                        connection.close(true);
                        iterator.remove();
                    }
                }

                if(selectedCount == 0) {
                    if(emptySelects++ == 250) {
                        emptySelects = 0;
                        Threads.sleep(25);
                    }
                    continue;
                }

                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while(iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    if(key.isReadable()) {
                        DatagramChannel channel = (DatagramChannel) key.channel();
                        SimpleUdpConnection connection = (SimpleUdpConnection) key.attachment();
                        try {
                            ByteBuffer buffer = connection.getReceiveBuffer();

                            buffer.clear();
                            channel.receive(buffer);
                            buffer.flip();

                            connection.handle(buffer);
                        } catch(Throwable throwable) {
                            Log.err("Error reading from UDP connection", throwable);
                        }
                    }

                    iterator.remove();
                }

            } catch(Throwable throwable) {
                if(shutdown) {
                    return;
                }
                Log.err("Error in selector thread", throwable);
            }
        }
    }

    protected void submit(UnsafeRunnable runnable) {
        checkShutdown();

        taskQueue.add(runnable);
        selector.wakeup();
    }

    private void checkShutdown() {
        if(shutdown) {
            throw new IllegalStateException("This connection manager has been shut down");
        }
    }

    protected void removeConnection(SimpleUdpConnection connection) {
        submit(() -> connections.remove(connection));
    }

    public Thread getThread() {
        return thread;
    }

}
