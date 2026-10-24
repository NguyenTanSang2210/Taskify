package com.doanltmmt.Backend.security.ratelimit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * P4-10 — bộ đếm dùng chung cho mọi instance, lưu ở bảng {@code rate_limit_windows} (V11).
 * Một câu upsert nguyên tử (khóa dòng) mỗi lần ghi nhận. Mọi thao tác chạy trong transaction riêng: lần bị chặn
 * (429 làm transaction của request rollback) vẫn được đếm.
 */
@Component
@ConditionalOnProperty(name = "app.rate-limit.store", havingValue = "jdbc")
@SuppressWarnings("null")
public class JdbcRateLimitStore implements RateLimitStore {

    private static final int MAX_KEY_LENGTH = 191;

    // gán lần lượt từ trái sang phải: điều kiện của window_end_ms vẫn đọc giá trị cũ
    private static final String UPSERT = """
            INSERT INTO rate_limit_windows (bucket_key, window_end_ms, hits) VALUES (?, ?, 1) AS n
            ON DUPLICATE KEY UPDATE
              hits = IF(rate_limit_windows.window_end_ms <= ?, 1, rate_limit_windows.hits + 1),
              window_end_ms = IF(rate_limit_windows.window_end_ms <= ?, n.window_end_ms, rate_limit_windows.window_end_ms)
            """;

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;

    public JdbcRateLimitStore(JdbcTemplate jdbc, PlatformTransactionManager txManager) {
        this.jdbc = jdbc;
        this.tx = new TransactionTemplate(txManager);
        this.tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public Hit hit(String key, long nowMillis, long windowMillis) {
        String k = storageKey(key);
        return tx.execute(status -> {
            jdbc.update(UPSERT, k, nowMillis + windowMillis, nowMillis, nowMillis);
            return jdbc.queryForObject("SELECT hits, window_end_ms FROM rate_limit_windows WHERE bucket_key = ?",
                    (rs, i) -> new Hit(rs.getInt(1), rs.getLong(2)), k);
        });
    }

    @Override
    public void evictExpired(long nowMillis) {
        tx.executeWithoutResult(status -> jdbc.update("DELETE FROM rate_limit_windows WHERE window_end_ms <= ?", nowMillis));
    }

    @Override
    public void clear() {
        tx.executeWithoutResult(status -> jdbc.update("DELETE FROM rate_limit_windows"));
    }

    @Override
    public int size() {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM rate_limit_windows", Integer.class);
        return n == null ? 0 : n;
    }

    /** Khóa dài (vd. email rất dài) được băm SHA-256 cho vừa cột khóa chính. */
    static String storageKey(String key) {
        if (key.length() <= MAX_KEY_LENGTH) return key;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
            return "sha256:" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
