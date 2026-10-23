package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.dto.request.LecturerAssignRequest;
import com.doanltmmt.Backend.dto.request.LecturerRevokeRequest;
import com.doanltmmt.Backend.dto.response.*;
import com.doanltmmt.Backend.entity.Lecturer;
import com.doanltmmt.Backend.entity.LecturerAssignment;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.entity.Workspace;
import com.doanltmmt.Backend.exception.ApiException;
import com.doanltmmt.Backend.exception.ErrorCodes;
import com.doanltmmt.Backend.repository.LecturerAssignmentRepository;
import com.doanltmmt.Backend.repository.LecturerRepository;
import com.doanltmmt.Backend.repository.UserRepository;
import com.doanltmmt.Backend.repository.WorkspaceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Phân công giảng viên cho đợt (chỉ khi đợt ở Bản nháp / Mở đề tài). */
@Service
@SuppressWarnings("null")
public class LecturerAssignmentService {

    private final LecturerAssignmentRepository repo;
    private final WorkspaceRepository workspaceRepo;
    private final LecturerRepository lecturerRepo;
    private final UserRepository userRepo;
    private final SecurityScopeService scope;
    private final AuditLogService auditLogService;

    public LecturerAssignmentService(LecturerAssignmentRepository repo, WorkspaceRepository workspaceRepo, LecturerRepository lecturerRepo, UserRepository userRepo, SecurityScopeService scope, AuditLogService auditLogService) {
        this.repo = repo;
        this.workspaceRepo = workspaceRepo;
        this.lecturerRepo = lecturerRepo;
        this.userRepo = userRepo;
        this.scope = scope;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<LecturerAssignmentDTO> list(Long workspaceId) {
        Workspace w = workspaceRepo.findById(workspaceId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đợt."));
        scope.requireDepartmentAccess(w.getDepartment().getId());
        return repo.findByWorkspace_IdOrderByIdDesc(workspaceId).stream().map(LecturerAssignmentDTO::from).toList();
    }

    @Transactional
    public LecturerAssignmentDTO assign(LecturerAssignRequest body) {
        Long workspaceId = body.workspaceId();
        Long lecturerUserId = body.lecturerId();
        String type = body.type();
        Workspace w = workspaceRepo.findById(workspaceId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đợt."));
        scope.requireDepartmentAccess(w.getDepartment().getId());
        requireWorkspaceConfigurable(w);

        User lecturerUser = userRepo.findById(lecturerUserId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng."));
        if (lecturerUser.getDepartment() != null && !lecturerUser.getDepartment().getId().equals(w.getDepartment().getId()) && !scope.hasRole("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giảng viên không thuộc khoa của đợt.");
        }

        Lecturer lecturer = lecturerRepo.findById(lecturerUserId).orElseGet(() -> {
            Lecturer l = new Lecturer();
            l.setUser(lecturerUser);
            l.setDepartment(lecturerUser.getDepartment());
            return lecturerRepo.save(l);
        });

        LecturerAssignment a = repo.findByWorkspace_IdAndLecturer_IdAndType(workspaceId, lecturer.getId(), type).orElseGet(() -> {
            LecturerAssignment la = new LecturerAssignment();
            la.setWorkspace(w);
            la.setLecturer(lecturer);
            la.setType(type);
            return la;
        });
        a.setQuotaMaxGroups(body.quotaMaxGroups());
        a.setActive(true);
        a.setReason(body.reason());
        LecturerAssignment saved = repo.save(a);
        auditLogService.log("ASSIGN_LECTURER", "LecturerAssignment", saved.getId().toString(), "workspace=" + workspaceId + ",lecturer=" + lecturerUserId + ",type=" + type);
        return LecturerAssignmentDTO.from(saved);
    }

    @Transactional
    public LecturerAssignmentDTO revoke(LecturerRevokeRequest body) {
        String reason = body.reason();
        LecturerAssignment a = repo.findById(body.assignmentId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phân công."));
        scope.requireDepartmentAccess(a.getWorkspace().getDepartment().getId());
        requireWorkspaceConfigurable(a.getWorkspace());
        a.setActive(false);
        a.setReason(reason);
        LecturerAssignment saved = repo.save(a);
        auditLogService.log("REVOKE_LECTURER_ASSIGNMENT", "LecturerAssignment", saved.getId().toString(), "reason=" + (reason == null ? "" : reason));
        return LecturerAssignmentDTO.from(saved);
    }

    private static void requireWorkspaceConfigurable(Workspace w) {
        if (!w.getStatus().allowsConfiguration()) {
            throw ApiException.badRequest(ErrorCodes.WORKSPACE_PHASE,
                    "Chỉ gán lớp / phân công giảng viên khi đợt ở trạng thái Bản nháp hoặc Mở đề tài");
        }
    }
}
