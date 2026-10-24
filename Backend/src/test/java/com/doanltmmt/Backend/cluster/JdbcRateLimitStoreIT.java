package com.doanltmmt.Backend.cluster;

import com.doanltmmt.Backend.exception.TooManyRequestsException;
import com.doanltmmt.Backend.security.RateLimiter;
import com.doanltmmt.Backend.security.ratelimit.JdbcRateLimitStore;
import com.doanltmmt.Backend.security.ratelimit.RateLimitStore;
import com.doanltmmt.Backend.service.EmailService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P4-10 — bộ đếm giới hạn tần suất dùng chung (bảng {@code rate_limit_windows}): hai "instance" (hai RateLimiter, hai
 * kho riêng) cùng đếm một giới hạn; tăng đồng thời không mất lượt; cửa sổ hết hạn thì đếm lại; lần bị chặn vẫn được đếm.
 */
@SpringBootTest
@SuppressWarnings("null")
class JdbcRateLimitStoreIT {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager txManager;
    @MockitoBean private EmailService emailService;

    private final String prefix = "it-" + Long.toString(System.nanoTime(), 36);

    private JdbcRateLimitStore newStore() {
        return new JdbcRateLimitStore(jdbc, txManager);
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM rate_limit_windows WHERE bucket_key LIKE ?", prefix + "%");
    }

    @Test
    void twoInstancesShareOneLimit() {
        RateLimiter a = new RateLimiter(newStore());
        RateLimiter b = new RateLimiter(newStore());
        a.check(prefix, "1.2.3.4", 3, Duration.ofMinutes(1));
        b.check(prefix, "1.2.3.4", 3, Duration.ofMinutes(1));
        a.check(prefix, "1.2.3.4", 3, Duration.ofMinutes(1));

        TooManyRequestsException ex = assertThrows(TooManyRequestsException.class,
                () -> b.check(prefix, "1.2.3.4", 3, Duration.ofMinutes(1)));
        assertTrue(ex.getRetryAfterSeconds() >= 1 && ex.getRetryAfterSeconds() <= 60);
        assertDoesNotThrow(() -> b.check(prefix, "5.6.7.8", 3, Duration.ofMinutes(1)), "khóa khác không bị ảnh hưởng");
        // lần bị chặn vẫn được ghi nhận (transaction riêng, không bị rollback theo request)
        assertEquals(4, jdbc.queryForObject("SELECT hits FROM rate_limit_windows WHERE bucket_key = ?", Integer.class, prefix + "|1.2.3.4"));
    }

    @Test
    void concurrentHitsAreNotLost() throws Exception {
        RateLimitStore store = newStore();
        long now = System.currentTimeMillis();
        AtomicInteger allowed = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Void>> calls = new ArrayList<>();
            for (int i = 0; i < 40; i++) {
                calls.add(() -> {
                    if (store.hit(prefix + "|race", now, 60_000).count() <= 10) allowed.incrementAndGet();
                    return null;
                });
            }
            for (Future<Void> f : pool.invokeAll(calls)) f.get();
        } finally {
            pool.shutdown();
        }
        assertEquals(10, allowed.get(), "đúng 10 lượt được phép dù gọi đồng thời");
        assertEquals(40, store.hit(prefix + "|race", now, 60_000).count() - 1);
    }

    @Test
    void expiredWindowStartsOver_andEvictRemovesIt() {
        RateLimitStore store = newStore();
        long t0 = System.currentTimeMillis();
        store.hit(prefix + "|k", t0, 1_000);
        store.hit(prefix + "|k", t0, 1_000);
        assertEquals(3, store.hit(prefix + "|k", t0 + 500, 1_000).count());

        RateLimitStore.Hit fresh = store.hit(prefix + "|k", t0 + 1_000, 1_000);
        assertEquals(1, fresh.count(), "cửa sổ mới");
        assertEquals(t0 + 2_000, fresh.windowEndMillis());

        store.evictExpired(t0 + 5_000);
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM rate_limit_windows WHERE bucket_key LIKE ?", Integer.class, prefix + "%"));
    }

    @Test
    void longKeysAreHashedToFitTheColumn() {
        jdbc.update("DELETE FROM rate_limit_windows WHERE bucket_key LIKE 'sha256:%'");
        RateLimitStore store = newStore();
        String longKey = prefix + "|" + "x".repeat(400);
        assertEquals(1, store.hit(longKey, System.currentTimeMillis(), 60_000).count());
        assertEquals(2, store.hit(longKey, System.currentTimeMillis(), 60_000).count());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM rate_limit_windows WHERE bucket_key LIKE 'sha256:%' AND hits = 2", Integer.class));
        jdbc.update("DELETE FROM rate_limit_windows WHERE bucket_key LIKE 'sha256:%'");
    }
}
