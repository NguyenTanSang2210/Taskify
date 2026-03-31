package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.entity.AcademicClass;
import com.doanltmmt.Backend.entity.Department;
import com.doanltmmt.Backend.repository.AcademicClassRepository;
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
@RequestMapping("/api/classes")
@CrossOrigin(origins = "http://localhost:5173")
@SuppressWarnings("null")
public class AcademicClassController {

    private final AcademicClassRepository repo;
    private final DepartmentRepository deptRepo;
    private final SecurityScopeService scope;
    private final AuditLogService auditLogService;

    public AcademicClassController(AcademicClassRepository repo, DepartmentRepository deptRepo, SecurityScopeService scope, AuditLogService auditLogService) {
        this.repo = repo;
        this.deptRepo = deptRepo;
        this.scope = scope;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_ADMIN')")
    public List<AcademicClass> list(@RequestParam(required = false) Long departmentId) {
        if (departmentId == null) {
            if (scope.hasRole("ADMIN")) return repo.findAll();
            Long currentDeptId = scope.requireCurrentUser().getDepartment() != null ? scope.requireCurrentUser().getDepartment().getId() : null;
            if (currentDeptId == null) return List.of();
            return repo.findByDepartment_IdOrderByIdDesc(currentDeptId);
        }
        scope.requireDepartmentAccess(departmentId);
        return repo.findByDepartment_IdOrderByIdDesc(departmentId);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_ADMIN')")
    public AcademicClass create(@RequestBody Map<String, Object> body) {
        Long departmentId = body.get("departmentId") != null ? Long.valueOf(String.valueOf(body.get("departmentId"))) : null;
        if (!scope.hasRole("ADMIN")) {
            departmentId = scope.requireCurrentUser().getDepartment() != null ? scope.requireCurrentUser().getDepartment().getId() : null;
        }
        scope.requireDepartmentAccess(departmentId);
        Department dept = deptRepo.findById(departmentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found"));
        String code = String.valueOf(body.getOrDefault("code", "")).trim();
        String name = String.valueOf(body.getOrDefault("name", "")).trim();
        if (code.isBlank() || name.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "code and name are required");
        AcademicClass c = new AcademicClass();
        c.setDepartment(dept);
        c.setCode(code);
        c.setName(name);
        if (body.get("active") != null) c.setActive(Boolean.parseBoolean(String.valueOf(body.get("active"))));
        AcademicClass saved = repo.save(c);
        auditLogService.log("CREATE_CLASS", "AcademicClass", saved.getId().toString(), "dept=" + dept.getId());
        return saved;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_ADMIN')")
    public AcademicClass update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        AcademicClass c = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found"));
        scope.requireDepartmentAccess(c.getDepartment().getId());
        String code = body.get("code") != null ? String.valueOf(body.get("code")).trim() : null;
        String name = body.get("name") != null ? String.valueOf(body.get("name")).trim() : null;
        if (code != null && !code.isBlank()) c.setCode(code);
        if (name != null && !name.isBlank()) c.setName(name);
        if (body.get("active") != null) c.setActive(Boolean.parseBoolean(String.valueOf(body.get("active"))));
        AcademicClass saved = repo.save(c);
        auditLogService.log("UPDATE_CLASS", "AcademicClass", saved.getId().toString(), "updated");
        return saved;
    }
}




