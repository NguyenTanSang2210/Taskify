package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.dto.WorkspaceDTO;
import com.doanltmmt.Backend.entity.Department;
import com.doanltmmt.Backend.entity.Workspace;
import com.doanltmmt.Backend.repository.DepartmentRepository;
import com.doanltmmt.Backend.repository.TopicRepository;
import com.doanltmmt.Backend.repository.WorkspaceClassRepository;
import com.doanltmmt.Backend.repository.WorkspaceRepository;
import com.doanltmmt.Backend.util.Inputs;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
@SuppressWarnings("null")
public class WorkspaceService {

    private final WorkspaceRepository repo;
    private final DepartmentRepository deptRepo;
    private final TopicRepository topicRepo;
    private final WorkspaceClassRepository workspaceClassRepo;
    private final SecurityScopeService scope;
    private final AuditLogService auditLogService;

    public WorkspaceService(WorkspaceRepository repo, DepartmentRepository deptRepo, TopicRepository topicRepo, WorkspaceClassRepository workspaceClassRepo, SecurityScopeService scope, AuditLogService auditLogService) {
        this.repo = repo;
        this.deptRepo = deptRepo;
        this.topicRepo = topicRepo;
        this.workspaceClassRepo = workspaceClassRepo;
        this.scope = scope;
        this.auditLogService = auditLogService;
    }

    /** Đợt ở trạng thái OPEN_TOPIC của khoa người dùng (Admin: mọi khoa). */
    public List<WorkspaceDTO> listOpenForTopics() {
        List<String> open = List.of("OPEN_TOPIC", "OPEN");
        if (scope.hasRole("ADMIN")) {
            return repo.findByStatusInOrderByIdDesc(open).stream().map(WorkspaceDTO::new).toList();
        }
        var dept = scope.requireCurrentUser().getDepartment();
        if (dept == null) return List.of();
        return repo.findByDepartment_IdAndStatusInOrderByIdDesc(dept.getId(), open).stream().map(WorkspaceDTO::new).toList();
    }

    public List<WorkspaceDTO> list(Long departmentId) {
        List<Workspace> list;
        if (departmentId == null) {
            if (scope.hasRole("ADMIN")) {
                list = repo.findAll();
            } else {
                Long currentDeptId = scope.requireCurrentUser().getDepartment() != null ? scope.requireCurrentUser().getDepartment().getId() : null;
                if (currentDeptId == null) return List.of();
                list = repo.findByDepartment_IdOrderByIdDesc(currentDeptId);
            }
        } else {
            scope.requireDepartmentAccess(departmentId);
            list = repo.findByDepartment_IdOrderByIdDesc(departmentId);
        }
        return list.stream().map(WorkspaceDTO::new).toList();
    }

    public WorkspaceDTO get(Long id) {
        Workspace w = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));
        scope.requireDepartmentAccess(w.getDepartment().getId());
        return new WorkspaceDTO(w);
    }

    @Transactional
    public WorkspaceDTO create(Map<String, Object> body) {
        Long departmentId;
        if (scope.hasRole("ADMIN")) {
            departmentId = Inputs.requiredId(body.get("departmentId"), "Khoa (departmentId)");
        } else {
            departmentId = scope.requireCurrentUser().getDepartment() != null ? scope.requireCurrentUser().getDepartment().getId() : null;
        }
        scope.requireDepartmentAccess(departmentId);
        Department dept = deptRepo.findById(departmentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found"));
        String name = Inputs.trimmed(body.get("name"));
        if (name.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        
        Workspace w = new Workspace();
        w.setDepartment(dept);
        w.setName(name);
        if (body.get("type") != null) w.setType(String.valueOf(body.get("type")).trim());
        if (body.get("semester") != null) w.setSemester(String.valueOf(body.get("semester")).trim());
        if (body.get("active") != null) w.setActive(Boolean.parseBoolean(String.valueOf(body.get("active"))));
        w.setStartAt(Inputs.dateTime(body.get("startAt"), "Ngày bắt đầu"));
        w.setEndAt(Inputs.dateTime(body.get("endAt"), "Ngày kết thúc"));
        validatePeriod(w);
        if (w.getStatus() == null || w.getStatus().isBlank()) w.setStatus("DRAFT");
        
        Workspace saved = repo.save(w);
        auditLogService.log("CREATE_WORKSPACE", "Workspace", saved.getId().toString(), "dept=" + dept.getId());
        return new WorkspaceDTO(saved);
    }

    @Transactional
    public WorkspaceDTO update(Long id, Map<String, Object> body) {
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
        if (body.get("startAt") != null) w.setStartAt(Inputs.dateTime(body.get("startAt"), "Ngày bắt đầu"));
        if (body.get("endAt") != null) w.setEndAt(Inputs.dateTime(body.get("endAt"), "Ngày kết thúc"));
        validatePeriod(w);

        Workspace saved = repo.save(w);
        auditLogService.log("UPDATE_WORKSPACE", "Workspace", saved.getId().toString(), "updated");
        return new WorkspaceDTO(saved);
    }

    @Transactional
    public WorkspaceDTO transition(Long id, String to) {
        Workspace w = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));
        scope.requireDepartmentAccess(w.getDepartment().getId());
        
        String current = normalizeStatus(w.getStatus());
        String target = normalizeStatus(to);
        
        if (!isValidTransition(current, target)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid transition: " + current + " -> " + target);
        }
        
        requireTransitionPreconditions(w, current, target);
        w.setStatus(target);
        
        Workspace saved = repo.save(w);
        auditLogService.log("TRANSITION_WORKSPACE", "Workspace", saved.getId().toString(), current + " -> " + target);
        return new WorkspaceDTO(saved);
    }

    @Transactional
    public WorkspaceDTO open(Long id) {
        return transition(id, "OPEN_TOPIC");
    }

    @Transactional
    public WorkspaceDTO close(Long id) {
        return transition(id, "CLOSED");
    }

    private String normalizeStatus(String status) {
        if (status == null) return "DRAFT";
        String s = status.trim().toUpperCase();
        if (s.equals("OPEN")) return "OPEN_TOPIC";
        return s;
    }

    private boolean isValidTransition(String from, String to) {
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

    private static void validatePeriod(Workspace w) {
        if (w.getStartAt() != null && w.getEndAt() != null && w.getEndAt().isBefore(w.getStartAt())) {
            throw Inputs.badRequest("Ngày kết thúc phải sau ngày bắt đầu");
        }
    }
}
