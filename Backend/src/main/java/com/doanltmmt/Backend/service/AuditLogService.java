package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.entity.AuditLog;
import com.doanltmmt.Backend.repository.AuditLogRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

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
        repo.save(new AuditLog(action, entityName, entityId, actor == null || actor.isBlank() ? "SYSTEM" : actor, details));
    }
}
