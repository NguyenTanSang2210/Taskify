package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.dto.request.AnnouncementRequest;
import com.doanltmmt.Backend.dto.response.*;
import com.doanltmmt.Backend.entity.Announcement;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.repository.AnnouncementRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Thông báo chung: theo đề tài (giảng viên hướng dẫn) hoặc toàn hệ thống (ADMIN). */
@Service
@SuppressWarnings("null")
public class AnnouncementService {

    private final AnnouncementRepository repo;
    private final AuditLogService auditLogService;
    private final SecurityScopeService scope;
    private final TopicAccessService topicAccess;

    public AnnouncementService(AnnouncementRepository repo,
                               AuditLogService auditLogService,
                               SecurityScopeService scope,
                               TopicAccessService topicAccess) {
        this.repo = repo;
        this.auditLogService = auditLogService;
        this.scope = scope;
        this.topicAccess = topicAccess;
    }

    @Transactional(readOnly = true)
    public List<AnnouncementDTO> list(Long topicId) {
        if (topicId == null) {
            return repo.findByTopicIsNullOrderByCreatedAtDesc().stream().map(AnnouncementDTO::from).toList();
        }
        Topic topic = topicAccess.loadTopicOr404(topicId);
        topicAccess.requireMember(topic);
        return repo.findByTopic_IdOrderByCreatedAtDesc(topicId).stream().map(AnnouncementDTO::from).toList();
    }

    @Transactional
    public AnnouncementDTO create(AnnouncementRequest body) {
        Topic topic = null;
        if (body.topicId() != null) {
            topic = topicAccess.loadTopicOr404(body.topicId());
            topicAccess.requireOwner(topic);
        } else if (!scope.hasRole("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ quản trị viên hệ thống được đăng thông báo toàn hệ thống.");
        }

        User author = scope.requireCurrentUser();
        Announcement a = new Announcement();
        a.setTitle(body.title());
        a.setContent(body.content());
        a.setAuthor(author);
        a.setTopic(topic);
        Announcement saved = repo.save(a);
        auditLogService.log("CREATE_ANNOUNCEMENT", "Announcement", saved.getId().toString(), saved.getTitle());
        return AnnouncementDTO.from(saved);
    }
}
