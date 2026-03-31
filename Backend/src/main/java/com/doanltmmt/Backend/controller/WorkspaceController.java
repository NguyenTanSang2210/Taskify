package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.entity.Department;
import com.doanltmmt.Backend.entity.Workspace;
import com.doanltmmt.Backend.repository.DepartmentRepository;
import com.doanltmmt.Backend.repository.TopicRepository;
import com.doanltmmt.Backend.repository.WorkspaceClassRepository;
import com.doanltmmt.Backend.repository.WorkspaceRepository;
import com.doanltmmt.Backend.service.AuditLogService;
import com.doanltmmt.Backend.service.SecurityScopeService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workspaces")
@CrossOrigin(origins = "http://localhost:5173")
@SuppressWarnings("null")
public class WorkspaceController {

    private final WorkspaceRepository repo;
    private final DepartmentRepository deptRepo;
    private final TopicRepository topicRepo;
    private final WorkspaceClassRepository workspaceClassRepo;
    private final SecurityScopeService scope;
    private final AuditLogService auditLogService;

    public WorkspaceController(WorkspaceRepository repo, DepartmentRepository deptRepo, TopicRepository topicRepo, WorkspaceClassRepository workspaceClassRepo, SecurityScopeService scope, AuditLogService auditLogService) {
        this.repo = repo;
        this.deptRepo = deptRepo;
        this.topicRepo = topicRepo;
        this.workspaceClassRepo = workspaceClassRepo;
        this.scope = scope;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_ADMIN')")
    public List<Workspace> list(@RequestParam(required = false) Long departmentId) {
        if (departmentId == null) {
            if (scope.hasRole("ADMIN")) return repo.findAll();
            Long currentDeptId = scope.requireCurrentUser().getDepartment() != null ? scope.requireCurrentUser().getDepartment().getId() : null;
            if (currentDeptId == null) return List.of();
            return repo.findByDepartment_IdOrderByIdDesc(currentDeptId);
        }
        scope.requireDepartmentAccess(departmentId);
        return repo.findByDepartment_IdOrderByIdDesc(departmentId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_ADMIN')")
    public Workspace get(@PathVariable Long id) {
        Workspace w = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));
        scope.requireDepartmentAccess(w.getDepartment().getId());
        return w;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_ADMIN')")
    public Workspace create(@RequestBody Map<String, Object> body) {
        Long departmentId = body.get("departmentId") != null ? Long.valueOf(String.valueOf(body.get("departmentId"))) : null;
        if (!scope.hasRole("ADMIN")) {
            departmentId = scope.requireCurrentUser().getDepartment() != null ? scope.requireCurrentUser().getDepartment().getId() : null;
        }
        scope.requireDepartmentAccess(departmentId);
        Department dept = deptRepo.findById(departmentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found"));
        String name = String.valueOf(body.getOrDefault("name", "")).trim();
        if (name.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        Workspace w = new Workspace();
        w.setDepartment(dept);
        w.setName(name);
        if (body.get("type") != null) w.setType(String.valueOf(body.get("type")).trim());
        if (body.get("semester") != null) w.setSemester(String.valueOf(body.get("semester")).trim());
        if (body.get("active") != null) w.setActive(Boolean.parseBoolean(String.valueOf(body.get("active"))));
        if (body.get("startAt") != null) w.setStartAt(LocalDateTime.parse(String.valueOf(body.get("startAt"))));
        if (body.get("endAt") != null) w.setEndAt(LocalDateTime.parse(String.valueOf(body.get("endAt"))));
        if (w.getStatus() == null || w.getStatus().isBlank()) w.setStatus("DRAFT");
        Workspace saved = repo.save(w);
        auditLogService.log("CREATE_WORKSPACE", "Workspace", saved.getId().toString(), "dept=" + dept.getId());
        return saved;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_ADMIN')")
    public Workspace update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Workspace w = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));
        scope.requireDepartmentAccess(w.getDepartment().getId());
        if (w.getStatus() != null && !w.getStatus().equalsIgnoreCase("DRAFT")) {
            if (body.get("name") != null || body.get("type") != null || body.get("semester") != null || body.get("startAt") != null || body.get("endAt") != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workspace is not editable in current status");
            }
        }
        if (body.get("name") != null) {
            String name = String.valueOf(body.get("name")).trim();
            if (!name.isBlank()) w.setName(name);
        }
        if (body.get("type") != null) w.setType(String.valueOf(body.get("type")).trim());
        if (body.get("semester") != null) w.setSemester(String.valueOf(body.get("semester")).trim());
        if (body.get("active") != null) w.setActive(Boolean.parseBoolean(String.valueOf(body.get("active"))));
        if (body.get("startAt") != null) w.setStartAt(LocalDateTime.parse(String.valueOf(body.get("startAt"))));
        if (body.get("endAt") != null) w.setEndAt(LocalDateTime.parse(String.valueOf(body.get("endAt"))));
        Workspace saved = repo.save(w);
        auditLogService.log("UPDATE_WORKSPACE", "Workspace", saved.getId().toString(), "updated");
        return saved;
    }

    @PostMapping("/{id}/transition")
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_ADMIN')")
    public Workspace transition(@PathVariable Long id, @RequestParam String to) {
        Workspace w = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));
        scope.requireDepartmentAccess(w.getDepartment().getId());
        String current = normalizeStatus(w.getStatus());
        String target = normalizeStatus(to);
        if (!isValidTransition(current, target)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid transition");
        }
        requireTransitionPreconditions(w, current, target);
        w.setStatus(target);
        Workspace saved = repo.save(w);
        auditLogService.log("TRANSITION_WORKSPACE", "Workspace", saved.getId().toString(), current + "->" + target);
        return saved;
    }

