package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.entity.RefreshToken;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.repository.RefreshTokenRepository;
import com.doanltmmt.Backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * P3-14 — refresh token: chuỗi ngẫu nhiên 256 bit gửi trong cookie HttpOnly, CSDL chỉ giữ SHA-256 của nó.
 * <ul>
 *   <li><b>Xoay vòng:</b> mỗi lần làm mới, token cũ bị thu hồi và thay bằng token mới cùng "họ" (family).</li>
 *   <li><b>Phát hiện dùng lại:</b> token đã bị thay mà còn được gửi lên (quá {@link #REUSE_GRACE}) → coi như bị đánh
 *       cắp: thu hồi MỌI phiên của người dùng và tăng tokenVersion (access token đang lưu hành cũng chết).</li>
 *   <li>Trong {@link #REUSE_GRACE} sau khi xoay vòng (hai tab cùng làm mới một lúc) vẫn cấp token mới, không báo động.</li>
 *   <li>Đổi mật khẩu / đăng xuất mọi nơi / khóa tài khoản (tokenVersion đổi) làm mọi refresh token cũ mất hiệu lực.</li>
 * </ul>
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    static final Duration REUSE_GRACE = Duration.ofSeconds(30);
    public static final String SESSION_EXPIRED = "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.";

    /** Token mới cấp: chuỗi thô (chỉ trả cho client, không lưu) và người dùng sở hữu. */
    public record Issued(String rawToken, User user) {
    }

    private final RefreshTokenRepository repo;
    private final UserRepository userRepo;
    private final AuditLogService auditLogService;
    private final Duration ttl;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository repo, UserRepository userRepo, AuditLogService auditLogService,
                               @Value("${app.auth.refresh-ttl-days:7}") long ttlDays) {
        this.repo = repo;
        this.userRepo = userRepo;
        this.auditLogService = auditLogService;
        this.ttl = Duration.ofDays(ttlDays);
    }

    public Duration getTtl() {
        return ttl;
    }

    /** Phiên mới sau khi đăng nhập xong (họ token mới). */
    @Transactional
    public String issue(Long userId, String userAgent, String ip) {
        User user = userRepo.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, SESSION_EXPIRED));
        return create(user, UUID.randomUUID().toString(), userAgent, ip).raw();
    }

    /**
     * Đổi refresh token lấy token mới. Không rollback khi ném 401: việc thu hồi (token hết hạn, bị dùng lại)
     * phải được lưu.
     */
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public Issued rotate(String rawToken, String userAgent, String ip) {
        if (rawToken == null || rawToken.isBlank()) throw expired();
        RefreshToken current = repo.findByTokenHash(hash(rawToken)).orElseThrow(RefreshTokenService::expired);
        User user = current.getUser();
        LocalDateTime now = LocalDateTime.now();

        if (current.getRevokedAt() != null) {
            boolean rotated = current.getReplacedById() != null;
            if (rotated && current.getRevokedAt().isAfter(now.minus(REUSE_GRACE)) && isUsable(current, user, now)) {
                return new Issued(create(user, current.getFamilyId(), userAgent, ip).raw(), user); // hai tab làm mới cùng lúc
            }
            if (rotated) {
                revokeEverything(user, now);
                log.warn("Refresh token bị dùng lại (họ {}) — đã thu hồi mọi phiên của người dùng {}", current.getFamilyId(), user.getId());
                auditLogService.log("REFRESH_TOKEN_REUSE", "User", user.getId().toString(),
                        "Refresh token đã xoay vòng bị dùng lại → thu hồi mọi phiên (ip=" + ip + ")", user.getUsername());
            }
            throw expired();
        }
        if (!isUsable(current, user, now)) {
            current.setRevokedAt(now);
            throw expired();
        }
        Created next = create(user, current.getFamilyId(), userAgent, ip);
        current.setRevokedAt(now);
        current.setReplacedById(next.entity().getId());
        return new Issued(next.raw(), user);
    }

    /** Đăng xuất phiên hiện tại (token không tồn tại / đã thu hồi: bỏ qua). */
    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        repo.findByTokenHash(hash(rawToken)).filter(t -> t.getRevokedAt() == null)
                .ifPresent(t -> t.setRevokedAt(LocalDateTime.now()));
    }

    /** Thu hồi mọi phiên của người dùng (đăng xuất mọi nơi, đổi/đặt lại mật khẩu). */
    @Transactional
    public void revokeAll(Long userId) {
        repo.revokeAllOfUser(userId, LocalDateTime.now());
    }

    /** Dọn token hết hạn / đã thu hồi quá 30 ngày (mỗi đêm 03:30). */
    @Scheduled(cron = "${app.auth.refresh-cleanup-cron:0 30 3 * * *}")
    @Transactional
    public int cleanup() {
        int deleted = repo.deleteStale(LocalDateTime.now().minusDays(30));
        if (deleted > 0) log.info("Đã dọn {} refresh token cũ", deleted);
        return deleted;
    }

    // ------------------------------------------------------------------ nội bộ

    private record Created(RefreshToken entity, String raw) {
    }

    private Created create(User user, String familyId, String userAgent, String ip) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        RefreshToken t = new RefreshToken();
        t.setUser(user);
        t.setTokenHash(hash(raw));
        t.setFamilyId(familyId);
        t.setTokenVersion(user.getTokenVersion());
        LocalDateTime now = LocalDateTime.now();
        t.setCreatedAt(now);
        t.setExpiresAt(now.plus(ttl));
        t.setUserAgent(userAgent == null ? null : userAgent.substring(0, Math.min(255, userAgent.length())));
        t.setIp(ip == null ? null : ip.substring(0, Math.min(64, ip.length())));
        return new Created(repo.save(t), raw);
    }

    private static boolean isUsable(RefreshToken t, User user, LocalDateTime now) {
        return t.getExpiresAt().isAfter(now) && user.isActive() && user.isActivated()
                && t.getTokenVersion() == user.getTokenVersion();
    }

    private void revokeEverything(User user, LocalDateTime now) {
        repo.revokeAllOfUser(user.getId(), now);
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepo.save(user);
    }

    static String hash(String raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ResponseStatusException expired() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, SESSION_EXPIRED);
    }
}
