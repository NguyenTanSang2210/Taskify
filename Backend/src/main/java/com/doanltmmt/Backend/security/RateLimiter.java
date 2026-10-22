package com.doanltmmt.Backend.security;

import com.doanltmmt.Backend.exception.TooManyRequestsException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Giới hạn số request theo (nhóm, khóa) bằng cửa sổ cố định, lưu trong RAM. Dùng cho các endpoint công khai
 * dễ bị lạm dụng (đăng nhập, OTP...) theo IP. Mất khi restart là chấp nhận được (cửa sổ chỉ vài phút);
 * khi chạy nhiều instance cần chuyển sang kho dùng chung (Redis — xem P4-10).
 */
@Component
public class RateLimiter {

    private record Window(long endMillis, int count) {
    }

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    /**
     * Ghi nhận một request; ném 429 (kèm Retry-After) nếu đã vượt {@code max} request trong {@code window}.
     *
     * @param bucket nhóm giới hạn, ví dụ "login"
     * @param key    đối tượng bị giới hạn, thường là IP
     */
    public void check(String bucket, String key, int max, Duration window) {
        long now = System.currentTimeMillis();
        Window current = windows.compute(bucket + "|" + key, (k, existing) ->
                existing == null || now >= existing.endMillis()
                        ? new Window(now + window.toMillis(), 1)
                        : new Window(existing.endMillis(), existing.count() + 1));

        if (current.count() > max) {
            long retryAfter = Math.max(1, (long) Math.ceil((current.endMillis() - now) / 1000.0));
            throw new TooManyRequestsException(
                    "Bạn thao tác quá nhanh. Vui lòng thử lại sau " + retryAfter + " giây.", retryAfter);
        }
    }

    /** Xóa các cửa sổ đã hết hạn để bộ nhớ không phình ra. */
    @Scheduled(fixedDelay = 60_000)
    public void evictExpired() {
        long now = System.currentTimeMillis();
        windows.entrySet().removeIf(e -> now >= e.getValue().endMillis());
    }

    /** Dùng cho test: xóa toàn bộ bộ đếm. */
    public void reset() {
        windows.clear();
    }

    int size() {
        return windows.size();
    }
}
