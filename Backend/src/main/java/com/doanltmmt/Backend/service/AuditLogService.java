package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.entity.AuditLog;
import com.doanltmmt.Backend.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditLogService {

    private final AuditLogRepository repo;

    public AuditLogService(AuditLogRepository repo) {
        this.repo = repo;
    }

    /** Ghi nhật ký; người thực hiện lấy từ người dùng đang đăng nhập (hoặc SYSTEM nếu không có). */
    public void log(String action, String entityName, String entityId, String details) {
        String actor = "SYSTEM";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            actor = auth.getName();
        }
        log(action, entityName, entityId, details, actor);
    }

    /**
     * Ghi nhật ký với người thực hiện tường minh — dùng cho luồng đăng nhập/OTP/kích hoạt, nơi chưa có
     * SecurityContext (chính là lúc cần ghi lại ai đang thử đăng nhập).
     */
    public void log(String action, String entityName, String entityId, String details, String actor) {
        AuditLog entry = new AuditLog(action, entityName, entityId, actor == null || actor.isBlank() ? "SYSTEM" : actor, details);
        // P4-02: IP + trình duyệt của request hiện tại (tác vụ nền không có request → để trống)
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest request = attrs.getRequest();
            entry.setIpAddress(request.getRemoteAddr());
            entry.setUserAgent(truncate(request.getHeader(HttpHeaders.USER_AGENT), 255));
        }
        repo.save(entry);
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
