package com.jobseekercopilot.postcodeiogateway.client;

import com.jobseekercopilot.postcodeiogateway.model.PostcodeLocation;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.LongSupplier;

final class ProviderCache {
    private final Map<String, Entry> entries;
    private final LongSupplier nanoTime;

    ProviderCache(int maximumEntries, LongSupplier nanoTime) {
        this.nanoTime = nanoTime;
        this.entries = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Entry> eldest) {
                return size() > maximumEntries;
            }
        };
    }

    synchronized Entry get(String key) {
        Entry entry = entries.get(key);
        if (entry != null && nanoTime.getAsLong() >= entry.expiresAtNanos()) {
            entries.remove(key);
            return null;
        }
        return entry;
    }

    synchronized void put(String key, PostcodeLocation location, Duration ttl) {
        entries.put(key, new Entry(location, true, nanoTime.getAsLong() + ttl.toNanos()));
    }

    synchronized void putNegative(String key, Duration ttl) {
        entries.put(key, new Entry(null, false, nanoTime.getAsLong() + ttl.toNanos()));
    }

    record Entry(PostcodeLocation location, boolean found, long expiresAtNanos) {
    }
}
