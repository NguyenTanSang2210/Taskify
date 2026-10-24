package com.doanltmmt.Backend.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * P3-14 — cookie mang refresh token: {@code HttpOnly} (JavaScript không đọc được), {@code SameSite=Strict},
 * {@code Path=/api/auth} (chỉ gửi kèm request xác thực), {@code Secure} ở production (cần HTTPS; trình duyệt vẫn
 * chấp nhận Secure với http://localhost). Endpoint dùng cookie còn kiểm tra {@code Origin} chống CSRF.
 */
@Component
public class RefreshCookie {

    public static final String NAME = "taskify_refresh";
    private static final String PATH = "/api/auth";

    private final boolean secure;
    private final Set<String> allowedOrigins;

    public RefreshCookie(@Value("${app.auth.refresh-cookie-secure:true}") boolean secure,
                         @Value("${app.cors.allowed-origins:http://localhost:5175}") String allowedOrigins) {
        this.secure = secure;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(",")).map(String::trim).filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
    }

    public void write(HttpServletResponse response, String rawToken, Duration maxAge) {
        response.addHeader(HttpHeaders.SET_COOKIE, build(rawToken, maxAge).toString());
    }

    public void clear(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, build("", Duration.ZERO).toString());
    }

    public Optional<String> read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return Optional.empty();
        return Arrays.stream(cookies).filter(c -> NAME.equals(c.getName())).map(Cookie::getValue)
                .filter(v -> v != null && !v.isBlank()).findFirst();
    }

    /**
     * Chặn request gửi cookie từ trang khác (CSRF): nếu có header Origin thì phải là origin được phép hoặc cùng
     * tên máy chủ với request (truy cập qua nginx / Vite proxy). Không có Origin (công cụ dòng lệnh) → cho qua,
     * vì trình duyệt luôn gửi Origin với POST.
     */
    public void requireSameOrigin(HttpServletRequest request) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin == null || origin.isBlank() || allowedOrigins.contains(origin)) return;
        try {
            String host = URI.create(origin).getHost();
            if (host != null && host.equalsIgnoreCase(request.getServerName())) return;
        } catch (IllegalArgumentException ignored) {
            // Origin không hợp lệ → từ chối bên dưới
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Yêu cầu không hợp lệ (nguồn gửi không được phép).");
    }

    private ResponseCookie build(String value, Duration maxAge) {
        return ResponseCookie.from(NAME, value).httpOnly(true).secure(secure).sameSite("Strict").path(PATH)
                .maxAge(maxAge).build();
    }
}
