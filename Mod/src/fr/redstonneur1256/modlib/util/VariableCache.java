package fr.redstonneur1256.modlib.util;

import arc.struct.ObjectMap;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A cache which unlike {@link com.google.common.cache.Cache} can have per value time to live
 */
public class VariableCache<K, V> {

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "ML-Cache-Cleaner");
        thread.setDaemon(true);
        return thread;
    });

    private ObjectMap<K, CacheValue> content;

    public VariableCache() {
        this.content = new ObjectMap<>();
    }

    public V get(K key) {
        CacheValue value = content.get(key);
        return value != null ? value.value : null;
    }

    public void put(K key, V value, Duration ttl) {
        CacheValue newValue = new CacheValue(Instant.now().plus(ttl), key, value);
        newValue.task = SCHEDULER.schedule(() -> remove(key), ttl.toMillis(), TimeUnit.MILLISECONDS);
        CacheValue previous = content.put(key, newValue);
        if(previous != null) {
            previous.task.cancel(false);
        }
    }

    public void remove(K key) {
        CacheValue value = content.remove(key);
        if(value != null) {
            value.task.cancel(false);
        }
    }

    /**
     * Getter for methods that might not be implemented directly, do not modify values directly
     */
    public ObjectMap<K, CacheValue> getContent() {
        return content;
    }

    public class CacheValue {

        public final Instant expiration;
        public final K key;
        public final V value;
        private Future<?> task;

        private CacheValue(Instant expiration, K key, V value) {
            this.expiration = expiration;
            this.key = key;
            this.value = value;
        }

    }

}
