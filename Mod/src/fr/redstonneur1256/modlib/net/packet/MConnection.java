package fr.redstonneur1256.modlib.net.packet;

import arc.func.Cons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface MConnection {

    boolean supportsPacket(Class<?> packet);

    <T> boolean isCallAvailable(Class<T> type);

    /**
     * Send a reply packet
     */
    default void sendReply(@NotNull MPacket original, @NotNull MPacket reply) {
        sendPacket(reply, original, null, null, null, 0);
    }

    /**
     * Send a reply packet
     */
    default <R extends MPacket> void sendReply(@NotNull MPacket original, @NotNull MPacket reply,
                                               @NotNull Class<R> expectedReply, @NotNull Cons<R> callback) {
        sendPacket(reply, original, expectedReply, callback, null, 0);
    }

    /**
     * Send a reply packet
     */
    default <R extends MPacket> void sendReply(@NotNull MPacket original, @NotNull MPacket reply,
                                               @NotNull Class<R> expectedReply, @NotNull Cons<R> callback,
                                               @Nullable Runnable timeout, long timeoutDuration) {
        sendPacket(reply, original, expectedReply, callback, timeout, timeoutDuration);

    }

    /**
     * Send a reply packet
     */
    default <R extends MPacket> void sendPacket(@NotNull MPacket packet,
                                                @NotNull Class<R> expectedReply, @NotNull Cons<R> callback) {
        sendPacket(packet, null, expectedReply, callback, null, 0);
    }

    /**
     * Send a reply packet
     */
    default <R extends MPacket> void sendPacket(@NotNull MPacket packet,
                                                @NotNull Class<R> expectedReply, @NotNull Cons<R> callback,
                                                @Nullable Runnable timeout, long timeoutDuration) {
        sendPacket(packet, null, expectedReply, callback, timeout, timeoutDuration);
    }

    <R extends MPacket> void sendPacket(@NotNull MPacket packet, @Nullable MPacket original,
                                        @Nullable Class<R> expectedReply, @Nullable Cons<R> callback,
                                        @Nullable Runnable timeout, long timeoutDuration);

    void received(MPacket packet);

}
