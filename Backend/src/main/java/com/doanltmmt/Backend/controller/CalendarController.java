package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.dto.request.CalendarEventRequest;
import com.doanltmmt.Backend.dto.response.*;
import com.doanltmmt.Backend.entity.CalendarEvent;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.enums.CalendarEventType;
import com.doanltmmt.Backend.repository.CalendarEventRepository;
import com.doanltmmt.Backend.service.AuditLogService;
import com.doanltmmt.Backend.service.TopicAccessService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Lịch của đề tài. Xem: thành viên đề tài. Tạo/xóa: chỉ người quản lý đề tài đó. */
@RestController
@RequestMapping("/api/calendar")
@SuppressWarnings("null")
public class CalendarController {

    private final CalendarEventRepository repo;
    private final AuditLogService auditLogService;
    private final TopicAccessService topicAccess;

    public CalendarController(CalendarEventRepository repo, AuditLogService auditLogService, TopicAccessService topicAccess) {
        this.repo = repo;
        this.auditLogService = auditLogService;
        this.topicAccess = topicAccess;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<CalendarEventDTO> list(@RequestParam Long topicId) {
        Topic topic = topicAccess.loadTopicOr404(topicId);
        topicAccess.requireMember(topic);
        return repo.findByTopic_IdOrderByStartTimeAsc(topicId).stream().map(CalendarEventDTO::from).toList();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN')")
    public CalendarEventDTO create(@Valid @RequestBody CalendarEventRequest body) {
        Topic topic = topicAccess.loadTopicOr404(body.topicId());
        topicAccess.requireOwner(topic);

        CalendarEvent ev = new CalendarEvent();
        ev.setTopic(topic);
        ev.setTitle(body.title());
        ev.setDescription(body.description());
        ev.setStartTime(body.startTime());
        ev.setEndTime(body.endTime());
        ev.setType(CalendarEventType.parseOrDefault(body.type()));
        CalendarEvent saved = repo.save(ev);
        auditLogService.log("CREATE_CAL_EVENT", "CalendarEvent", saved.getId().toString(), "Title: " + saved.getTitle());
        return CalendarEventDTO.from(saved);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN')")
    public void delete(@PathVariable Long id) {
        CalendarEvent ev = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sự kiện"));
        topicAccess.requireOwner(ev.getTopic());

        repo.delete(ev);
        auditLogService.log("DELETE_CAL_EVENT", "CalendarEvent", id.toString(), "Deleted event");
    }
}
