package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.dto.request.WorkspaceClassRequest;
import com.doanltmmt.Backend.entity.AcademicClass;
import com.doanltmmt.Backend.entity.Workspace;
import com.doanltmmt.Backend.entity.WorkspaceClass;
import com.doanltmmt.Backend.exception.ApiException;
import com.doanltmmt.Backend.exception.ErrorCodes;
import com.doanltmmt.Backend.repository.AcademicClassRepository;
import com.doanltmmt.Backend.repository.WorkspaceClassRepository;
import com.doanltmmt.Backend.repository.WorkspaceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Gán lớp vào đợt (chỉ khi đợt ở Bản nháp / Mở đề tài). */
@Service
@SuppressWarnings("null")
public class WorkspaceClassService {

    private final WorkspaceClassRepository repo;
    private final WorkspaceRepository workspaceRepo;
    private final AcademicClassRepository classRepo;
    private final SecurityScopeService scope;
    private final AuditLogService auditLogService;

    public WorkspaceClassService(WorkspaceClassRepository repo, WorkspaceRepository workspaceRepo, AcademicClassRepository classRepo, SecurityScopeService scope, AuditLogService auditLogService) {
        this.repo = repo;
        this.workspaceRepo = workspaceRepo;
        this.classRepo = classRepo;
        this.scope = scope;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<WorkspaceClass> list(Long workspaceId) {
        Workspace w = workspaceRepo.findById(workspaceId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đợt."));
        scope.requireDepartmentAccess(w.getDepartment().getId());
        return repo.findByWorkspace_IdOrderByIdDesc(workspaceId);
    }

    @Transactional
    public WorkspaceClass assign(WorkspaceClassRequest body) {
        Long workspaceId = body.workspaceId();
        Long classId = body.classId();
        Workspace w = workspaceRepo.findById(workspaceId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đợt."));
        scope.requireDepartmentAccess(w.getDepartment().getId());
        requireWorkspaceConfigurable(w);
        AcademicClass c = classRepo.findById(classId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy lớp."));
        if (!c.getDepartment().getId().equals(w.getDepartment().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lớp không thuộc khoa của đợt.");
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

    @Transactional
    public WorkspaceClass unassign(WorkspaceClassRequest body) {
        Long workspaceId = body.workspaceId();
        Long classId = body.classId();
        Workspace w = workspaceRepo.findById(workspaceId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đợt."));
        scope.requireDepartmentAccess(w.getDepartment().getId());
        requireWorkspaceConfigurable(w);
        WorkspaceClass link = repo.findByWorkspace_IdAndAcademicClass_Id(workspaceId, classId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lớp chưa được gán cho đợt này."));
        link.setActive(false);
        WorkspaceClass saved = repo.save(link);
        auditLogService.log("UNASSIGN_CLASS_FROM_WORKSPACE", "WorkspaceClass", saved.getId().toString(), "workspace=" + workspaceId + ",class=" + classId);
        return saved;
    }

    private static void requireWorkspaceConfigurable(Workspace w) {
        if (!w.getStatus().allowsConfiguration()) {
            throw ApiException.badRequest(ErrorCodes.WORKSPACE_PHASE,
                    "Chỉ gán lớp / phân công giảng viên khi đợt ở trạng thái Bản nháp hoặc Mở đề tài");
        }
    }
}
