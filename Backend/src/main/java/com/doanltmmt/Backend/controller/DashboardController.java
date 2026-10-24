package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.dto.response.DashboardDTO;
import com.doanltmmt.Backend.service.DashboardOverviewService;
import com.doanltmmt.Backend.service.DashboardService;
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
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private static final MediaType XLSX = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final DashboardService service;
    private final DashboardOverviewService overview;

    public DashboardController(DashboardService service, DashboardOverviewService overview) {
        this.service = service;
        this.overview = overview;
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> getAdminStats() {
        return service.getAdminStats();
    }

    @GetMapping("/lecturer")
    @PreAuthorize("hasRole('LECTURER')")
    public DashboardDTO.Lecturer getLecturerStats() {
        return service.getLecturerStats();
    }

    @GetMapping("/student")
    @PreAuthorize("hasRole('STUDENT')")
    public DashboardDTO.Student getStudentStats() {
        return service.getStudentStats();
    }

    @GetMapping("/department-admin")
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_ADMIN')")
    public Map<String, Object> getDepartmentAdminStats() {
        return service.getDepartmentAdminStats();
    }

    /** P4-01 — thống kê nâng cao; ADMIN chọn khoa (bỏ trống = toàn hệ thống), quản trị khoa luôn là khoa mình. */
    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_ADMIN')")
    public DashboardDTO.Overview getOverview(@RequestParam(required = false) Long departmentId,
                                             @RequestParam(required = false) Long workspaceId) {
        return overview.overview(departmentId, workspaceId);
    }

    @GetMapping("/overview/export")
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_ADMIN')")
    public ResponseEntity<byte[]> exportOverview(@RequestParam(required = false) Long departmentId,
                                                 @RequestParam(required = false) Long workspaceId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("thong-ke.xlsx", StandardCharsets.UTF_8).build().toString())
                .contentType(XLSX)
                .body(overview.exportExcel(departmentId, workspaceId));
    }
}