    @PostMapping("/{id}/open")
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_ADMIN')")
    public Workspace open(@PathVariable Long id) {
        Workspace w = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));
        scope.requireDepartmentAccess(w.getDepartment().getId());
        String current = normalizeStatus(w.getStatus());
        String target = "OPEN_TOPIC";
        if (!isValidTransition(current, target)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid transition");
        }
        requireTransitionPreconditions(w, current, target);
        w.setStatus(target);
        Workspace saved = repo.save(w);
        auditLogService.log("OPEN_WORKSPACE", "Workspace", saved.getId().toString(), "status=" + target);
        return saved;
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_ADMIN')")
    public Workspace close(@PathVariable Long id) {
        Workspace w = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));
        scope.requireDepartmentAccess(w.getDepartment().getId());
        String current = normalizeStatus(w.getStatus());
        String target = "CLOSED";
        if (!isValidTransition(current, target)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid transition");
        }
        requireTransitionPreconditions(w, current, target);
        w.setStatus(target);
        Workspace saved = repo.save(w);
        auditLogService.log("CLOSE_WORKSPACE", "Workspace", saved.getId().toString(), "status=CLOSED");
        return saved;
    }

    private static String normalizeStatus(String status) {
        if (status == null) return "DRAFT";
        String s = status.trim().toUpperCase();
        if (s.equals("OPEN")) return "OPEN_TOPIC";
        return s;
    }

    private static boolean isValidTransition(String from, String to) {
        if (from.equals(to)) return true;
        return switch (from) {
            case "DRAFT" -> to.equals("OPEN_TOPIC");
            case "OPEN_TOPIC" -> to.equals("OPEN_REGISTRATION") || to.equals("CLOSED");
            case "OPEN_REGISTRATION" -> to.equals("LOCK_REGISTRATION") || to.equals("CLOSED");
            case "LOCK_REGISTRATION" -> to.equals("IN_PROGRESS") || to.equals("CLOSED");
            case "IN_PROGRESS" -> to.equals("CLOSED");
            case "CLOSED" -> false;
            default -> false;
        };
    }

    private void requireTransitionPreconditions(Workspace w, String from, String to) {
        if (from.equals(to)) return;
        if (to.equals("OPEN_REGISTRATION")) {
            long activeClassCount = workspaceClassRepo.countByWorkspace_IdAndActiveTrue(w.getId());
            if (activeClassCount <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workspace must have at least one active class before opening registration");
            }
            long topicCount = topicRepo.countByWorkspace_Id(w.getId());
            if (topicCount <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workspace must have at least one topic before opening registration");
            }
        }
    }
}

