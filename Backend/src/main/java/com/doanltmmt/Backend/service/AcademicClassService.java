package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.dto.request.AcademicClassRequest;
import com.doanltmmt.Backend.entity.AcademicClass;
import com.doanltmmt.Backend.entity.Department;
import com.doanltmmt.Backend.repository.AcademicClassRepository;
import com.doanltmmt.Backend.repository.DepartmentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Lớp sinh hoạt thuộc khoa. */
@Service
@SuppressWarnings("null")
public class AcademicClassService {

    private final AcademicClassRepository repo;
    private final DepartmentRepository deptRepo;
    private final SecurityScopeService scope;
    private final AuditLogService auditLogService;

    public AcademicClassService(AcademicClassRepository repo, DepartmentRepository deptRepo, SecurityScopeService scope, AuditLogService auditLogService) {
        this.repo = repo;
        this.deptRepo = deptRepo;
        this.scope = scope;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<AcademicClass> list(Long departmentId) {
        if (departmentId == null) {
            if (scope.hasRole("ADMIN")) return repo.findAll();
            Long currentDeptId = scope.requireCurrentUser().getDepartment() != null ? scope.requireCurrentUser().getDepartment().getId() : null;
            if (currentDeptId == null) return List.of();
            return repo.findByDepartment_IdOrderByIdDesc(currentDeptId);
        }
        scope.requireDepartmentAccess(departmentId);
        return repo.findByDepartment_IdOrderByIdDesc(departmentId);
    }

    @Transactional
    public AcademicClass create(AcademicClassRequest body) {
        Long departmentId = body.departmentId();
        if (!scope.hasRole("ADMIN")) {
            departmentId = scope.requireCurrentUser().getDepartment() != null ? scope.requireCurrentUser().getDepartment().getId() : null;
        }
        scope.requireDepartmentAccess(departmentId);
        Department dept = deptRepo.findById(departmentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khoa."));
        AcademicClass c = new AcademicClass();
        c.setDepartment(dept);
        c.setCode(body.code());
        c.setName(body.name());
        if (body.active() != null) c.setActive(body.active());
        AcademicClass saved = repo.save(c);
        auditLogService.log("CREATE_CLASS", "AcademicClass", saved.getId().toString(), "dept=" + dept.getId());
        return saved;
    }

    @Transactional
    public AcademicClass update(Long id, AcademicClassRequest body) {
        AcademicClass c = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy lớp."));
        scope.requireDepartmentAccess(c.getDepartment().getId());
        if (body.code() != null) c.setCode(body.code());
        if (body.name() != null) c.setName(body.name());
        if (body.active() != null) c.setActive(body.active());
        AcademicClass saved = repo.save(c);
        auditLogService.log("UPDATE_CLASS", "AcademicClass", saved.getId().toString(), "updated");
        return saved;
    }
}
