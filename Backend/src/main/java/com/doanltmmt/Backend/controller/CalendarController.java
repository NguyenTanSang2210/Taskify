package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.entity.enums.CalendarEventType;
import com.doanltmmt.Backend.dto.response.*;
import com.doanltmmt.Backend.entity.CalendarEvent;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.repository.CalendarEventRepository;
import com.doanltmmt.Backend.service.AuditLogService;
import com.doanltmmt.Backend.service.TopicAccessService;
import com.doanltmmt.Backend.util.Inputs;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

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
    public CalendarEventDTO create(@RequestBody Map<String, String> body) {
        Topic topic = topicAccess.loadTopicOr404(Inputs.requiredId(body.get("topicId"), "topicId"));
        topicAccess.requireOwner(topic);

        CalendarEvent ev = new CalendarEvent();
        ev.setTopic(topic);
        ev.setTitle(Inputs.requiredText(body.get("title"), "Tiêu đề sự kiện", 255));
        ev.setDescription(Inputs.optionalText(body.get("description"), "Mô tả", 5000));
        ev.setStartTime(Inputs.dateTime(body.get("startTime"), "Thời gian bắt đầu"));
        ev.setEndTime(Inputs.dateTime(body.get("endTime"), "Thời gian kết thúc"));
        if (ev.getStartTime() != null && ev.getEndTime() != null && ev.getEndTime().isBefore(ev.getStartTime())) {
            throw Inputs.badRequest("Thời gian kết thúc phải sau thời gian bắt đầu");
        }
        ev.setType(CalendarEventType.parseOrDefault(body.get("type")));
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
