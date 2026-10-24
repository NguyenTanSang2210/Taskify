package com.doanltmmt.Backend.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.function.Supplier;

/**
 * P4-08 — quyền đọc {@code /actuator/prometheus}: header {@code X-Metrics-Token} phải đúng {@code app.metrics.token}
 * (so sánh thời gian hằng). Tạo trong {@code SecurityConfig}. Không cấu hình token = không ai đọc được. Prometheus gửi header này (xem monitoring/).
 */
public class MetricsTokenAuthorization implements AuthorizationManager<RequestAuthorizationContext> {

    public static final String HEADER = "X-Metrics-Token";

    private final byte[] token;

    public MetricsTokenAuthorization(String token) {
        this.token = token == null || token.isBlank() ? null : token.trim().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        HttpServletRequest request = context.getRequest();
        String given = request.getHeader(HEADER);
        boolean ok = token != null && given != null && MessageDigest.isEqual(token, given.trim().getBytes(StandardCharsets.UTF_8));
        return new AuthorizationDecision(ok);
    }
}
