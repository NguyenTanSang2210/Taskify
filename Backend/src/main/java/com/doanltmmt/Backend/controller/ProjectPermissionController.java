package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.entity.ProjectPermission;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.repository.ProjectPermissionRepository;
import com.doanltmmt.Backend.repository.TopicRepository;
import com.doanltmmt.Backend.service.AuditLogService;
import com.doanltmmt.Backend.service.SecurityScopeService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/permissions")
@CrossOrigin(origins = "http://localhost:5173")
public class ProjectPermissionController {

    private final ProjectPermissionRepository repo;
    private final TopicRepository topicRepo;
    private final AuditLogService auditLogService;
    private final SecurityScopeService scope;

    public ProjectPermissionController(ProjectPermissionRepository repo, TopicRepository topicRepo, AuditLogService auditLogService, SecurityScopeService scope) {
        this.repo = repo;
        this.topicRepo = topicRepo;
        this.auditLogService = auditLogService;
        this.scope = scope;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN','DEPARTMENT_ADMIN')")
    public List<ProjectPermission> list(@RequestParam Long topicId) {
        Topic topic = topicRepo.findById(topicId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
        requireAccess(topic);
        return repo.findByTopic_Id(topicId);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN','DEPARTMENT_ADMIN')")
    public ProjectPermission upsert(@RequestBody Map<String, Object> body) {
        Long topicId = Long.valueOf(body.get("topicId").toString());
        String roleName = (String) body.get("roleName");
        String toolName = (String) body.get("toolName");
        Topic topic = topicRepo.findById(topicId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
        requireAccess(topic);
        ProjectPermission perm = repo.findByTopic_IdAndRoleNameAndToolName(topicId, roleName, toolName)
                .orElseGet(ProjectPermission::new);
        perm.setTopic(topic);
        perm.setRoleName(roleName);
        perm.setToolName(toolName);
        perm.setCanView(Boolean.parseBoolean(body.getOrDefault("canView", true).toString()));
        perm.setCanCreate(Boolean.parseBoolean(body.getOrDefault("canCreate", false).toString()));
        perm.setCanUpdate(Boolean.parseBoolean(body.getOrDefault("canUpdate", false).toString()));
        perm.setCanDelete(Boolean.parseBoolean(body.getOrDefault("canDelete", false).toString()));
        ProjectPermission saved = repo.save(perm);
        auditLogService.log("UPSERT_PERMISSION", "ProjectPermission", saved.getId().toString(), roleName + " / " + toolName);
        return saved;
    }

    private void requireAccess(Topic topic) {
        if (scope.hasRole("ADMIN")) return;
        if (scope.hasRole("DEPARTMENT_ADMIN")) {
            if (topic.getWorkspace() == null || topic.getWorkspace().getDepartment() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic has no workspace/department");
            }
            scope.requireDepartmentAccess(topic.getWorkspace().getDepartment().getId());
            return;
        }
        if (!scope.hasRole("LECTURER")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        Long currentUserId = scope.requireCurrentUser().getId();
        if (topic.getLecturer() == null || topic.getLecturer().getId() == null || !currentUserId.equals(topic.getLecturer().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Out of scope");
        }
    }
}
