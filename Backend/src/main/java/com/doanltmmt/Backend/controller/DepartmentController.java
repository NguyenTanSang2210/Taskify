package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.entity.Department;
import com.doanltmmt.Backend.repository.DepartmentRepository;
import com.doanltmmt.Backend.service.AuditLogService;
import com.doanltmmt.Backend.service.SecurityScopeService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/departments")
@CrossOrigin(origins = "http://localhost:5173")
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
    public Department create(@RequestBody Map<String, Object> body) {
        String code = String.valueOf(body.getOrDefault("code", "")).trim();
        String name = String.valueOf(body.getOrDefault("name", "")).trim();
        if (code.isBlank() || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "code and name are required");
        }
        Department d = new Department();
        d.setCode(code);
        d.setName(name);
        if (body.get("active") != null) {
            d.setActive(Boolean.parseBoolean(String.valueOf(body.get("active"))));
        }
        Department saved = repo.save(d);
        auditLogService.log("CREATE_DEPARTMENT", "Department", saved.getId().toString(), "code=" + saved.getCode());
        return saved;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Department update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Department d = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found"));
        String code = body.get("code") != null ? String.valueOf(body.get("code")).trim() : null;
        String name = body.get("name") != null ? String.valueOf(body.get("name")).trim() : null;
        if (code != null && !code.isBlank()) d.setCode(code);
        if (name != null && !name.isBlank()) d.setName(name);
        if (body.get("active") != null) d.setActive(Boolean.parseBoolean(String.valueOf(body.get("active"))));
        Department saved = repo.save(d);
        auditLogService.log("UPDATE_DEPARTMENT", "Department", saved.getId().toString(), "updated");
        return saved;
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Department updateStatus(@PathVariable Long id, @RequestParam boolean active) {
        Department d = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found"));
        d.setActive(active);
        Department saved = repo.save(d);
        auditLogService.log(active ? "ENABLE_DEPARTMENT" : "DISABLE_DEPARTMENT", "Department", saved.getId().toString(), "active=" + active);
        return saved;
    }
}




