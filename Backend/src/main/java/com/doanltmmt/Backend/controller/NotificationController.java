package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.entity.enums.NotificationType;
import com.doanltmmt.Backend.dto.response.*;
import com.doanltmmt.Backend.entity.Notification;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.repository.NotificationRepository;
import com.doanltmmt.Backend.repository.TopicRegistrationRepository;
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

@SuppressWarnings("null")
public class NotificationController {

    private final NotificationRepository notificationRepo;
    private final UserRepository userRepo;
    private final EventPublisher events;
    private final com.doanltmmt.Backend.service.AuditLogService auditLogService;
    private final SecurityScopeService scope;
    private final TopicRegistrationRepository regRepo;

    public NotificationController(NotificationRepository notificationRepo, UserRepository userRepo, EventPublisher events, com.doanltmmt.Backend.service.AuditLogService auditLogService, SecurityScopeService scope, TopicRegistrationRepository regRepo) {
        this.notificationRepo = notificationRepo;
        this.userRepo = userRepo;
        this.events = events;
        this.auditLogService = auditLogService;
        this.scope = scope;
        this.regRepo = regRepo;
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('STUDENT','LECTURER','ADMIN','DEPARTMENT_ADMIN')")
    public List<NotificationDTO> mine(@RequestParam Long userId) {
        if (!scope.hasRole("ADMIN")) {
            userId = scope.requireCurrentUser().getId();
        }
        return notificationRepo.findByUser_IdOrderByCreatedAtDesc(userId).stream().map(NotificationDTO::from).toList();
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
    public List<NotificationDTO> recent(@RequestParam Long userId, @RequestParam(defaultValue = "5") int limit) {
        if (!scope.hasRole("ADMIN")) {
            userId = scope.requireCurrentUser().getId();
        }
        List<Notification> all = notificationRepo.findByUser_IdOrderByCreatedAtDesc(userId);
        return all.stream().limit(Math.max(1, limit)).map(NotificationDTO::from).toList();
    }

    @PostMapping("/read/{id}")
    @PreAuthorize("hasAnyRole('STUDENT','LECTURER','ADMIN','DEPARTMENT_ADMIN')")
    public NotificationDTO markRead(@PathVariable Long id) {
        Notification n = notificationRepo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (!scope.hasRole("ADMIN")) {
            Long currentUserId = scope.requireCurrentUser().getId();
            if (n.getUser() == null || n.getUser().getId() == null || !currentUserId.equals(n.getUser().getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Out of scope");
            }
        }
        n.setReadAt(LocalDateTime.now());
        return NotificationDTO.from(notificationRepo.save(n));
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
    public NotificationDTO create(@RequestParam Long userId,
                               @RequestParam String title,
                               @RequestParam(required = false) String content,
                               @RequestParam(defaultValue = "INFO") NotificationType type) {
        User u = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người nhận"));
        // ADMIN gửi cho bất kỳ ai; giảng viên CHỈ gửi cho sinh viên đã đăng ký đề tài do mình hướng dẫn
        if (!scope.hasRole("ADMIN")) {
            User me = scope.requireCurrentUser();
            if (!regRepo.existsByTopic_Lecturer_IdAndStudent_Id(me.getId(), u.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Bạn chỉ được gửi thông báo cho sinh viên đã đăng ký đề tài do bạn hướng dẫn.");
            }
        }
        Notification n = new Notification();
        n.setUser(u);
        n.setTitle(title);
        n.setContent(content);
        n.setType(type);
        Notification saved = notificationRepo.save(n);
        events.notificationCreated(saved);
        auditLogService.log("CREATE_NOTIFICATION", "Notification", saved.getId().toString(), "Manual notification to user " + userId);
        return NotificationDTO.from(saved);
    }
}



