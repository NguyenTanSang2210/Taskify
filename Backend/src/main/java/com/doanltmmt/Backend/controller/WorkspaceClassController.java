package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.entity.AcademicClass;
import com.doanltmmt.Backend.entity.Workspace;
import com.doanltmmt.Backend.entity.WorkspaceClass;
import com.doanltmmt.Backend.repository.AcademicClassRepository;
import com.doanltmmt.Backend.repository.WorkspaceClassRepository;
import com.doanltmmt.Backend.repository.WorkspaceRepository;
import com.doanltmmt.Backend.service.AuditLogService;
import com.doanltmmt.Backend.service.SecurityScopeService;
import com.doanltmmt.Backend.util.Inputs;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workspace-classes")

@SuppressWarnings("null")
public class WorkspaceClassController {

    private final WorkspaceClassRepository repo;
    private final WorkspaceRepository workspaceRepo;
    private final AcademicClassRepository classRepo;
    private final SecurityScopeService scope;
    private final AuditLogService auditLogService;

    public WorkspaceClassController(WorkspaceClassRepository repo, WorkspaceRepository workspaceRepo, AcademicClassRepository classRepo, SecurityScopeService scope, AuditLogService auditLogService) {
        this.repo = repo;
        this.workspaceRepo = workspaceRepo;
        this.classRepo = classRepo;
        this.scope = scope;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_ADMIN')")
    public List<WorkspaceClass> list(@RequestParam Long workspaceId) {
        Workspace w = workspaceRepo.findById(workspaceId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));
        scope.requireDepartmentAccess(w.getDepartment().getId());
        return repo.findByWorkspace_IdOrderByIdDesc(workspaceId);
    }

    @PostMapping("/assign")
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_ADMIN')")
    public WorkspaceClass assign(@RequestBody Map<String, Object> body) {
        Long workspaceId = Inputs.requiredId(body.get("workspaceId"), "workspaceId");
        Long classId = Inputs.requiredId(body.get("classId"), "classId");
        Workspace w = workspaceRepo.findById(workspaceId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));
        scope.requireDepartmentAccess(w.getDepartment().getId());
        requireWorkspaceConfigurable(w);
        AcademicClass c = classRepo.findById(classId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found"));
        if (!c.getDepartment().getId().equals(w.getDepartment().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Class is not in workspace department");
        }
        WorkspaceClass link = repo.findByWorkspace_IdAndAcademicClass_Id(workspaceId, classId).orElseGet(() -> {
            WorkspaceClass wc = new WorkspaceClass();
            wc.setWorkspace(w);
            wc.setAcademicClass(c);
            wc.setActive(true);
            return wc;
        });
        link.setActive(true);
        WorkspaceClass saved = repo.save(link);
        auditLogService.log("ASSIGN_CLASS_TO_WORKSPACE", "WorkspaceClass", saved.getId().toString(), "workspace=" + workspaceId + ",class=" + classId);
        return saved;
    }

    @PostMapping("/unassign")
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_ADMIN')")
    public WorkspaceClass unassign(@RequestBody Map<String, Object> body) {
        Long workspaceId = Inputs.requiredId(body.get("workspaceId"), "workspaceId");
        Long classId = Inputs.requiredId(body.get("classId"), "classId");
        Workspace w = workspaceRepo.findById(workspaceId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));
        scope.requireDepartmentAccess(w.getDepartment().getId());
        requireWorkspaceConfigurable(w);
        WorkspaceClass link = repo.findByWorkspace_IdAndAcademicClass_Id(workspaceId, classId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Link not found"));
        link.setActive(false);
        WorkspaceClass saved = repo.save(link);
        auditLogService.log("UNASSIGN_CLASS_FROM_WORKSPACE", "WorkspaceClass", saved.getId().toString(), "workspace=" + workspaceId + ",class=" + classId);
        return saved;
    }

    private static void requireWorkspaceConfigurable(Workspace w) {
        if (!w.getStatus().allowsConfiguration()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Chỉ gán lớp / phân công giảng viên khi đợt ở trạng thái Bản nháp hoặc Mở đề tài");
        }
    }
}



