package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.entity.AuditLog;
import com.doanltmmt.Backend.repository.AuditLogRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Nhật ký hệ thống — chứa điểm số, lý do từ chối, tên đăng nhập bị dò...: CHỈ quản trị viên hệ thống được xem
 * (trước đây mọi người dùng đã đăng nhập đều đọc được toàn bộ).
 */
@RestController
@RequestMapping("/api/audit")
public class AuditLogController {

    private final AuditLogRepository repo;

    public AuditLogController(AuditLogRepository repo) {
        this.repo = repo;
    }

    /** 200 bản ghi mới nhất. (Phân trang + lọc: P3-06.) */
    @GetMapping("/recent")
    @PreAuthorize("hasRole('ADMIN')")
    public List<AuditLog> recent() {
        return repo.findTop200ByOrderByTimestampDesc();
    }
}
