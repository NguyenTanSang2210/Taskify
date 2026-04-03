package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.dto.TopicDTO;
import com.doanltmmt.Backend.entity.*;
import com.doanltmmt.Backend.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("null")
public class TopicService {

    private final TopicRepository topicRepo;
    private final LecturerRepository lecturerRepo;
    private final TopicRegistrationRepository regRepo;
    private final UserRepository userRepo;
    private final WorkspaceRepository workspaceRepo;
    private final SecurityScopeService scope;
    private final AuditLogService auditLogService;

    public TopicService(TopicRepository topicRepo,
                        LecturerRepository lecturerRepo,
                        TopicRegistrationRepository regRepo,
                        UserRepository userRepo,
                        WorkspaceRepository workspaceRepo,
                        SecurityScopeService scope,
                        AuditLogService auditLogService) {
        this.topicRepo = topicRepo;
        this.lecturerRepo = lecturerRepo;
        this.regRepo = regRepo;
        this.userRepo = userRepo;
        this.workspaceRepo = workspaceRepo;
        this.scope = scope;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public TopicDTO createTopic(Long lecturerId, Topic topic) {
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
        return new TopicDTO(saved);
    }

    @Transactional
    public TopicDTO updateTopic(Long id, Long lecturerId, Topic payload) {
        Topic topic = topicRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
        
        if (!canModifyTopic(topic, lecturerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Permission denied to update topic");
        }
        
        if (topic.getWorkspace() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic is not assigned to a workspace");
        }
        
        requireWorkspaceAllowsTopicCreateOrEdit(topic.getWorkspace());
        
        if (payload.getTitle() != null) topic.setTitle(payload.getTitle());
        if (payload.getDescription() != null) topic.setDescription(payload.getDescription());
        if (payload.getCapacity() != null) topic.setCapacity(payload.getCapacity());
        if (payload.getStatus() != null) topic.setStatus(payload.getStatus());
        
        Topic saved = topicRepo.save(topic);
        auditLogService.log("UPDATE_TOPIC", "Topic", saved.getId().toString(), "Updated topic details");
        return new TopicDTO(saved);
    }

    @Transactional
    public void deleteTopic(Long id, Long lecturerId) {
        Topic topic = topicRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
        
        if (!canModifyTopic(topic, lecturerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Permission denied to delete topic");
        }
        
        if (topic.getWorkspace() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic is not assigned to a workspace");
        }
        
        requireWorkspaceAllowsTopicCreateOrEdit(topic.getWorkspace());
        topicRepo.deleteById(id);
        auditLogService.log("DELETE_TOPIC", "Topic", id.toString(), "Deleted");
    }

    @Transactional
    public TopicDTO openTopic(Long id, Long lecturerId) {
        Topic topic = topicRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
        if (!canModifyTopic(topic, lecturerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Permission denied to open topic");
        }
        requireWorkspaceAllowsTopicOpenClose(topic.getWorkspace());
        topic.setStatus("OPEN");
        return new TopicDTO(topicRepo.save(topic));
    }

    @Transactional
    public TopicDTO closeTopic(Long id, Long lecturerId) {
        Topic topic = topicRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
        if (!canModifyTopic(topic, lecturerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Permission denied to close topic");
        }
        requireWorkspaceAllowsTopicOpenClose(topic.getWorkspace());
        topic.setStatus("CLOSED");
        return new TopicDTO(topicRepo.save(topic));
    }

    public Map<String, Object> getAll(Long studentId, String queryText, String status, Long lecturerId, int page, int size) {
        Specification<Topic> spec = Specification.where(null);

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
            if (deptId != null) {
                Long finalDeptId = deptId;
                spec = spec.and((root, q, cb) -> cb.equal(root.get("workspace").get("department").get("id"), finalDeptId));
            } else {
                return Map.of("items", List.of(), "page", page, "size", size, "totalElements", 0, "totalPages", 0);
            }
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<Topic> pageRes = topicRepo.findAll(spec, pageable);

        List<Map<String, Object>> items = pageRes.getContent().stream().map(topic -> {
            Map<String, Object> topicMap = new HashMap<>();
            topicMap.put("id", topic.getId());
            topicMap.put("title", topic.getTitle());
            topicMap.put("description", topic.getDescription());
            topicMap.put("status", topic.getStatus());
            topicMap.put("capacity", topic.getCapacity());
            
            // Basic lecturer info to avoid full entity deep nesting in this specific map
            if (topic.getLecturer() != null && topic.getLecturer().getUser() != null) {
                topicMap.put("lecturerName", topic.getLecturer().getUser().getFullName());
                topicMap.put("lecturerId", topic.getLecturer().getId());
            }

            topicMap.put("registrationCount", regRepo.countByTopic_Id(topic.getId()));
            topicMap.put("pendingCount", regRepo.countByTopic_IdAndApprovedIsNull(topic.getId()));
            topicMap.put("approvedCount", regRepo.countByTopic_IdAndApprovedTrue(topic.getId()));
            topicMap.put("rejectedCount", regRepo.countByTopic_IdAndApprovedFalse(topic.getId()));

            String regStatus = "CHUA_DANG_KY";
            if (studentId != null) {
                regStatus = regRepo.findTopByStudent_IdAndTopic_IdOrderByRegisteredAtDesc(studentId, topic.getId())
                        .map(reg -> {
                            if (reg.getApproved() == null) return "CHO_DUYET";
                            return reg.getApproved() ? "DA_DUYET" : "TU_CHOI";
                        }).orElse("CHUA_DANG_KY");
            }
            topicMap.put("registrationStatus", regStatus);
            return topicMap;
        }).collect(Collectors.toList());

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
        if (!scope.hasRole("LECTURER") || lecturerId == null) return false;
        Long currentUserId = scope.requireCurrentUser().getId();
        if (!Objects.equals(currentUserId, lecturerId)) return false;
        return topic.getLecturer() != null && Objects.equals(topic.getLecturer().getId(), lecturerId);
    }

    private void requireWorkspaceAllowsTopicCreateOrEdit(Workspace w) {
        String status = w.getStatus() == null ? "DRAFT" : w.getStatus().trim().toUpperCase();
        if (status.equals("OPEN")) status = "OPEN_TOPIC";
        if (!status.equals("OPEN_TOPIC")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workspace is not open for topic creation");
        }
    }

    private void requireWorkspaceAllowsTopicOpenClose(Workspace w) {
        String status = w.getStatus() == null ? "DRAFT" : w.getStatus().trim().toUpperCase();
        if (status.equals("OPEN")) status = "OPEN_TOPIC";
        if (!status.equals("OPEN_TOPIC") && !status.equals("OPEN_REGISTRATION")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workspace is not in topic/registration phase");
        }
    }
}
