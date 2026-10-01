package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.dto.response.*;
import com.doanltmmt.Backend.entity.Announcement;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.repository.AnnouncementRepository;
import com.doanltmmt.Backend.service.AuditLogService;
import com.doanltmmt.Backend.service.EventPublisher;
import com.doanltmmt.Backend.service.SecurityScopeService;
import com.doanltmmt.Backend.service.TopicAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * Thông báo. Có hai loại:
 * <ul>
 *   <li><b>Theo đề tài</b> (có topicId): thành viên đề tài xem; người quản lý đề tài đó đăng.</li>
 *   <li><b>Toàn hệ thống</b> (không topicId): mọi người đã đăng nhập xem; CHỈ quản trị viên hệ thống đăng.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/announcements")
@SuppressWarnings("null")
public class AnnouncementController {

    private final AnnouncementRepository repo;
    private final AuditLogService auditLogService;
    private final EventPublisher events;
    private final SecurityScopeService scope;
    private final TopicAccessService topicAccess;

    public AnnouncementController(AnnouncementRepository repo,
                                  AuditLogService auditLogService,
                                  EventPublisher events,
                                  SecurityScopeService scope,
                                  TopicAccessService topicAccess) {
        this.repo = repo;
        this.auditLogService = auditLogService;
        this.events = events;
        this.scope = scope;
        this.topicAccess = topicAccess;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<AnnouncementDTO> list(@RequestParam(required = false) Long topicId) {
        if (topicId == null) {
            return repo.findByTopicIsNullOrderByCreatedAtDesc().stream().map(AnnouncementDTO::from).toList();
        }
        Topic topic = topicAccess.loadTopicOr404(topicId);
        topicAccess.requireMember(topic);
        return repo.findByTopic_IdOrderByCreatedAtDesc(topicId).stream().map(AnnouncementDTO::from).toList();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN')")
    public AnnouncementDTO create(@RequestBody Map<String, String> body) {
        Topic topic = null;
        if (body.get("topicId") != null) {
            topic = topicAccess.loadTopicOr404(Long.valueOf(body.get("topicId")));
            topicAccess.requireOwner(topic);
        } else if (!scope.hasRole("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ quản trị viên hệ thống được đăng thông báo toàn hệ thống.");
        }

        User author = scope.requireCurrentUser();
        Announcement a = new Announcement();
        a.setTitle(body.get("title"));
        a.setContent(body.get("content"));
        a.setAuthor(author);
        a.setTopic(topic);
        Announcement saved = repo.save(a);
        auditLogService.log("CREATE_ANNOUNCEMENT", "Announcement", saved.getId().toString(), saved.getTitle());
        return AnnouncementDTO.from(saved);
    }
}
