package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.service.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> getAdminStats() {
        return service.getAdminStats();
    }

    @GetMapping("/lecturer")
    @PreAuthorize("hasRole('LECTURER')")
    public Map<String, Object> getLecturerStats() {
        return service.getLecturerStats();
    }

    @GetMapping("/department-admin")
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_ADMIN')")
    public Map<String, Object> getDepartmentAdminStats() {
        return service.getDepartmentAdminStats();
    }
}
