package com.doanltmmt.Backend.security;

import com.doanltmmt.Backend.exception.TooManyRequestsException;
import com.doanltmmt.Backend.security.ratelimit.InMemoryRateLimitStore;
import com.doanltmmt.Backend.security.ratelimit.RateLimitStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Giới hạn số request theo (nhóm, khóa) bằng cửa sổ cố định. Dùng cho các endpoint công khai dễ bị lạm dụng
 * (đăng nhập, OTP...) theo IP. Bộ đếm nằm trong {@link RateLimitStore}: RAM của từng instance (mặc định) hoặc
 * bảng dùng chung khi chạy nhiều instance ({@code app.rate-limit.store=jdbc}, P4-10).
 */
@Component
public class RateLimiter {

    private final RateLimitStore store;

    @Autowired
    public RateLimiter(RateLimitStore store) {
        this.store = store;
    }

    /** Bộ đếm trong RAM (test đơn vị). */
    public RateLimiter() {
        this(new InMemoryRateLimitStore());
    }

    /**
     * Ghi nhận một request; ném 429 (kèm Retry-After) nếu đã vượt {@code max} request trong {@code window}.
     *
     * @param bucket nhóm giới hạn, ví dụ "login"
     * @param key    đối tượng bị giới hạn, thường là IP
     */
    public void check(String bucket, String key, int max, Duration window) {
        long now = System.currentTimeMillis();
        RateLimitStore.Hit current = store.hit(bucket + "|" + key, now, window.toMillis());

        if (current.count() > max) {
            long retryAfter = Math.max(1, (long) Math.ceil((current.windowEndMillis() - now) / 1000.0));
            throw new TooManyRequestsException(
                    "Bạn thao tác quá nhanh. Vui lòng thử lại sau " + retryAfter + " giây.", retryAfter);
        }
    }

    /** Xóa các cửa sổ đã hết hạn để bộ nhớ / bảng không phình ra (chạy trên mọi instance — không cần khóa). */
    @Scheduled(fixedDelay = 60_000)
    public void evictExpired() {
        store.evictExpired(System.currentTimeMillis());
    }

    /** Dùng cho test: xóa toàn bộ bộ đếm. */
    public void reset() {
        store.clear();
    }

    int size() {
        return store.size();
    }
}
