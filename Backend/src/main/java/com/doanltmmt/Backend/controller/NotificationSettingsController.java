package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.dto.request.NotificationSettingsRequest;
import com.doanltmmt.Backend.dto.response.NotificationSettingsDTO;
import com.doanltmmt.Backend.service.NotificationSettingsService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** P4-06 — cài đặt email thông báo của chính người dùng đang đăng nhập. */
@RestController
@RequestMapping("/api/notification-settings")
@PreAuthorize("isAuthenticated()")
public class NotificationSettingsController {

    private final NotificationSettingsService service;

    public NotificationSettingsController(NotificationSettingsService service) {
        this.service = service;
    }

    @GetMapping
    public NotificationSettingsDTO mine() {
        return service.mine();
    }

    @PutMapping
    public NotificationSettingsDTO update(@Valid @RequestBody NotificationSettingsRequest body) {
        return service.update(body);
    }
}
