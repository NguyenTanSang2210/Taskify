package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.dto.request.CalendarEventRequest;
import com.doanltmmt.Backend.dto.response.*;
import com.doanltmmt.Backend.entity.CalendarEvent;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.enums.CalendarEventType;
import com.doanltmmt.Backend.repository.CalendarEventRepository;
import com.doanltmmt.Backend.repository.MilestoneRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Lịch sự kiện của đề tài. */
@Service
@SuppressWarnings("null")
public class CalendarService {

    private final CalendarEventRepository repo;
    private final AuditLogService auditLogService;
    private final TopicAccessService topicAccess;
    private final MilestoneRepository milestoneRepo;

    public CalendarService(CalendarEventRepository repo, AuditLogService auditLogService, TopicAccessService topicAccess,
                           MilestoneRepository milestoneRepo) {
        this.milestoneRepo = milestoneRepo;
        this.repo = repo;
        this.auditLogService = auditLogService;
        this.topicAccess = topicAccess;
    }

    @Transactional(readOnly = true)
    public List<CalendarEventDTO> list(Long topicId) {
        Topic topic = topicAccess.loadTopicOr404(topicId);
        topicAccess.requireMember(topic);
        // P4-05: lịch học thuật gồm cả các mốc của đề tài, xếp theo thời gian
        List<CalendarEventDTO> items = new ArrayList<>(repo.findByTopic_IdOrderByStartTimeAsc(topicId).stream().map(CalendarEventDTO::from).toList());
        milestoneRepo.findByTopic_IdOrderByDeadlineAsc(topicId).forEach(m -> items.add(CalendarEventDTO.fromMilestone(m)));
        items.sort(Comparator.comparing(CalendarEventDTO::startTime, Comparator.nullsLast(Comparator.naturalOrder())));
        return items;
    }

    @Transactional
    public CalendarEventDTO create(CalendarEventRequest body) {
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

    @Transactional
    public void delete(Long id) {
        CalendarEvent ev = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sự kiện"));
        topicAccess.requireOwner(ev.getTopic());

        repo.delete(ev);
        auditLogService.log("DELETE_CAL_EVENT", "CalendarEvent", id.toString(), "Deleted event");
    }
}
