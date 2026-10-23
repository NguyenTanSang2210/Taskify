package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.dto.request.AnnouncementRequest;
import com.doanltmmt.Backend.dto.response.*;
import com.doanltmmt.Backend.service.AnnouncementService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Thông báo. Có hai loại:
 * <ul>
 *   <li><b>Theo đề tài</b> (có topicId): thành viên đề tài xem; người quản lý đề tài đó đăng.</li>
 *   <li><b>Toàn hệ thống</b> (không topicId): mọi người đã đăng nhập xem; CHỈ quản trị viên hệ thống đăng.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private final AnnouncementService service;

    public AnnouncementController(AnnouncementService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<AnnouncementDTO> list(@RequestParam(required = false) Long topicId) {
        return service.list(topicId);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN')")
    public AnnouncementDTO create(@Valid @RequestBody AnnouncementRequest body) {
        return service.create(body);
    }
}
