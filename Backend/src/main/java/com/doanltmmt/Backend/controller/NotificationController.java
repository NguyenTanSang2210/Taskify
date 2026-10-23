package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.dto.response.NotificationDTO;
import com.doanltmmt.Backend.entity.enums.NotificationType;
import com.doanltmmt.Backend.service.NotificationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('STUDENT','LECTURER','ADMIN','DEPARTMENT_ADMIN')")
    public List<NotificationDTO> mine(@RequestParam Long userId) {
        return service.mine(userId);
    }

    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('STUDENT','LECTURER','ADMIN','DEPARTMENT_ADMIN')")
    public long countUnread(@RequestParam Long userId) {
        return service.countUnread(userId);
    }

    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('STUDENT','LECTURER','ADMIN','DEPARTMENT_ADMIN')")
    public List<NotificationDTO> recent(@RequestParam Long userId, @RequestParam(defaultValue = "5") int limit) {
        return service.recent(userId, limit);
    }

    @PostMapping("/read/{id}")
    @PreAuthorize("hasAnyRole('STUDENT','LECTURER','ADMIN','DEPARTMENT_ADMIN')")
    public NotificationDTO markRead(@PathVariable Long id) {
        return service.markRead(id);
    }

    @PostMapping("/read-all")
    @PreAuthorize("hasAnyRole('STUDENT','LECTURER','ADMIN','DEPARTMENT_ADMIN')")
    public void markAllRead(@RequestParam Long userId) {
        service.markAllRead(userId);
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN')")
    public NotificationDTO create(@RequestParam Long userId,
                                  @RequestParam String title,
                                  @RequestParam(required = false) String content,
                                  @RequestParam(defaultValue = "INFO") NotificationType type) {
        return service.createManual(userId, title, content, type);
    }
}
