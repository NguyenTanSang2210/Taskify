package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.dto.response.NotificationDTO;
import com.doanltmmt.Backend.dto.response.PageResponse;
import com.doanltmmt.Backend.entity.enums.NotificationType;
import com.doanltmmt.Backend.service.NotificationService;
import com.doanltmmt.Backend.util.Pages;
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
    public PageResponse<NotificationDTO> mine(@RequestParam Long userId,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = Pages.DEFAULT_SIZE) int size) {
        return service.mine(userId, page, size);
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
