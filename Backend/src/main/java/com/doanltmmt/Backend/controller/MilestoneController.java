package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.entity.Milestone;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.repository.MilestoneRepository;
import com.doanltmmt.Backend.repository.TopicRepository;
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
@RequestMapping("/api/milestones")
@CrossOrigin(origins = "http://localhost:5173")
@SuppressWarnings("null")
public class MilestoneController {

    private final MilestoneRepository milestoneRepo;
    private final TopicRepository topicRepo;
    private final AuditLogService auditLogService;
    private final SecurityScopeService scope;

    public MilestoneController(MilestoneRepository milestoneRepo, TopicRepository topicRepo, AuditLogService auditLogService, SecurityScopeService scope) {
        this.milestoneRepo = milestoneRepo;
        this.topicRepo = topicRepo;
        this.auditLogService = auditLogService;
        this.scope = scope;
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Milestone getMilestone(@PathVariable Long id) {
        Milestone m = milestoneRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Milestone not found"));
        if (scope.hasRole("DEPARTMENT_ADMIN") && !scope.hasRole("ADMIN")) {
            if (m.getTopic() == null || m.getTopic().getWorkspace() == null || m.getTopic().getWorkspace().getDepartment() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Milestone has no topic/workspace/department");
            }
            scope.requireDepartmentAccess(m.getTopic().getWorkspace().getDepartment().getId());
        }
        return m;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<Milestone> getMilestones(@RequestParam Long topicId) {
        if (scope.hasRole("DEPARTMENT_ADMIN") && !scope.hasRole("ADMIN")) {
            Topic topic = topicRepo.findById(topicId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
            if (topic.getWorkspace() == null || topic.getWorkspace().getDepartment() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic has no workspace/department");
            }
            scope.requireDepartmentAccess(topic.getWorkspace().getDepartment().getId());
        }
        return milestoneRepo.findByTopic_IdOrderByDeadlineAsc(topicId);
    }

    @PostMapping
    @PreAuthorize("hasRole('LECTURER')")
    public Milestone createMilestone(@RequestBody Map<String, Object> body) {
        Long topicId = Long.valueOf(body.get("topicId").toString());
        Topic topic = topicRepo.findById(topicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));

        Milestone milestone = new Milestone();
        milestone.setTopic(topic);
        milestone.setTitle((String) body.get("title"));
        milestone.setDescription((String) body.getOrDefault("description", ""));

        if (body.get("deadline") != null) {
            milestone.setDeadline(LocalDateTime.parse((String) body.get("deadline")));
        }
        if (body.get("startDate") != null) {
            milestone.setStartDate(LocalDateTime.parse((String) body.get("startDate")));
        }

        Milestone saved = milestoneRepo.save(milestone);
        auditLogService.log("CREATE_MILESTONE", "Milestone", saved.getId().toString(), "Created milestone: " + saved.getTitle());
        return saved;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('LECTURER')")
    public Milestone updateMilestone(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Milestone milestone = milestoneRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Milestone not found"));

        if (body.containsKey("title")) milestone.setTitle((String) body.get("title"));
        if (body.containsKey("description")) milestone.setDescription((String) body.get("description"));
        if (body.containsKey("deadline")) {
            String dl = (String) body.get("deadline");
            milestone.setDeadline(dl == null ? null : LocalDateTime.parse(dl));
        }

        Milestone saved = milestoneRepo.save(milestone);
        auditLogService.log("UPDATE_MILESTONE", "Milestone", saved.getId().toString(), "Updated milestone");
        return saved;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('LECTURER')")
    public void deleteMilestone(@PathVariable Long id) {
        Milestone milestone = milestoneRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Milestone not found"));
        milestoneRepo.delete(milestone);
        auditLogService.log("DELETE_MILESTONE", "Milestone", id.toString(), "Deleted milestone");
    }
}



