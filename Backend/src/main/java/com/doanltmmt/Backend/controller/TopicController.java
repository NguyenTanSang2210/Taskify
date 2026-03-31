package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.entity.Lecturer;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.entity.TopicRegistration;
import com.doanltmmt.Backend.entity.Workspace;
import com.doanltmmt.Backend.repository.LecturerRepository;
import com.doanltmmt.Backend.repository.TopicRepository;
import com.doanltmmt.Backend.repository.TopicRegistrationRepository;
import com.doanltmmt.Backend.repository.UserRepository;
import com.doanltmmt.Backend.repository.WorkspaceRepository;
import com.doanltmmt.Backend.service.SecurityScopeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/topics")
@CrossOrigin(origins = "http://localhost:5173")
@SuppressWarnings("null")
public class TopicController {

    private final TopicRepository topicRepo;
    private final LecturerRepository lecturerRepo;
    private final TopicRegistrationRepository regRepo;
    private final UserRepository userRepo;
    private final WorkspaceRepository workspaceRepo;
    private final SecurityScopeService scope;
    private final com.doanltmmt.Backend.service.AuditLogService auditLogService;

    public TopicController(TopicRepository topicRepo,
                           LecturerRepository lecturerRepo,
                           TopicRegistrationRepository regRepo,
                           UserRepository userRepo,
                           WorkspaceRepository workspaceRepo,
                           SecurityScopeService scope,
                           com.doanltmmt.Backend.service.AuditLogService auditLogService) {
        this.topicRepo = topicRepo;
        this.lecturerRepo = lecturerRepo;
        this.regRepo = regRepo;
        this.userRepo = userRepo;
        this.workspaceRepo = workspaceRepo;
        this.scope = scope;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN')")
    public Topic createTopic(@RequestParam Long lecturerId, @RequestBody Topic topic) {
        if (scope.hasRole("LECTURER")) {
            Long currentUserId = scope.requireCurrentUser().getId();
            if (!Objects.equals(currentUserId, lecturerId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Out of scope");
            }
        }
        if (topic.getWorkspace() == null || topic.getWorkspace().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic must be assigned to a workspace");
        }
        Workspace w = workspaceRepo.findById(topic.getWorkspace().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));
        requireWorkspaceAllowsTopicCreateOrEdit(w);

        // Ensure a Lecturer entity exists for the given user id. If missing, create it on-the-fly.
        Lecturer lecturer = lecturerRepo.findById(lecturerId).orElseGet(() -> {
            User user = userRepo.findById(lecturerId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found for lecturerId"));
            Lecturer newLecturer = new Lecturer();
            newLecturer.setUser(user);
            newLecturer.setDepartment(user.getDepartment());
            return lecturerRepo.save(newLecturer);
        });
        if (!scope.hasRole("ADMIN")) {
            if (lecturer.getUser() == null || lecturer.getUser().getDepartment() == null || w.getDepartment() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing department");
            }
            if (!lecturer.getUser().getDepartment().getId().equals(w.getDepartment().getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecturer is not in workspace department");
            }
        }

        topic.setLecturer(lecturer);
        topic.setStatus("OPEN");
        topic.setWorkspace(w);

        Topic saved = topicRepo.save(topic);
        auditLogService.log("CREATE_TOPIC", "Topic", saved.getId().toString(), "Title: " + saved.getTitle());
        return saved;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN','DEPARTMENT_ADMIN')")
    public Topic updateTopic(@PathVariable Long id,
                             @RequestParam(required = false) Long lecturerId,
                             @RequestBody Topic payload) {
        Topic topic = topicRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
        if (!canModifyTopic(topic, lecturerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Permission denied to update topic");
        }
        if (topic.getWorkspace() == null || topic.getWorkspace().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic is not assigned to a workspace");
        }
        Workspace w = workspaceRepo.findById(topic.getWorkspace().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));
        requireWorkspaceAllowsTopicCreateOrEdit(w);
        if (payload.getTitle() != null) topic.setTitle(payload.getTitle());
        if (payload.getDescription() != null) topic.setDescription(payload.getDescription());
        if (payload.getCapacity() != null) topic.setCapacity(payload.getCapacity());
        if (payload.getStatus() != null) topic.setStatus(payload.getStatus());
        Topic saved = topicRepo.save(topic);
        auditLogService.log("UPDATE_TOPIC", "Topic", saved.getId().toString(), "Updated topic details");
        return saved;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN','DEPARTMENT_ADMIN')")
    public void deleteTopic(@PathVariable Long id,
                            @RequestParam(required = false) Long lecturerId) {
        Topic topic = topicRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
        if (!canModifyTopic(topic, lecturerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Permission denied to delete topic");
        }
        if (topic.getWorkspace() == null || topic.getWorkspace().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic is not assigned to a workspace");
        }
        Workspace w = workspaceRepo.findById(topic.getWorkspace().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));
        requireWorkspaceAllowsTopicCreateOrEdit(w);
        topicRepo.deleteById(id);
    }

    @PostMapping("/{id}/open")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN','DEPARTMENT_ADMIN')")
    public Topic openTopic(@PathVariable Long id,
                           @RequestParam(required = false) Long lecturerId) {
        Topic topic = topicRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
        if (!canModifyTopic(topic, lecturerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Permission denied to open topic");
        }
        if (topic.getWorkspace() == null || topic.getWorkspace().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic is not assigned to a workspace");
        }
        Workspace w = workspaceRepo.findById(topic.getWorkspace().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));
        requireWorkspaceAllowsTopicOpenClose(w);
        topic.setStatus("OPEN");
        return topicRepo.save(topic);
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN','DEPARTMENT_ADMIN')")
    public Topic closeTopic(@PathVariable Long id,
                            @RequestParam(required = false) Long lecturerId) {
        Topic topic = topicRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
        if (!canModifyTopic(topic, lecturerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Permission denied to close topic");
        }
        if (topic.getWorkspace() == null || topic.getWorkspace().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic is not assigned to a workspace");
        }
        Workspace w = workspaceRepo.findById(topic.getWorkspace().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));
        requireWorkspaceAllowsTopicOpenClose(w);
        topic.setStatus("CLOSED");
        return topicRepo.save(topic);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT','LECTURER','ADMIN','DEPARTMENT_ADMIN')")
    public Map<String, Object> getAll(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false, name = "query") String queryText,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long lecturerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size
    ) {
        org.springframework.data.jpa.domain.Specification<Topic> spec = org.springframework.data.jpa.domain.Specification.where(null);

        if (status != null && !status.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status));
        }
        if (lecturerId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("lecturer").get("id"), lecturerId));
        }
        if (queryText != null && !queryText.isBlank()) {
            String qLower = "%" + queryText.toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("title")), qLower));
        }
        if (scope.hasRole("DEPARTMENT_ADMIN") && !scope.hasRole("ADMIN")) {
            Long deptId = scope.requireCurrentUser().getDepartment() != null ? scope.requireCurrentUser().getDepartment().getId() : null;
            if (deptId == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("items", List.of());
                response.put("page", page);
                response.put("size", size);
                response.put("totalElements", 0);
                response.put("totalPages", 0);
                return response;
            }
            Long finalDeptId = deptId;
            spec = spec.and((root, q, cb) -> cb.equal(root.get("workspace").get("department").get("id"), finalDeptId));
        }

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id"));

