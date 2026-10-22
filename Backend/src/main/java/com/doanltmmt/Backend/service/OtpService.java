package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.entity.OtpPurpose;
import com.doanltmmt.Backend.entity.OtpToken;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.exception.TooManyRequestsException;
import com.doanltmmt.Backend.repository.OtpTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Cấp và xác minh mã OTP (6 chữ số).
 *
 * <ul>
 *   <li>Mỗi (người dùng, mục đích) chỉ có MỘT mã còn hiệu lực: cấp mã mới ⇒ mã cũ bị hủy.</li>
 *   <li>Tối đa {@value #MAX_ATTEMPTS} lần nhập sai/mã; đạt giới hạn ⇒ mã bị hủy (phải xin mã mới).</li>
 *   <li>So sánh mã theo thời gian hằng số; hàng OTP được khóa khi xác minh nên nhập đồng thời không lách được giới hạn.</li>
 *   <li>Email chỉ được gửi SAU khi mã đã lưu xong — không giữ giao dịch DB trong lúc chờ SMTP.</li>
 * </ul>
 */
@Service
public class OtpService {

    public static final int MAX_ATTEMPTS = 5;
    public static final int RESEND_COOLDOWN_SECONDS = 60;

    public enum VerifyResult {
        /** Mã đúng (mã bị đánh dấu đã dùng). */
        OK,
        /** Sai mã, còn lượt thử. */
        INVALID,
        /** Mã đã hết hạn. */
        EXPIRED,
        /** Sai quá số lần cho phép — mã đã bị hủy. */
        LOCKED,
        /** Không có mã nào còn hiệu lực (chưa cấp / đã dùng / đã hủy). */
        NONE
    }

    public record VerifyOutcome(VerifyResult result, int remainingAttempts) {
    }

    private final OtpTokenRepository repo;
    private final EmailService emailService;
    private final TransactionTemplate tx;
    private final SecureRandom random = new SecureRandom();

    public OtpService(OtpTokenRepository repo, EmailService emailService, TransactionTemplate tx) {
        this.repo = repo;
        this.emailService = emailService;
        this.tx = tx;
    }

    /** Cấp mã mới (hủy mã cũ cùng mục đích) rồi gửi email cho người dùng. */
    public void issue(User user, OtpPurpose purpose) {
        String code = tx.execute(status -> createToken(user, purpose));
        EmailTemplates.Mail mail = mail(purpose, code);
        emailService.sendEmail(user.getEmail(), mail.subject(), mail.html());
    }

    /** Như {@link #issue} nhưng từ chối (429) nếu mã gần nhất vừa được tạo cách đây chưa tới thời gian chờ. */
    public void issueWithCooldown(User user, OtpPurpose purpose) {
        long wait = secondsUntilResendAllowed(user, purpose);
        if (wait > 0) {
            throw new TooManyRequestsException("Vui lòng chờ " + wait + " giây trước khi yêu cầu mã mới.", wait);
        }
        issue(user, purpose);
    }

    /** Số giây còn phải chờ trước khi được gửi lại mã (0 nếu được phép ngay). */
    public long secondsUntilResendAllowed(User user, OtpPurpose purpose) {
        Optional<OtpToken> latest = repo.findTopByUserAndPurposeOrderByIdDesc(user, purpose);
        if (latest.isEmpty() || latest.get().getCreatedAt() == null) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime allowedAt = latest.get().getCreatedAt().plusSeconds(RESEND_COOLDOWN_SECONDS);
        if (!allowedAt.isAfter(now)) {
            return 0;
        }
        // làm tròn lên để không báo "còn 0 giây" khi thực ra vẫn phải chờ
        return Math.max(1, (long) Math.ceil(Duration.between(now, allowedAt).toMillis() / 1000.0));
    }

    /** Xác minh mã. Không ném lỗi nghiệp vụ — trả kết quả để nơi gọi quyết định thông báo. */
    public VerifyOutcome verify(User user, OtpPurpose purpose, String code) {
        return tx.execute(status -> {
            Optional<OtpToken> active = repo.findTopByUserAndPurposeAndUsedFalseOrderByIdDesc(user, purpose);
            if (active.isEmpty()) {
                return new VerifyOutcome(VerifyResult.NONE, 0);
            }
            OtpToken token = active.get();

            if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
                token.setUsed(true);
                repo.save(token);
                return new VerifyOutcome(VerifyResult.EXPIRED, 0);
            }
            if (token.getAttempts() >= MAX_ATTEMPTS) {
                token.setUsed(true);
                repo.save(token);
                return new VerifyOutcome(VerifyResult.LOCKED, 0);
            }
            if (constantTimeEquals(token.getCode(), normalize(code))) {
                token.setUsed(true);
                repo.save(token);
                return new VerifyOutcome(VerifyResult.OK, MAX_ATTEMPTS - token.getAttempts());
            }

            token.setAttempts(token.getAttempts() + 1);
            if (token.getAttempts() >= MAX_ATTEMPTS) {
                token.setUsed(true);
                repo.save(token);
                return new VerifyOutcome(VerifyResult.LOCKED, 0);
            }
            repo.save(token);
            return new VerifyOutcome(VerifyResult.INVALID, MAX_ATTEMPTS - token.getAttempts());
        });
    }

    /** Hủy mọi mã còn hiệu lực của (user, mục đích). */
    public void invalidateAll(User user, OtpPurpose purpose) {
        tx.executeWithoutResult(status -> repo.findByUserAndPurposeAndUsedFalse(user, purpose).forEach(t -> {
            t.setUsed(true);
            repo.save(t);
        }));
    }

    // ------------------------------------------------------------------ nội bộ

    private String createToken(User user, OtpPurpose purpose) {
        repo.findByUserAndPurposeAndUsedFalse(user, purpose).forEach(old -> {
            old.setUsed(true);
            repo.save(old);
        });

        String code = String.format("%06d", random.nextInt(1_000_000));
        OtpToken token = new OtpToken();
        token.setUser(user);
        token.setPurpose(purpose);
        token.setCode(code);
        token.setExpiresAt(LocalDateTime.now().plus(lifetime(purpose)));
        token.setUsed(false);
        token.setAttempts(0);
        repo.save(token);
        return code;
    }

    static Duration lifetime(OtpPurpose purpose) {
        return switch (purpose) {
            case LOGIN -> Duration.ofMinutes(5);
            case ACTIVATION -> Duration.ofHours(72);
            case RESET_PASSWORD -> Duration.ofMinutes(10);
        };
    }

    private static String normalize(String code) {
        return code == null ? "" : code.trim();
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    private static EmailTemplates.Mail mail(OtpPurpose purpose, String code) {
        return switch (purpose) {
            case LOGIN -> EmailTemplates.loginOtp(code);
            case ACTIVATION -> EmailTemplates.activation(code);
            case RESET_PASSWORD -> EmailTemplates.resetPassword(code);
        };
    }
}
