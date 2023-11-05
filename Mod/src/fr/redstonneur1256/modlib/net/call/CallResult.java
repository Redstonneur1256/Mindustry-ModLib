package fr.redstonneur1256.modlib.net.call;

import arc.func.Cons;
import fr.redstonneur1256.modlib.MVars;
import fr.redstonneur1256.modlib.net.packets.CustomInvokeResultPacket;
import fr.redstonneur1256.modlib.util.Task;

import java.util.concurrent.TimeUnit;

@SuppressWarnings("unchecked")
public class CallResult<T> extends Task<T> {

    public static <T> CallResult<T> of(T value) {
        CallResult<T> result = new CallResult<>();
        result.complete(value);
        return result;
    }

    public static <T> CallResult<T> failed(Throwable throwable) {
        CallResult<T> result = new CallResult<>();
        result.fail(throwable);
        return result;
    }

    public void listen(Cons<T> listener) {
        listen(listener, null, null, 0, null);
    }

    public void listen(Cons<T> listener, Cons<Throwable> failure) {
        listen(listener, failure, null, 0, null);
    }

    public void listen(Cons<T> listener, Cons<Throwable> failure, Runnable timeout, long time, TimeUnit unit) {
        onComplete(listener);
        onFail(failure);

        if (timeout != null) {
            MVars.net.getScheduler().schedule(() -> {
                if (!isCompleted()) {
                    synchronized (lock) {
                        listeners.remove(listener);
                        failListeners.remove(failure);
                        timeout.run();
                    }
                }
            }, time, unit);
        }
    }

    /**
     * This is called internally to complete the method execution
     */
    public void complete(CustomInvokeResultPacket packet) {
        if (packet.throwable != null) {
            fail(packet.throwable);
        } else {
            complete((T) packet.result);
        }

    }

}
