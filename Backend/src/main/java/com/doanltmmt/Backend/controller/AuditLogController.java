package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.dto.response.PageResponse;
import com.doanltmmt.Backend.entity.AuditLog;
import com.doanltmmt.Backend.service.AuditLogQueryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * Nhật ký hệ thống — chứa điểm số, lý do từ chối, tên đăng nhập bị dò...: CHỈ quản trị viên hệ thống được xem
 * (trước đây mọi người dùng đã đăng nhập đều đọc được toàn bộ).
 */
@RestController
@RequestMapping("/api/audit")
public class AuditLogController {

    private final AuditLogQueryService service;

    public AuditLogController(AuditLogQueryService service) {
        this.service = service;
    }

    /** Lọc theo {@code actor} (tên đăng nhập), {@code action}, {@code from}–{@code to} (yyyy-MM-ddTHH:mm); mới nhất trước. */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<AuditLog> search(@RequestParam(required = false) String actor,
                                         @RequestParam(required = false) String action,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "50") int size) {
        return service.search(actor, action, from, to, page, size);
    }

    /** Giữ cho client cũ: trang đầu (50 bản ghi mới nhất). */
    @GetMapping("/recent")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<AuditLog> recent() {
        return service.search(null, null, null, null, 0, 50);
    }
}
