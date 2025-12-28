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

    public void log(String action, String entityName, String entityId, String details) {
        String actor = "SYSTEM";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            actor = auth.getName();
        }
        
        AuditLog log = new AuditLog(action, entityName, entityId, actor, details);
        repo.save(log);
    }
}
