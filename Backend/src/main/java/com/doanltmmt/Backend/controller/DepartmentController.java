package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.dto.request.DepartmentRequest;
import com.doanltmmt.Backend.dto.request.OnCreate;
import com.doanltmmt.Backend.entity.Department;
import com.doanltmmt.Backend.repository.DepartmentRepository;
import com.doanltmmt.Backend.service.AuditLogService;
import com.doanltmmt.Backend.service.SecurityScopeService;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/departments")

@SuppressWarnings("null")
public class DepartmentController {

    private final DepartmentRepository repo;
    private final SecurityScopeService scope;
    private final AuditLogService auditLogService;

    public DepartmentController(DepartmentRepository repo, SecurityScopeService scope, AuditLogService auditLogService) {
        this.repo = repo;
        this.scope = scope;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_ADMIN')")
    public List<Department> list() {
        if (scope.hasRole("ADMIN")) return repo.findAll();
        Long deptId = scope.requireCurrentUser().getDepartment() != null ? scope.requireCurrentUser().getDepartment().getId() : null;
        if (deptId == null) return List.of();
        return repo.findById(deptId).map(List::of).orElse(List.of());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Department create(@Validated({OnCreate.class, Default.class}) @RequestBody DepartmentRequest body) {
        Department d = new Department();
        d.setCode(body.code());
        d.setName(body.name());
        if (body.active() != null) d.setActive(body.active());
        Department saved = repo.save(d);
        auditLogService.log("CREATE_DEPARTMENT", "Department", saved.getId().toString(), "code=" + saved.getCode());
        return saved;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Department update(@PathVariable Long id, @Valid @RequestBody DepartmentRequest body) {
        Department d = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khoa."));
        if (body.code() != null) d.setCode(body.code());
        if (body.name() != null) d.setName(body.name());
        if (body.active() != null) d.setActive(body.active());
        Department saved = repo.save(d);
        auditLogService.log("UPDATE_DEPARTMENT", "Department", saved.getId().toString(), "updated");
        return saved;
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Department updateStatus(@PathVariable Long id, @RequestParam boolean active) {
        Department d = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khoa."));
        d.setActive(active);
        Department saved = repo.save(d);
        auditLogService.log(active ? "ENABLE_DEPARTMENT" : "DISABLE_DEPARTMENT", "Department", saved.getId().toString(), "active=" + active);
        return saved;
    }
}




