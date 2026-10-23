package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.service.ExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/** Xuất danh sách đăng ký + điểm ra Excel (quyền xem {@link ExportService}). */
@RestController
@RequestMapping("/api/export")
public class ExportController {

    private static final MediaType XLSX = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ExportService service;

    public ExportController(ExportService service) {
        this.service = service;
    }

    @GetMapping("/excel")
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_ADMIN','LECTURER')")
    public ResponseEntity<byte[]> exportToExcel(@RequestParam(required = false) Long topicId) throws IOException {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=registrations.xlsx")
                .contentType(XLSX)
                .body(service.registrationsWorkbook(topicId));
    }
}
