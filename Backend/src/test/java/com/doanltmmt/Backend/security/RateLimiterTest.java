package com.doanltmmt.Backend.security;

import com.doanltmmt.Backend.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Kiểm thử đơn vị thuần cho bộ giới hạn tần suất (không cần Spring/DB). */
class RateLimiterTest {

    private final RateLimiter limiter = new RateLimiter();

    @Test
    void allowsUpToTheLimit_thenBlocksWithRetryAfter() {
        for (int i = 0; i < 3; i++) {
            limiter.check("login", "1.2.3.4", 3, Duration.ofMinutes(1));
        }

        TooManyRequestsException ex = assertThrows(TooManyRequestsException.class,
                () -> limiter.check("login", "1.2.3.4", 3, Duration.ofMinutes(1)));
        assertEquals(429, ex.getStatusCode().value());
        assertTrue(ex.getRetryAfterSeconds() >= 1 && ex.getRetryAfterSeconds() <= 60, "Retry-After = " + ex.getRetryAfterSeconds());
        assertEquals(String.valueOf(ex.getRetryAfterSeconds()), ex.getHeaders().getFirst("Retry-After"));
    }

    @Test
    void keysAndBucketsAreIndependent() {
        limiter.check("login", "1.1.1.1", 1, Duration.ofMinutes(1));

        assertThrows(TooManyRequestsException.class, () -> limiter.check("login", "1.1.1.1", 1, Duration.ofMinutes(1)));
        assertDoesNotThrow(() -> limiter.check("login", "2.2.2.2", 1, Duration.ofMinutes(1)), "IP khác không bị ảnh hưởng");
        assertDoesNotThrow(() -> limiter.check("otp", "1.1.1.1", 1, Duration.ofMinutes(1)), "nhóm khác không bị ảnh hưởng");
    }

    @Test
    void counterStartsOverWhenTheWindowHasPassed() throws InterruptedException {
        Duration window = Duration.ofMillis(150);
        limiter.check("x", "k", 1, window);
        assertThrows(TooManyRequestsException.class, () -> limiter.check("x", "k", 1, window));

        Thread.sleep(200);

        assertDoesNotThrow(() -> limiter.check("x", "k", 1, window), "cửa sổ mới phải cho phép lại");
    }

    @Test
    void evictExpired_removesFinishedWindowsOnly() throws InterruptedException {
        limiter.check("short", "k", 5, Duration.ofMillis(50));
        limiter.check("long", "k", 5, Duration.ofMinutes(5));
        assertEquals(2, limiter.size());

        Thread.sleep(100);
        limiter.evictExpired();

        assertEquals(1, limiter.size(), "chỉ còn cửa sổ dài");
    }

    @Test
    void reset_clearsEverything() {
        limiter.check("a", "k", 1, Duration.ofMinutes(1));
        limiter.reset();

        assertEquals(0, limiter.size());
        assertDoesNotThrow(() -> limiter.check("a", "k", 1, Duration.ofMinutes(1)));
    }
}
