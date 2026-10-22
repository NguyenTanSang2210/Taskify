package com.doanltmmt.Backend.security;

import com.doanltmmt.Backend.security.JwtTokenProvider.Scope;
import com.doanltmmt.Backend.security.JwtTokenProvider.TokenClaims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Kiểm thử thuần đơn vị (không cần Spring/DB) cho việc tạo/kiểm tra JWT và kiểm tra cấu hình secret. */
class JwtTokenProviderTest {

    private static final byte[] SECRET_BYTES = "unit-test-secret-key-0123456789-abcdefghij".getBytes(StandardCharsets.UTF_8);
    private static final String SECRET = Base64.getEncoder().encodeToString(SECRET_BYTES);
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET_BYTES);

    private static final String DEV_PUBLIC_SECRET = "ZGV2LW9ubHktand0LXNlY3JldC1jaGFuZ2UtbWUtMjAyNg==";
    private static final String CI_PUBLIC_SECRET = "ZGV2LXNlY3JldC1mb3ItdGVzdGluZy1wdXJwb3Nlcy1vbmx5LTIwMjY=";

    private static JwtTokenProvider provider() {
        return provider(SECRET, 86_400_000L, 600_000L);
    }

    private static JwtTokenProvider provider(String secret, long accessTtlMs, long preAuthTtlMs, String... profiles) {
        MockEnvironment env = new MockEnvironment();
        if (profiles.length > 0) {
            env.setActiveProfiles(profiles);
        }
        return new JwtTokenProvider(secret, accessTtlMs, preAuthTtlMs, env);
    }

    private static String randomSecret(int bytes) {
        byte[] b = new byte[bytes];
        new SecureRandom().nextBytes(b);
        return Base64.getEncoder().encodeToString(b);
    }

    // ---------------------------------------------------------------- token hợp lệ

    @Test
    void accessToken_roundTripKeepsAllClaims() {
        String token = provider().generateAccessToken("alice", 42L, "LECTURER", 7);

        Optional<TokenClaims> parsed = provider().parse(token, Scope.ACCESS);

        assertTrue(parsed.isPresent());
        assertEquals("alice", parsed.get().username());
        assertEquals(42L, parsed.get().userId());
        assertEquals(Scope.ACCESS, parsed.get().scope());
        assertEquals(7, parsed.get().tokenVersion());
    }

    @Test
    void preAuthToken_isAcceptedOnlyForItsOwnScope() {
        JwtTokenProvider p = provider();
        String otpToken = p.generatePreAuthToken("bob", 5L, 0, Scope.OTP_PENDING);

        assertTrue(p.parse(otpToken, Scope.OTP_PENDING).isPresent());
        assertTrue(p.parse(otpToken, Scope.ACCESS).isEmpty(), "token OTP_PENDING không được dùng như ACCESS");
        assertTrue(p.parse(otpToken, Scope.PWD_CHANGE).isEmpty());
    }

    @Test
    void accessToken_isRejectedWhenAPreAuthScopeIsExpected() {
        JwtTokenProvider p = provider();
        String access = p.generateAccessToken("carol", 9L, "STUDENT", 0);

        assertTrue(p.parse(access, Scope.OTP_PENDING).isEmpty());
        assertTrue(p.parse(access, Scope.PWD_CHANGE).isEmpty());
    }

    @Test
    void generatePreAuthToken_refusesAccessScope() {
        assertThrows(IllegalArgumentException.class,
                () -> provider().generatePreAuthToken("x", 1L, 0, Scope.ACCESS));
    }

    @Test
    void preAuthToken_usesShorterLifetimeThanAccessToken() {
        JwtTokenProvider p = provider(SECRET, 86_400_000L, 600_000L);
        assertEquals(86_400L, p.getAccessTtlSeconds());
        assertEquals(600L, p.getPreAuthTtlSeconds());
    }

    // ---------------------------------------------------------------- token không hợp lệ

    @Test
    void tamperedToken_isRejected() {
        JwtTokenProvider p = provider();
        String token = p.generateAccessToken("alice", 1L, "ADMIN", 0);
        // đổi ký tự ĐẦU của phần chữ ký (ký tự cuối của base64url chỉ mang 4 bit có nghĩa nên đổi nó đôi khi không làm đổi chữ ký)
        int sig = token.lastIndexOf('.') + 1;
        char c = token.charAt(sig);
        String tampered = token.substring(0, sig) + (c == 'A' ? 'B' : 'A') + token.substring(sig + 1);

        assertTrue(p.parse(tampered, Scope.ACCESS).isEmpty());
    }

    @Test
    void expiredToken_isRejected() {
        // TTL âm 2 phút: vượt cả độ lệch đồng hồ cho phép (30 giây)
        JwtTokenProvider expired = provider(SECRET, -120_000L, 600_000L);
        String token = expired.generateAccessToken("alice", 1L, "ADMIN", 0);

        assertTrue(expired.parse(token, Scope.ACCESS).isEmpty());
    }

    @Test
    void tokenSignedWithAnotherKey_isRejected() {
        JwtTokenProvider other = provider(randomSecret(48), 86_400_000L, 600_000L);
        String foreign = other.generateAccessToken("alice", 1L, "ADMIN", 0);

        assertTrue(provider().parse(foreign, Scope.ACCESS).isEmpty());
    }

    @Test
    void legacyTokenWithoutScopeAndTokenVersion_isRejected() {
        // token kiểu cũ: có chữ ký đúng nhưng không có scope/tv/iss
        String legacy = Jwts.builder()
                .subject("alice")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .claims(Map.of("role", "ADMIN", "userId", 1))
                .signWith(KEY, Jwts.SIG.HS256)
                .compact();

        assertTrue(provider().parse(legacy, Scope.ACCESS).isEmpty());
    }

    @Test
    void tokenWithForeignIssuer_isRejected() {
        String foreign = Jwts.builder()
                .issuer("someone-else")
                .subject("alice")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .claim("userId", 1)
                .claim("scope", "ACCESS")
                .claim("tv", 0)
                .signWith(KEY, Jwts.SIG.HS256)
                .compact();

        assertTrue(provider().parse(foreign, Scope.ACCESS).isEmpty());
    }

    @Test
    void unsignedAlgNoneToken_isRejected() {
        Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
        String header = enc.encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        String payload = enc.encodeToString(
                ("{\"iss\":\"taskify\",\"sub\":\"admin\",\"scope\":\"ACCESS\",\"tv\":0,\"userId\":1,\"exp\":"
                        + (System.currentTimeMillis() / 1000 + 3600) + "}").getBytes(StandardCharsets.UTF_8));

        assertTrue(provider().parse(header + "." + payload + ".", Scope.ACCESS).isEmpty());
    }

    @Test
    void garbageInput_isRejectedWithoutThrowing() {
        JwtTokenProvider p = provider();
        assertTrue(p.parse(null, Scope.ACCESS).isEmpty());
        assertTrue(p.parse("", Scope.ACCESS).isEmpty());
        assertTrue(p.parse("   ", Scope.ACCESS).isEmpty());
        assertTrue(p.parse("abc.def.ghi", Scope.ACCESS).isEmpty());
        assertTrue(p.parse("not-a-jwt", Scope.ACCESS).isEmpty());
    }

    // ---------------------------------------------------------------- kiểm tra cấu hình secret

    @Test
    void emptySecret_failsFastWithHelpfulMessage() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> provider("", 1000, 1000));
        assertTrue(ex.getMessage().contains("JWT_SECRET"));
    }

    @Test
    void placeholderSecret_thatIsNotBase64_failsFast() {
        // giá trị mẫu trong .env.docker.example — có dấu gạch dưới nên không phải base64 chuẩn
        assertThrows(IllegalStateException.class, () -> provider("CHANGE_ME_base64_secret", 1000, 1000));
    }

    @Test
    void secretShorterThan32Bytes_isRejected() {
        String tooShort = Base64.getEncoder().encodeToString("only-16-bytes!!!".getBytes(StandardCharsets.UTF_8));
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> provider(tooShort, 1000, 1000));
        assertTrue(ex.getMessage().contains("32"));
    }

    @Test
    void publicSecrets_areRejectedOutsideDevAndTest() {
        assertThrows(IllegalStateException.class, () -> provider(DEV_PUBLIC_SECRET, 1000, 1000));
        assertThrows(IllegalStateException.class, () -> provider(CI_PUBLIC_SECRET, 1000, 1000));
        assertThrows(IllegalStateException.class, () -> provider(DEV_PUBLIC_SECRET, 1000, 1000, "prod"));
    }

    @Test
    void publicSecrets_areAllowedInDevAndTest() {
        assertDoesNotThrow(() -> provider(DEV_PUBLIC_SECRET, 1000, 1000, "dev"));
        assertDoesNotThrow(() -> provider(CI_PUBLIC_SECRET, 1000, 1000, "test"));
    }

    @Test
    void freshRandomSecret_isAcceptedInProd() {
        assertDoesNotThrow(() -> provider(randomSecret(48), 1000, 1000, "prod"));
    }
}