        org.springframework.data.domain.Page<Topic> pageRes = topicRepo.findAll(spec, pageable);

        List<Map<String, Object>> items = new ArrayList<>();
        for (Topic topic : pageRes.getContent()) {
            Map<String, Object> topicMap = new HashMap<>();
            topicMap.put("id", topic.getId());
            topicMap.put("title", topic.getTitle());
            topicMap.put("description", topic.getDescription());
            topicMap.put("status", topic.getStatus());
            topicMap.put("lecturer", topic.getLecturer());
            long regCount = regRepo.countByTopic_Id(topic.getId());
            long pendingCount = regRepo.countByTopic_IdAndApprovedIsNull(topic.getId());
            long approvedCount = regRepo.countByTopic_IdAndApprovedTrue(topic.getId());
            long rejectedCount = regRepo.countByTopic_IdAndApprovedFalse(topic.getId());
            topicMap.put("registrationCount", regCount);
            topicMap.put("pendingCount", pendingCount);
            topicMap.put("approvedCount", approvedCount);
            topicMap.put("rejectedCount", rejectedCount);

            String regStatus = "CHUA_DANG_KY";
            if (studentId != null) {
                Optional<TopicRegistration> regOpt = regRepo.findTopByStudent_IdAndTopic_IdOrderByRegisteredAtDesc(studentId, topic.getId());
                if (regOpt.isPresent()) {
                    TopicRegistration reg = regOpt.get();
                    Boolean approved = reg.getApproved();
                    if (approved == null) {
                        regStatus = "CHO_DUYET";
                    } else if (approved) {
                        regStatus = "DA_DUYET";
                    } else {
                        regStatus = "TU_CHOI";
                    }
                }
            }

            topicMap.put("registrationStatus", regStatus);
            items.add(topicMap);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("items", items);
        response.put("page", pageRes.getNumber());
        response.put("size", pageRes.getSize());
        response.put("totalElements", pageRes.getTotalElements());
        response.put("totalPages", pageRes.getTotalPages());
        return response;
    }

    private boolean canModifyTopic(Topic topic, Long lecturerId) {
        if (scope.hasRole("ADMIN")) return true;
        if (scope.hasRole("DEPARTMENT_ADMIN")) {
            if (topic.getWorkspace() == null || topic.getWorkspace().getDepartment() == null) return false;
            scope.requireDepartmentAccess(topic.getWorkspace().getDepartment().getId());
            return true;
        }
        if (!scope.hasRole("LECTURER")) return false;
        if (lecturerId == null) return false;
        Long currentUserId = scope.requireCurrentUser().getId();
        if (!Objects.equals(currentUserId, lecturerId)) return false;
        return topic.getLecturer() != null && Objects.equals(topic.getLecturer().getId(), lecturerId);
    }

    private static void requireWorkspaceAllowsTopicCreateOrEdit(Workspace w) {
        String status = w.getStatus() == null ? "DRAFT" : w.getStatus().trim().toUpperCase();
        if (status.equals("OPEN")) status = "OPEN_TOPIC";
        if (!status.equals("OPEN_TOPIC")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workspace is not open for topic creation");
        }
    }

    private static void requireWorkspaceAllowsTopicOpenClose(Workspace w) {
        String status = w.getStatus() == null ? "DRAFT" : w.getStatus().trim().toUpperCase();
        if (status.equals("OPEN")) status = "OPEN_TOPIC";
        if (!status.equals("OPEN_TOPIC") && !status.equals("OPEN_REGISTRATION")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workspace is not in topic/registration phase");
        }
    }
}

