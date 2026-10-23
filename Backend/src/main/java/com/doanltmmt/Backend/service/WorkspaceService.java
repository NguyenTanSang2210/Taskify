package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.exception.ErrorCodes;
import com.doanltmmt.Backend.exception.ApiException;
import com.doanltmmt.Backend.entity.enums.WorkspaceStatus;
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
        if (scope.hasRole("ADMIN")) {
            return repo.findByStatusOrderByIdDesc(WorkspaceStatus.OPEN_TOPIC).stream().map(WorkspaceDTO::new).toList();
        }
        var dept = scope.requireCurrentUser().getDepartment();
        if (dept == null) return List.of();
        return repo.findByDepartment_IdAndStatusOrderByIdDesc(dept.getId(), WorkspaceStatus.OPEN_TOPIC).stream().map(WorkspaceDTO::new).toList();
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
        Workspace w = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đợt."));
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
        Department dept = deptRepo.findById(departmentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khoa."));
        String name = Inputs.trimmed(body.get("name"));
        if (name.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên là bắt buộc.");
        
        Workspace w = new Workspace();
        w.setDepartment(dept);
        w.setName(name);
        if (body.get("type") != null) w.setType(String.valueOf(body.get("type")).trim());
        if (body.get("semester") != null) w.setSemester(String.valueOf(body.get("semester")).trim());
        if (body.get("active") != null) w.setActive(Boolean.parseBoolean(String.valueOf(body.get("active"))));
        w.setStartAt(Inputs.dateTime(body.get("startAt"), "Ngày bắt đầu"));
        w.setEndAt(Inputs.dateTime(body.get("endAt"), "Ngày kết thúc"));
        validatePeriod(w);
        
        Workspace saved = repo.save(w);
        auditLogService.log("CREATE_WORKSPACE", "Workspace", saved.getId().toString(), "dept=" + dept.getId());
        return new WorkspaceDTO(saved);
    }

    @Transactional
    public WorkspaceDTO update(Long id, Map<String, Object> body) {
        Workspace w = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đợt."));
        scope.requireDepartmentAccess(w.getDepartment().getId());
        
        if (w.getStatus() != WorkspaceStatus.DRAFT) {
            if (body.get("name") != null || body.get("type") != null || body.get("semester") != null || body.get("startAt") != null || body.get("endAt") != null) {
                throw ApiException.badRequest(ErrorCodes.WORKSPACE_PHASE, "Chỉ sửa thông tin đợt khi đợt còn là bản nháp.");
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
        Workspace w = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đợt"));
        scope.requireDepartmentAccess(w.getDepartment().getId());

        WorkspaceStatus current = w.getStatus();
        WorkspaceStatus target = WorkspaceStatus.parse(to);
        if (!current.canTransitionTo(target)) {
            throw ApiException.badRequest(ErrorCodes.INVALID_TRANSITION, "Không thể chuyển đợt từ \"" + current.label() + "\" sang \"" + target.label() + "\"");
        }

        requireTransitionPreconditions(w, current, target);
        w.setStatus(target);

        Workspace saved = repo.save(w);
        auditLogService.log("TRANSITION_WORKSPACE", "Workspace", saved.getId().toString(), current + " -> " + target);
        return new WorkspaceDTO(saved);
    }

    @Transactional
    public WorkspaceDTO open(Long id) {
        return transition(id, WorkspaceStatus.OPEN_TOPIC.name());
    }

    @Transactional
    public WorkspaceDTO close(Long id) {
        return transition(id, WorkspaceStatus.CLOSED.name());
    }

    private void requireTransitionPreconditions(Workspace w, WorkspaceStatus from, WorkspaceStatus to) {
        if (from == to) return;
        if (to == WorkspaceStatus.OPEN_REGISTRATION) {
            long activeClassCount = workspaceClassRepo.countByWorkspace_IdAndActiveTrue(w.getId());
            if (activeClassCount <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cần gán ít nhất một lớp cho đợt trước khi mở đăng ký");
            }
            long topicCount = topicRepo.countByWorkspace_Id(w.getId());
            if (topicCount <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cần có ít nhất một đề tài trong đợt trước khi mở đăng ký");
            }
        }
    }

    private static void validatePeriod(Workspace w) {
        if (w.getStartAt() != null && w.getEndAt() != null && w.getEndAt().isBefore(w.getStartAt())) {
            throw Inputs.badRequest("Ngày kết thúc phải sau ngày bắt đầu");
        }
    }
}
