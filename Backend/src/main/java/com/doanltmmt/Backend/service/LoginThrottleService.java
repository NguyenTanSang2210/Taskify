package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.config.SecurityProperties;
import com.doanltmmt.Backend.entity.LoginThrottle;
import com.doanltmmt.Backend.exception.TooManyRequestsException;
import com.doanltmmt.Backend.repository.LoginThrottleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

/**
 * Chống dò mật khẩu: {@code maxFailures} lần sai trong {@code window} ⇒ khóa cặp (username, IP) trong {@code lock}.
 *
 * <ul>
 *   <li>Khóa theo CẶP (không theo username) để kẻ xấu không khóa được tài khoản của người khác từ IP khác.</li>
 *   <li>Lưu DB ⇒ còn nguyên sau khi restart backend và dùng chung được giữa nhiều instance.</li>
 *   <li>Đang khóa thì từ chối kể cả khi mật khẩu đúng (kẻ dò mật khẩu không biết đã đoán trúng).</li>
 * </ul>
 */
@Service
public class LoginThrottleService {

    private static final Logger log = LoggerFactory.getLogger(LoginThrottleService.class);

    private final LoginThrottleRepository repo;
    private final TransactionTemplate tx;
    private final SecurityProperties props;

    public LoginThrottleService(LoginThrottleRepository repo, TransactionTemplate tx, SecurityProperties props) {
        this.repo = repo;
        this.tx = tx;
        this.props = props;
    }

    /** Ném 429 (kèm Retry-After) nếu cặp (username, IP) đang bị khóa. */
    public void assertNotLocked(String username, String ip) {
        Optional<LoginThrottle> row = repo.findById(keyOf(username, ip));
        if (row.isEmpty() || row.get().getLockedUntil() == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime until = row.get().getLockedUntil();
        if (until.isAfter(now)) {
            long seconds = Math.max(1, (long) Math.ceil(Duration.between(now, until).toMillis() / 1000.0));
            long minutes = Math.max(1, (seconds + 59) / 60);
            throw new TooManyRequestsException(
                    "Đăng nhập sai quá nhiều lần. Vui lòng thử lại sau khoảng " + minutes + " phút.", seconds);
        }
    }

    /**
     * Ghi nhận một lần sai.
     *
     * @return true nếu lần sai này khiến cặp (username, IP) bị khóa
     */
    public boolean recordFailure(String username, String ip) {
        String key = keyOf(username, ip);
        try {
            return Boolean.TRUE.equals(tx.execute(status -> increment(key)));
        } catch (DataIntegrityViolationException raceOnFirstInsert) {
            // Hai lần sai đồng thời cùng chèn dòng đầu tiên: dòng đã có nên thử lại một lần (lúc này sẽ khóa hàng và cộng dồn)
            return Boolean.TRUE.equals(tx.execute(status -> increment(key)));
        }
    }

    /** Đăng nhập thành công ⇒ xóa bộ đếm của cặp này. */
    public void recordSuccess(String username, String ip) {
        String key = keyOf(username, ip);
        if (repo.existsById(key)) {
            repo.deleteById(key);
        }
    }

    private boolean increment(String key) {
        LocalDateTime now = LocalDateTime.now();
        Duration window = Duration.ofMinutes(props.getLogin().getWindowMinutes());
        LoginThrottle row = repo.findByKeyForUpdate(key).orElseGet(() -> new LoginThrottle(key, now));

        if (row.getWindowStart().isBefore(now.minus(window))) {
            row.setFailures(0);          // cửa sổ cũ đã qua — đếm lại từ đầu
            row.setWindowStart(now);
        }
        row.setFailures(row.getFailures() + 1);

        boolean locked = false;
        if (row.getFailures() >= props.getLogin().getMaxFailures()) {
            row.setLockedUntil(now.plusMinutes(props.getLogin().getLockMinutes()));
            row.setFailures(0);
            row.setWindowStart(now);
            locked = true;
        }
        repo.save(row);
        return locked;
    }

    /** Dọn các bộ đếm đã hết ý nghĩa (hằng ngày, 03:15). */
    @Scheduled(cron = "0 15 3 * * *")
    public void cleanUp() {
        LocalDateTime now = LocalDateTime.now();
        int removed = tx.execute(status ->
                repo.deleteStale(now.minus(Duration.ofMinutes(props.getLogin().getWindowMinutes())), now));
        if (removed > 0) {
            log.info("Đã dọn {} bộ đếm đăng nhập hết hạn", removed);
        }
    }

    static String keyOf(String username, String ip) {
        String raw = (username == null ? "" : username.trim().toLowerCase(Locale.ROOT)) + "|" + (ip == null ? "" : ip);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 không khả dụng", e);
        }
    }
}
