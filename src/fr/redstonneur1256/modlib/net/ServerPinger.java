package fr.redstonneur1256.modlib.net;

import arc.Core;
import arc.func.Cons;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Time;
import arc.util.Timer;
import arc.util.UnsafeRunnable;
import arc.util.async.Threads;
import mindustry.net.Host;
import mindustry.net.NetworkIO;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.concurrent.TimeoutException;

public class ServerPinger {

    private static final ByteBuffer PING_HEADER;

    static {
        PING_HEADER = ByteBuffer.wrap(new byte[] { -2, 1 });
    }

    private Seq<UnsafeRunnable> queue;
    private boolean initialized;
    private Selector selector;
    private Thread selectorThread;

    public ServerPinger() {
        queue = new Seq<>();
        initialized = false;
        selector = null;
    }

    public void ping(String address, int port, Cons<Host> callback, Cons<Exception> failure) {
        initialize();

        queue.add(() -> {
            InetSocketAddress socketAddress = new InetSocketAddress(address, port);

            if(socketAddress.isUnresolved()) {
                Log.warn("Attempted to ping unresolved address @:@", address, port);
                return;
            }

            DatagramChannel channel = selector.provider().openDatagramChannel();

            channel.connect(socketAddress);

            PingInfo info = new PingInfo(Time.millis(), channel, address, callback, failure);

            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_READ, info);

            PING_HEADER.position(0);
            channel.send(PING_HEADER, socketAddress);

            Timer.schedule(info::timeout, 2);
        });
        selector.wakeup();
    }

    private synchronized void initialize() {
        if(initialized) {
            return;
        }

        try {
            selector = Selector.open();

            selectorThread = Threads.daemon(this::run);

            initialized = true;
        }catch(Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    public synchronized void dispose() {
        if(!initialized) {
            throw new IllegalStateException("The pinger is not initialized");
        }
        try {
            selectorThread.interrupt();
            selector.close();

            initialized = false;
        }catch(Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private void run() {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(512);

            int emptySelects = 0;
            while(!selectorThread.isInterrupted()) {
                int select = selector.select();

                while(!queue.isEmpty()) {
                    UnsafeRunnable runnable = queue.remove(0);
                    try {
                        runnable.run();
                    }catch(Throwable throwable) {
                        Log.err("Error while running runnable " + runnable, throwable);
                    }
                }

                if(select == 0) {
                    emptySelects++;

                    if(emptySelects == 250) {
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
                        PingInfo info = (PingInfo) key.attachment();

                        try {
                            buffer.clear();
                            channel.receive(buffer);

                            int ping = (int) Time.timeSinceMillis(info.start);

                            buffer.position(0);
                            Host host = NetworkIO.readServerData(ping, info.address, buffer);
                            info.complete(host);
                        }catch(Exception exception) {
                            // Java throws the exception on read instead of write
                            key.cancel();
                            try {
                                channel.close();
                            }catch(Exception ignored) {
                            }
                        }
                    }

                    iterator.remove();
                }
            }
        }catch(IOException exception) {
            exception.printStackTrace();
            // TODO: Handle exception
        }
    }

    private static class PingInfo {

        private long start;
        private DatagramChannel channel;
        private String address;
        private Cons<Host> callback;
        private Cons<Exception> failure;
        private boolean complete;

        private PingInfo(long start, DatagramChannel channel, String address, Cons<Host> callback, Cons<Exception> failure) {
            this.start = start;
            this.channel = channel;
            this.address = address;
            this.callback = callback;
            this.failure = failure;
        }

        private void complete(Host host) {
            if(!complete) {
                complete();
                Core.app.post(() -> callback.get(host));
            }
        }

        private void timeout() {
            if(!complete) {
                complete();
                Core.app.post(() -> failure.get(new TimeoutException()));
            }
        }

        private void complete() {
            complete = true;
            try {
                channel.close();
            }catch(IOException exception) {
                Log.err("Failed to close ping channel", exception);
            }
        }

    }

}
