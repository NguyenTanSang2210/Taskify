package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.entity.Notification;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.repository.NotificationRepository;
import com.doanltmmt.Backend.repository.UserRepository;
import com.doanltmmt.Backend.service.EventPublisher;
import com.doanltmmt.Backend.service.SecurityScopeService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "http://localhost:5173")
@SuppressWarnings("null")
public class NotificationController {

    private final NotificationRepository notificationRepo;
    private final UserRepository userRepo;
    private final EventPublisher events;
    private final com.doanltmmt.Backend.service.AuditLogService auditLogService;
    private final SecurityScopeService scope;

    public NotificationController(NotificationRepository notificationRepo, UserRepository userRepo, EventPublisher events, com.doanltmmt.Backend.service.AuditLogService auditLogService, SecurityScopeService scope) {
        this.notificationRepo = notificationRepo;
        this.userRepo = userRepo;
        this.events = events;
        this.auditLogService = auditLogService;
        this.scope = scope;
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('STUDENT','LECTURER','ADMIN','DEPARTMENT_ADMIN')")
    public List<Notification> mine(@RequestParam Long userId) {
        if (!scope.hasRole("ADMIN")) {
            userId = scope.requireCurrentUser().getId();
        }
        return notificationRepo.findByUser_IdOrderByCreatedAtDesc(userId);
    }

    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('STUDENT','LECTURER','ADMIN','DEPARTMENT_ADMIN')")
    public long countUnread(@RequestParam Long userId) {
        if (!scope.hasRole("ADMIN")) {
            userId = scope.requireCurrentUser().getId();
        }
        return notificationRepo.countByUser_IdAndReadAtIsNull(userId);
    }

    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('STUDENT','LECTURER','ADMIN','DEPARTMENT_ADMIN')")
    public List<Notification> recent(@RequestParam Long userId, @RequestParam(defaultValue = "5") int limit) {
        if (!scope.hasRole("ADMIN")) {
            userId = scope.requireCurrentUser().getId();
        }
        List<Notification> all = notificationRepo.findByUser_IdOrderByCreatedAtDesc(userId);
        return all.stream().limit(Math.max(1, limit)).toList();
    }

    @PostMapping("/read/{id}")
    @PreAuthorize("hasAnyRole('STUDENT','LECTURER','ADMIN','DEPARTMENT_ADMIN')")
    public Notification markRead(@PathVariable Long id) {
        Notification n = notificationRepo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (!scope.hasRole("ADMIN")) {
            Long currentUserId = scope.requireCurrentUser().getId();
            if (n.getUser() == null || n.getUser().getId() == null || !currentUserId.equals(n.getUser().getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Out of scope");
            }
        }
        n.setReadAt(LocalDateTime.now());
        return notificationRepo.save(n);
    }

    @PostMapping("/read-all")
    @PreAuthorize("hasAnyRole('STUDENT','LECTURER','ADMIN','DEPARTMENT_ADMIN')")
    public void markAllRead(@RequestParam Long userId) {
        if (!scope.hasRole("ADMIN")) {
            userId = scope.requireCurrentUser().getId();
        }
        List<Notification> unread = notificationRepo.findByUser_IdAndReadAtIsNull(userId);
        LocalDateTime now = LocalDateTime.now();
        for (Notification n : unread) {
            n.setReadAt(now);
        }
        notificationRepo.saveAll(unread);
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN')")
    public Notification create(@RequestParam Long userId,
                               @RequestParam String title,
                               @RequestParam(required = false) String content,
                               @RequestParam(defaultValue = "INFO") String type) {
        User u = userRepo.findById(userId).orElseThrow();
        Notification n = new Notification();
        n.setUser(u);
        n.setTitle(title);
        n.setContent(content);
        n.setType(type);
        Notification saved = notificationRepo.save(n);
        events.notificationCreated(saved);
        auditLogService.log("CREATE_NOTIFICATION", "Notification", saved.getId().toString(), "Manual notification to user " + userId);
        return saved;
    }
}



