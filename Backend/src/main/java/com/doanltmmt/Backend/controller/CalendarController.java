package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.dto.request.CalendarEventRequest;
import com.doanltmmt.Backend.dto.response.*;
import com.doanltmmt.Backend.service.CalendarService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Lịch của đề tài. Xem: thành viên đề tài. Tạo/xóa: chỉ người quản lý đề tài đó. */
@RestController
@RequestMapping("/api/calendar")
public class CalendarController {

    private final CalendarService service;

    public CalendarController(CalendarService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<CalendarEventDTO> list(@RequestParam Long topicId) {
        return service.list(topicId);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN')")
    public CalendarEventDTO create(@Valid @RequestBody CalendarEventRequest body) {
        return service.create(body);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN')")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
