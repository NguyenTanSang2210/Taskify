package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.entity.Announcement;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.repository.AnnouncementRepository;
import com.doanltmmt.Backend.repository.TopicRepository;
import com.doanltmmt.Backend.repository.UserRepository;
import com.doanltmmt.Backend.service.AuditLogService;
import com.doanltmmt.Backend.service.EventPublisher;
import com.doanltmmt.Backend.service.SecurityScopeService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/announcements")
@CrossOrigin(origins = "http://localhost:5173")
public class AnnouncementController {

    private final AnnouncementRepository repo;
    private final TopicRepository topicRepo;
    private final UserRepository userRepo;
    private final AuditLogService auditLogService;
    private final EventPublisher events;
    private final SecurityScopeService scope;

    public AnnouncementController(AnnouncementRepository repo, TopicRepository topicRepo, UserRepository userRepo, AuditLogService auditLogService, EventPublisher events, SecurityScopeService scope) {
        this.repo = repo;
        this.topicRepo = topicRepo;
        this.userRepo = userRepo;
        this.auditLogService = auditLogService;
        this.events = events;
        this.scope = scope;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<Announcement> list(@RequestParam(required = false) Long topicId) {
        if (topicId == null) return repo.findByTopicIsNullOrderByCreatedAtDesc();
        if (scope.hasRole("DEPARTMENT_ADMIN") && !scope.hasRole("ADMIN")) {
            Topic topic = topicRepo.findById(topicId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
            if (topic.getWorkspace() == null || topic.getWorkspace().getDepartment() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic has no workspace/department");
            }
            scope.requireDepartmentAccess(topic.getWorkspace().getDepartment().getId());
        }
        return repo.findByTopic_IdOrderByCreatedAtDesc(topicId);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN')")
    public Announcement create(@RequestBody Map<String, String> body) {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User author = userRepo.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Announcement a = new Announcement();
        a.setTitle(body.get("title"));
        a.setContent(body.get("content"));
        a.setAuthor(author);
        if (body.get("topicId") != null) {
            Topic topic = topicRepo.findById(Long.valueOf(body.get("topicId")))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
            a.setTopic(topic);
        }
        Announcement saved = repo.save(a);
        auditLogService.log("CREATE_ANNOUNCEMENT", "Announcement", saved.getId().toString(), saved.getTitle());
        events.genericEvent("ANNOUNCEMENT_CREATED", saved.getId());
        return saved;
    }
}
