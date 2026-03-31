package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.entity.Lecturer;
import com.doanltmmt.Backend.entity.LecturerAssignment;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.entity.Workspace;
import com.doanltmmt.Backend.repository.LecturerAssignmentRepository;
import com.doanltmmt.Backend.repository.LecturerRepository;
import com.doanltmmt.Backend.repository.UserRepository;
import com.doanltmmt.Backend.repository.WorkspaceRepository;
import com.doanltmmt.Backend.service.AuditLogService;
import com.doanltmmt.Backend.service.SecurityScopeService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assignments")
@CrossOrigin(origins = "http://localhost:5173")
@SuppressWarnings("null")
public class LecturerAssignmentController {

    private final LecturerAssignmentRepository repo;
    private final WorkspaceRepository workspaceRepo;
    private final LecturerRepository lecturerRepo;
    private final UserRepository userRepo;
    private final SecurityScopeService scope;
    private final AuditLogService auditLogService;

    public LecturerAssignmentController(LecturerAssignmentRepository repo, WorkspaceRepository workspaceRepo, LecturerRepository lecturerRepo, UserRepository userRepo, SecurityScopeService scope, AuditLogService auditLogService) {
        this.repo = repo;
        this.workspaceRepo = workspaceRepo;
        this.lecturerRepo = lecturerRepo;
        this.userRepo = userRepo;
        this.scope = scope;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_ADMIN')")
    public List<LecturerAssignment> list(@RequestParam Long workspaceId) {
        Workspace w = workspaceRepo.findById(workspaceId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));
        scope.requireDepartmentAccess(w.getDepartment().getId());
        return repo.findByWorkspace_IdOrderByIdDesc(workspaceId);
    }

    @PostMapping("/assign")
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_ADMIN')")
    public LecturerAssignment assign(@RequestBody Map<String, Object> body) {
        Long workspaceId = Long.valueOf(String.valueOf(body.get("workspaceId")));
        Long lecturerUserId = Long.valueOf(String.valueOf(body.get("lecturerId")));
        String type = String.valueOf(body.getOrDefault("type", "MAIN")).trim().toUpperCase();
        int quotaMaxGroups = body.get("quotaMaxGroups") != null ? Integer.parseInt(String.valueOf(body.get("quotaMaxGroups"))) : 0;
        String reason = body.get("reason") != null ? String.valueOf(body.get("reason")).trim() : null;
        if (!type.equals("MAIN") && !type.equals("ASSISTANT")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid type");
        }
        Workspace w = workspaceRepo.findById(workspaceId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));
        scope.requireDepartmentAccess(w.getDepartment().getId());
        requireWorkspaceConfigurable(w);

        User lecturerUser = userRepo.findById(lecturerUserId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (lecturerUser.getDepartment() != null && !lecturerUser.getDepartment().getId().equals(w.getDepartment().getId()) && !scope.hasRole("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecturer is not in workspace department");
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
        a.setQuotaMaxGroups(Math.max(0, quotaMaxGroups));
        a.setActive(true);
        a.setReason(reason);
        LecturerAssignment saved = repo.save(a);
        auditLogService.log("ASSIGN_LECTURER", "LecturerAssignment", saved.getId().toString(), "workspace=" + workspaceId + ",lecturer=" + lecturerUserId + ",type=" + type);
        return saved;
    }

    @PostMapping("/revoke")
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_ADMIN')")
    public LecturerAssignment revoke(@RequestBody Map<String, Object> body) {
        Long assignmentId = Long.valueOf(String.valueOf(body.get("assignmentId")));
        String reason = body.get("reason") != null ? String.valueOf(body.get("reason")).trim() : null;
        LecturerAssignment a = repo.findById(assignmentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));
        scope.requireDepartmentAccess(a.getWorkspace().getDepartment().getId());
        requireWorkspaceConfigurable(a.getWorkspace());
        a.setActive(false);
        a.setReason(reason);
        LecturerAssignment saved = repo.save(a);
        auditLogService.log("REVOKE_LECTURER_ASSIGNMENT", "LecturerAssignment", saved.getId().toString(), "reason=" + (reason == null ? "" : reason));
        return saved;
    }

    private static void requireWorkspaceConfigurable(Workspace w) {
        String status = w.getStatus() == null ? "DRAFT" : w.getStatus().trim().toUpperCase();
        if (status.equals("OPEN")) status = "OPEN_TOPIC";
        if (!status.equals("DRAFT") && !status.equals("OPEN_TOPIC")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workspace is not configurable in current status");
        }
    }
}

