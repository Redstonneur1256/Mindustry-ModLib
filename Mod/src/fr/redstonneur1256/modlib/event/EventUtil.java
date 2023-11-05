package fr.redstonneur1256.modlib.event;

import arc.Events;
import arc.func.Cons;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Reflect;

/**
 * Same as {@link Events} but you can unregister them
 */
public class EventUtil {

    private static final ObjectMap<Object, Seq<Cons<?>>> events;

    static {
        events = Reflect.get(Events.class, "events");
    }

    public static <T> RegisteredListener run(T type, Runnable listener) {
        return on(type, e -> listener.run());
    }

    public static <T> RegisteredListener on(Class<T> type, Cons<T> listener) {
        return on((Object) type, listener);
    }

    private static <T> RegisteredListener on(Object type, Cons<T> listener) {
        events.get(type, () -> new Seq<>(Cons.class)).add(listener);
        return new RegisteredListener(type, listener);
    }

    public static void unregister(RegisteredListener listener) {
        Seq<Cons<?>> listeners = events.get(listener.getType());
        if (listeners != null) {
            listeners.remove(listener.getAction());
        }
    }

}
