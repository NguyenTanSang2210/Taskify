package com.doanltmmt.Backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Tạo và kiểm tra JWT.
 *
 * <p>Mọi token mang: {@code sub} (username), {@code iss=taskify}, {@code userId}, {@code scope}, {@code tv}
 * (tokenVersion của người dùng). Chỉ token có {@code scope=ACCESS} dùng được để gọi API; token
 * {@code OTP_PENDING} / {@code PWD_CHANGE} chỉ dùng cho đúng một bước của luồng đăng nhập.
 *
 * <p>Khóa ký được dựng MỘT lần khi khởi động và được kiểm tra: base64 hợp lệ, tối thiểu 32 byte, và ngoài
 * môi trường dev/test không được dùng các secret công khai trong repo.
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    public static final String ISSUER = "taskify";
    static final String CLAIM_SCOPE = "scope";
    static final String CLAIM_TOKEN_VERSION = "tv";
    static final String CLAIM_ROLE = "role";
    static final String CLAIM_USER_ID = "userId";
    private static final long CLOCK_SKEW_SECONDS = 30;

    /** Loại token. Chỉ {@link #ACCESS} được phép gọi API nghiệp vụ. */
    public enum Scope {
        /** Phiên đăng nhập đầy đủ. */
        ACCESS,
        /** Đã đúng mật khẩu, đang chờ nhập OTP — chỉ dùng cho /auth/otp/verify và /auth/otp/resend. */
        OTP_PENDING,
        /** Đã đúng mật khẩu tạm, bắt buộc đổi mật khẩu — chỉ dùng cho /auth/password/initial. */
        PWD_CHANGE
    }

    /** Nội dung đã được xác thực chữ ký + hạn + issuer + scope của một token. */
    public record TokenClaims(String username, Long userId, Scope scope, int tokenVersion) {
    }

    /**
     * Secret đã bị công khai trong repo (application-dev.properties, cấu hình Docker/CI cũ).
     * Ai cũng giả mạo được token nếu production dùng lại các giá trị này.
     */
    private static final Set<String> KNOWN_INSECURE_SECRETS = Set.of(
            "ZGV2LW9ubHktand0LXNlY3JldC1jaGFuZ2UtbWUtMjAyNg==",
            "ZGV2LXNlY3JldC1mb3ItdGVzdGluZy1wdXJwb3Nlcy1vbmx5LTIwMjY="
    );

    private final SecretKey key;
    private final long accessTtlMs;
    private final long preAuthTtlMs;

    public JwtTokenProvider(@Value("${jwt.secret:}") String secretBase64,
                            @Value("${jwt.expiration:86400000}") long accessTtlMs,
                            @Value("${jwt.preauth-expiration:600000}") long preAuthTtlMs,
                            Environment env) {
        this.key = buildKey(secretBase64, env);
        this.accessTtlMs = accessTtlMs;
        this.preAuthTtlMs = preAuthTtlMs;
    }

    private static SecretKey buildKey(String secretBase64, Environment env) {
        String secret = secretBase64 == null ? "" : secretBase64.trim();
        if (secret.isEmpty()) {
            throw new IllegalStateException(
                    "JWT_SECRET chưa được cấu hình. Tạo bằng: .\\scripts\\init-env.ps1 "
                            + "(hoặc đặt biến môi trường JWT_SECRET = chuỗi base64 của >= 32 byte ngẫu nhiên).");
        }

        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "JWT_SECRET không phải chuỗi base64 hợp lệ (đang là giá trị mẫu/placeholder?). "
                            + "Tạo secret thật bằng: .\\scripts\\init-env.ps1", ex);
        }
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET quá ngắn: cần tối thiểu 32 byte (256 bit) sau khi giải mã base64, hiện chỉ có "
                            + keyBytes.length + " byte.");
        }

        boolean devLike = env.acceptsProfiles(Profiles.of("dev", "test"));
        if (KNOWN_INSECURE_SECRETS.contains(secret)) {
            if (!devLike) {
                throw new IllegalStateException(
                        "JWT_SECRET đang dùng giá trị CÔNG KHAI trong repo — ai cũng giả mạo được token (kể cả ADMIN). "
                                + "Hãy tạo secret riêng: .\\scripts\\init-env.ps1");
            }
            log.warn("Đang dùng JWT secret dev công khai — chỉ chấp nhận cho môi trường phát triển/kiểm thử.");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // ------------------------------------------------------------------------------------------
    // Tạo token
    // ------------------------------------------------------------------------------------------

    /** Token phiên đầy đủ ({@code scope=ACCESS}). */
    public String generateAccessToken(String username, Long userId, String role, int tokenVersion) {
        return build(username, userId, Scope.ACCESS, accessTtlMs, tokenVersion, role);
    }

    /** Token tạm cho một bước của luồng đăng nhập ({@code OTP_PENDING} hoặc {@code PWD_CHANGE}). */
    public String generatePreAuthToken(String username, Long userId, int tokenVersion, Scope scope) {
        if (scope == Scope.ACCESS) {
            throw new IllegalArgumentException("Dùng generateAccessToken cho scope ACCESS");
        }
        return build(username, userId, scope, preAuthTtlMs, tokenVersion, null);
    }

    private String build(String username, Long userId, Scope scope, long ttlMs, int tokenVersion, String role) {
        Date now = new Date();
        var builder = Jwts.builder()
                .issuer(ISSUER)
                .subject(username)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMs))
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_SCOPE, scope.name())
                .claim(CLAIM_TOKEN_VERSION, tokenVersion);
        if (role != null) {
            builder.claim(CLAIM_ROLE, role);
        }
        return builder.signWith(key, Jwts.SIG.HS256).compact();
    }

    // ------------------------------------------------------------------------------------------
    // Kiểm tra token
    // ------------------------------------------------------------------------------------------

    /**
     * Xác thực chữ ký, hạn, issuer và yêu cầu đúng {@code scope}. Token sai, hết hạn, thiếu
     * {@code scope}/{@code tv} (token cũ trước khi có cơ chế này) hoặc sai loại → {@link Optional#empty()}.
     */
    public Optional<TokenClaims> parse(String token, Scope expected) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(ISSUER)
                    .clockSkewSeconds(CLOCK_SKEW_SECONDS)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String scopeName = claims.get(CLAIM_SCOPE, String.class);
            Integer tokenVersion = claims.get(CLAIM_TOKEN_VERSION, Integer.class);
            String username = claims.getSubject();
            if (scopeName == null || tokenVersion == null || username == null || username.isBlank()) {
                return Optional.empty();
            }
            Scope scope = Scope.valueOf(scopeName);
            if (scope != expected) {
                return Optional.empty();
            }
            Number userId = claims.get(CLAIM_USER_ID, Number.class);
            return Optional.of(new TokenClaims(username, userId == null ? null : userId.longValue(), scope, tokenVersion));
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public long getAccessTtlSeconds() {
        return accessTtlMs / 1000;
    }

    public long getPreAuthTtlSeconds() {
        return preAuthTtlMs / 1000;
    }

    // ------------------------------------------------------------------------------------------
    // API cũ — tạm giữ để các lớp gọi chưa đổi vẫn biên dịch; sẽ xóa ở P1-01
    // ------------------------------------------------------------------------------------------

    /** @deprecated dùng {@link #generateAccessToken}. */
    @Deprecated
    public String generateToken(String username, Map<String, Object> claims) {
        Object userId = claims == null ? null : claims.get(CLAIM_USER_ID);
        Object role = claims == null ? null : claims.get(CLAIM_ROLE);
        return build(username, userId instanceof Number n ? n.longValue() : null, Scope.ACCESS, accessTtlMs, 0,
                role == null ? null : role.toString());
    }

    /** @deprecated dùng {@link #parse}. */
    @Deprecated
    public String getUsername(String token) {
        return parse(token, Scope.ACCESS).map(TokenClaims::username)
                .orElseThrow(() -> new JwtException("Token không hợp lệ"));
    }

    /** @deprecated dùng {@link #parse}. */
    @Deprecated
    public boolean validate(String token) {
        return parse(token, Scope.ACCESS).isPresent();
    }
}
