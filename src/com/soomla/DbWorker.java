package com.soomla;

import android.annotation.TargetApi;
import android.os.Build;

import java.util.AbstractMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * @author vedi
 *         date 04/10/15
 */
@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class DbWorker {

    private final DbSaver saver;
    private final Object lock;

    private List<Map.Entry<String, String>> toSaveList = new LinkedList<>();
    private Map<String, Map.Entry<String, String>> toSaveMap =  new ConcurrentHashMap<>();

    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);

    private Runnable workerRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (lock) {
                Map.Entry<String, String> entry;
                while ((entry = DbWorker.this.pop()) != null) {
                    DbWorker.this.saver.save(entry.getKey(), entry.getValue());
                }
            }
        }
    };

    public DbWorker(DbSaver saver, Object lock) {
        if (saver == null) {
            throw new IllegalArgumentException();
        }
        if (lock == null) {
            throw new IllegalArgumentException();
        }

        this.saver = saver;
        this.lock = lock;
    }

    public synchronized void put(String key, String value) {
        boolean run = toSaveList.isEmpty();

        Map.Entry<String, String> entry = toSaveMap.get(key);
        if (entry != null) {
            entry.setValue(value);
        } else {
            entry = new AbstractMap.SimpleEntry<>(key, value);
            toSaveMap.put(key, entry);
            toSaveList.add(entry);
        }

        if (run) {
            scheduledThreadPoolExecutor.execute(workerRunnable);
        }
    }

    private synchronized Map.Entry<String, String> pop() {
        if (!toSaveList.isEmpty()) {
            Map.Entry<String, String> entry = toSaveList.remove(0);
            toSaveMap.remove(entry.getKey());
            return entry;
        } else {
            return null;
        }
    }

    public String get(String key) {
        Map.Entry<String, String> entry = toSaveMap.get(key);
        if (entry != null) {
            return entry.getValue();
        } else {
            return null;
        }
    }
}
