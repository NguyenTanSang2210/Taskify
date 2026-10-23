package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.dto.request.DepartmentRequest;
import com.doanltmmt.Backend.entity.Department;
import com.doanltmmt.Backend.repository.DepartmentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Khoa. */
@Service
@SuppressWarnings("null")
public class DepartmentService {

    private final DepartmentRepository repo;
    private final SecurityScopeService scope;
    private final AuditLogService auditLogService;

    public DepartmentService(DepartmentRepository repo, SecurityScopeService scope, AuditLogService auditLogService) {
        this.repo = repo;
        this.scope = scope;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<Department> list() {
        if (scope.hasRole("ADMIN")) return repo.findAll();
        Long deptId = scope.requireCurrentUser().getDepartment() != null ? scope.requireCurrentUser().getDepartment().getId() : null;
        if (deptId == null) return List.of();
        return repo.findById(deptId).map(List::of).orElse(List.of());
    }

    @Transactional
    public Department create(DepartmentRequest body) {
        Department d = new Department();
        d.setCode(body.code());
        d.setName(body.name());
        if (body.active() != null) d.setActive(body.active());
        Department saved = repo.save(d);
        auditLogService.log("CREATE_DEPARTMENT", "Department", saved.getId().toString(), "code=" + saved.getCode());
        return saved;
    }

    @Transactional
    public Department update(Long id, DepartmentRequest body) {
        Department d = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khoa."));
        if (body.code() != null) d.setCode(body.code());
        if (body.name() != null) d.setName(body.name());
        if (body.active() != null) d.setActive(body.active());
        Department saved = repo.save(d);
        auditLogService.log("UPDATE_DEPARTMENT", "Department", saved.getId().toString(), "updated");
        return saved;
    }

    @Transactional
    public Department updateStatus(Long id, boolean active) {
        Department d = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khoa."));
        d.setActive(active);
        Department saved = repo.save(d);
        auditLogService.log(active ? "ENABLE_DEPARTMENT" : "DISABLE_DEPARTMENT", "Department", saved.getId().toString(), "active=" + active);
        return saved;
    }
}
