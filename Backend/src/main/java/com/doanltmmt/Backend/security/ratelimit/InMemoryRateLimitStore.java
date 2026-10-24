package com.doanltmmt.Backend.security.ratelimit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/** Bộ đếm trong RAM của từng instance — mất khi restart là chấp nhận được (cửa sổ chỉ vài phút). */
@Component
@ConditionalOnProperty(name = "app.rate-limit.store", havingValue = "memory", matchIfMissing = true)
public class InMemoryRateLimitStore implements RateLimitStore {

    private final ConcurrentHashMap<String, Hit> windows = new ConcurrentHashMap<>();

    @Override
    public Hit hit(String key, long nowMillis, long windowMillis) {
        return windows.compute(key, (k, existing) ->
                existing == null || nowMillis >= existing.windowEndMillis()
                        ? new Hit(1, nowMillis + windowMillis)
                        : new Hit(existing.count() + 1, existing.windowEndMillis()));
    }

    @Override
    public void evictExpired(long nowMillis) {
        windows.entrySet().removeIf(e -> nowMillis >= e.getValue().windowEndMillis());
    }

    @Override
    public void clear() {
        windows.clear();
    }

    @Override
    public int size() {
        return windows.size();
    }
}
