package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.dto.response.PageResponse;
import com.doanltmmt.Backend.entity.AuditLog;
import com.doanltmmt.Backend.service.AuditLogQueryService;
import com.doanltmmt.Backend.service.AuditLogQueryService.Filter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Nhật ký hệ thống — chứa điểm số, lý do từ chối, tên đăng nhập bị dò, IP...: CHỈ quản trị viên hệ thống được xem.
 * Bộ lọc: {@code actor}, {@code action}, {@code category} (GRADE | AUTH | ACCOUNT), {@code entity}, {@code entityId},
 * {@code from}–{@code to} (yyyy-MM-ddTHH:mm, {@code to} không bao gồm).
 */
@RestController
@RequestMapping("/api/audit")
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final AuditLogQueryService service;

    public AuditLogController(AuditLogQueryService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<AuditLog> search(@RequestParam(required = false) String actor,
                                         @RequestParam(required = false) String action,
                                         @RequestParam(required = false) String category,
                                         @RequestParam(required = false) String entity,
                                         @RequestParam(required = false) String entityId,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "50") int size) {
        return service.search(new Filter(actor, action, category, entity, entityId, from, to), page, size);
    }

    /** Giá trị cho ô chọn: {@code actions}, {@code entities}, {@code categories}. */
    @GetMapping("/options")
    public Map<String, List<String>> options() {
        return service.options();
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String actor,
                                         @RequestParam(required = false) String action,
                                         @RequestParam(required = false) String category,
                                         @RequestParam(required = false) String entity,
                                         @RequestParam(required = false) String entityId,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        byte[] body = service.exportCsv(new Filter(actor, action, category, entity, entityId, from, to));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("nhat-ky-he-thong.csv").build().toString())
                .contentType(CSV)
                .body(body);
    }

    /** Giữ cho client cũ: trang đầu (50 bản ghi mới nhất). */
    @GetMapping("/recent")
    public PageResponse<AuditLog> recent() {
        return service.search(new Filter(null, null, null, null, null, null, null), 0, 50);
    }
}
