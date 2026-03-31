package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.entity.CalendarEvent;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.repository.CalendarEventRepository;
import com.doanltmmt.Backend.repository.TopicRepository;
import com.doanltmmt.Backend.service.AuditLogService;
import com.doanltmmt.Backend.service.SecurityScopeService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/calendar")
@CrossOrigin(origins = "http://localhost:5173")
@SuppressWarnings("null")
public class CalendarController {

    private final CalendarEventRepository repo;
    private final TopicRepository topicRepo;
    private final AuditLogService auditLogService;
    private final SecurityScopeService scope;

    public CalendarController(CalendarEventRepository repo, TopicRepository topicRepo, AuditLogService auditLogService, SecurityScopeService scope) {
        this.repo = repo;
        this.topicRepo = topicRepo;
        this.auditLogService = auditLogService;
        this.scope = scope;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<CalendarEvent> list(@RequestParam Long topicId) {
        if (scope.hasRole("DEPARTMENT_ADMIN") && !scope.hasRole("ADMIN")) {
            Topic topic = topicRepo.findById(topicId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
            if (topic.getWorkspace() == null || topic.getWorkspace().getDepartment() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic has no workspace/department");
            }
            scope.requireDepartmentAccess(topic.getWorkspace().getDepartment().getId());
        }
        return repo.findByTopic_IdOrderByStartTimeAsc(topicId);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN')")
    public CalendarEvent create(@RequestBody Map<String, String> body) {
        Long topicId = Long.valueOf(body.get("topicId"));
        Topic topic = topicRepo.findById(topicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
        CalendarEvent ev = new CalendarEvent();
        ev.setTopic(topic);
        ev.setTitle(body.get("title"));
        ev.setDescription(body.getOrDefault("description", ""));
        if (body.get("startTime") != null) ev.setStartTime(LocalDateTime.parse(body.get("startTime")));
        if (body.get("endTime") != null) ev.setEndTime(LocalDateTime.parse(body.get("endTime")));
        ev.setType(body.getOrDefault("type", "MEETING"));
        CalendarEvent saved = repo.save(ev);
        auditLogService.log("CREATE_CAL_EVENT", "CalendarEvent", saved.getId().toString(), "Title: " + saved.getTitle());
        return saved;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN')")
    public void delete(@PathVariable Long id) {
        CalendarEvent ev = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        repo.delete(ev);
        auditLogService.log("DELETE_CAL_EVENT", "CalendarEvent", id.toString(), "Deleted event");
    }
}


