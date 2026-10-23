package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.entity.AuditLog;
import com.doanltmmt.Backend.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Tra cứu nhật ký hệ thống (chỉ ADMIN). */
@Service
public class AuditLogQueryService {

    private final AuditLogRepository repo;

    public AuditLogQueryService(AuditLogRepository repo) {
        this.repo = repo;
    }

    /** 200 bản ghi mới nhất. (Phân trang + lọc: P3-06.) */
    @Transactional(readOnly = true)
    public List<AuditLog> recent() {
        return repo.findTop200ByOrderByTimestampDesc();
    }
}
