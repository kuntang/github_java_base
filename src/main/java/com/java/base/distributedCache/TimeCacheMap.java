package com.java.base.distributedCache;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Expires keys that have not been updated in the configured number of seconds.
 * The algorithm used will take between expirationSecs and
 * expirationSecs * (1 + 1 / (numBuckets-1)) to actually expire the message.
 * <p/>
 * get, put, remove, containsKey, and size take O(numBuckets) time to run.
 * <p/>
 * The advantage of this design is that the expiration thread only locks the object
 * for O(1) time, meaning the object is essentially always available for gets/puts.
 */
public class TimeCacheMap<K, V> {
    //this default ensures things expire at most 50% past the expiration time
    private static final int DEFAULT_NUM_BUCKETS = 3;

    public interface ExpiredCallback<K, V> {
        void expire(K key, V val);
    }

    private LinkedList<ConcurrentHashMap<K, V>> _buckets;

    private final Object _lock = new Object();
    private Thread _cleaner;
    private ExpiredCallback _callback;

    public TimeCacheMap(int expirationSecs, int numBuckets, ExpiredCallback<K, V> callback) {
        if (numBuckets < 2) {
            throw new IllegalArgumentException("numBuckets must be >= 2");
        }
        _buckets = new LinkedList<ConcurrentHashMap<K, V>>();
        for (int i = 0; i < numBuckets; i++) {
            _buckets.add(new ConcurrentHashMap<K, V>());
        }
        _callback = callback;
        final long expirationMillis = expirationSecs * 1000L;
        final long sleepTime = expirationMillis / (numBuckets - 1);
        _cleaner = new Thread(new Runnable() {
            public void run() {
                try {
                    while (true) {
                        Map<K, V> dead = null;
                        Time.sleep(sleepTime);
                        synchronized (_lock) {
                            dead = _buckets.removeLast();
                            _buckets.addFirst(new ConcurrentHashMap<K, V>());
                        }
                        if (_callback != null) {
                            for (Entry<K, V> entry : dead.entrySet()) {
                                _callback.expire(entry.getKey(), entry.getValue());
                            }
                        }
                    }
                } catch (InterruptedException ex) {}
            }
        });
        _cleaner.setDaemon(true);
        _cleaner.start();
    }

    public TimeCacheMap(int expirationSecs, ExpiredCallback<K, V> callback) {
        this(expirationSecs, DEFAULT_NUM_BUCKETS, callback);
    }

    public TimeCacheMap(int expirationSecs) {
        this(expirationSecs, DEFAULT_NUM_BUCKETS);
    }

    public TimeCacheMap(int expirationSecs, int numBuckets) {
        this(expirationSecs, numBuckets, null);
    }


    public boolean containsKey(K key) {
        synchronized (_lock) {
            for (ConcurrentHashMap<K, V> bucket : _buckets) {
                if (bucket.containsKey(key)) {
                    return true;
                }
            }
            return false;
        }
    }

    public V get(K key) {
        synchronized (_lock) {
            for (ConcurrentHashMap<K, V> bucket : _buckets) {
                if (bucket.containsKey(key)) {
                    return bucket.get(key);
                }
            }
            return null;
        }
    }

    public void put(K key, V value) {
        synchronized (_lock) {
            Iterator<ConcurrentHashMap<K, V>> it = _buckets.iterator();
            ConcurrentHashMap<K, V> bucket = it.next();
            bucket.put(key, value);
//            while (it.hasNext()) {
//                bucket = it.next();
//                bucket.remove(key);
//            }
        }
    }

    public Object remove(K key) {
        synchronized (_lock) {
            for (ConcurrentHashMap<K, V> bucket : _buckets) {
                if (bucket.containsKey(key)) {
                    return bucket.remove(key);
                }
            }
            return null;
        }
    }

    public int size() {
        synchronized (_lock) {
            int size = 0;
            for (ConcurrentHashMap<K, V> bucket : _buckets) {
                size += bucket.size();
            }
            return size;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            _cleaner.interrupt();
        } finally {
            super.finalize();
        }
    }



    public static class Time {
//    public static Logger LOG = LoggerFactory.getLogger(Time.class);

        private static AtomicBoolean simulating = new AtomicBoolean(false);
        //TODO: should probably use weak references here or something
        private static Map<Thread, AtomicLong> threadSleepTimes;
        private static final Object sleepTimesLock = new Object();

        private static AtomicLong simulatedCurrTimeMs; //should this be a thread local that's allowed to keep advancing?

        public static void startSimulating() {
            simulating.set(true);
            simulatedCurrTimeMs = new AtomicLong(0);
            threadSleepTimes = new ConcurrentHashMap<Thread, AtomicLong>();
        }

        public static void stopSimulating() {
            simulating.set(false);
            threadSleepTimes = null;
        }

        public static boolean isSimulating() {
            return simulating.get();
        }

        public static void sleepUntil(long targetTimeMs) throws InterruptedException {
            if (simulating.get()) {
                try {
                    synchronized (sleepTimesLock) {
                        threadSleepTimes.put(Thread.currentThread(), new AtomicLong(targetTimeMs));
                    }
                    while (simulatedCurrTimeMs.get() < targetTimeMs) {
                        Thread.sleep(10);
                    }
                } finally {
                    synchronized (sleepTimesLock) {
                        threadSleepTimes.remove(Thread.currentThread());
                    }
                }
            } else {
                long sleepTime = targetTimeMs - currentTimeMillis();
                if (sleepTime > 0)
                    Thread.sleep(sleepTime);
            }
        }

        public static void sleep(long ms) throws InterruptedException {
            sleepUntil(currentTimeMillis() + ms);
        }

        public static long currentTimeMillis() {
            if (simulating.get()) {
                return simulatedCurrTimeMs.get();
            } else {
                return System.currentTimeMillis();
            }
        }

        public static void advanceTime(long ms) {
            if (!simulating.get()) throw new IllegalStateException("Cannot simulate time unless in simulation mode");
            simulatedCurrTimeMs.set(simulatedCurrTimeMs.get() + ms);
        }

        public static boolean isThreadWaiting(Thread t) {
            if (!simulating.get()) throw new IllegalStateException("Must be in simulation mode");
            AtomicLong time;
            synchronized (sleepTimesLock) {
                time = threadSleepTimes.get(t);
            }
            return !t.isAlive() || time != null && currentTimeMillis() < time.longValue();
        }
    }

}
