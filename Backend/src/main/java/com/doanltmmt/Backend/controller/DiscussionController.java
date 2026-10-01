package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.entity.DiscussionPost;
import com.doanltmmt.Backend.entity.DiscussionThread;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.repository.DiscussionPostRepository;
import com.doanltmmt.Backend.repository.DiscussionThreadRepository;
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

/** Thảo luận theo đề tài: chỉ thành viên đề tài (đã được duyệt / giảng viên hướng dẫn / quản trị) xem và đăng bài. */
@RestController
@RequestMapping("/api/discuss")
@SuppressWarnings("null")
public class DiscussionController {

    private final DiscussionThreadRepository threadRepo;
    private final DiscussionPostRepository postRepo;
    private final AuditLogService auditLogService;
    private final EventPublisher events;
    private final SecurityScopeService scope;
    private final TopicAccessService topicAccess;

    public DiscussionController(DiscussionThreadRepository threadRepo,
                                DiscussionPostRepository postRepo,
                                AuditLogService auditLogService,
                                EventPublisher events,
                                SecurityScopeService scope,
                                TopicAccessService topicAccess) {
        this.threadRepo = threadRepo;
        this.postRepo = postRepo;
        this.auditLogService = auditLogService;
        this.events = events;
        this.scope = scope;
        this.topicAccess = topicAccess;
    }

    @GetMapping("/threads")
    @PreAuthorize("isAuthenticated()")
    public List<DiscussionThread> listThreads(@RequestParam Long topicId) {
        Topic topic = topicAccess.loadTopicOr404(topicId);
        topicAccess.requireMember(topic);
        return threadRepo.findByTopic_IdOrderByCreatedAtDesc(topicId);
    }

    @PostMapping("/threads")
    @PreAuthorize("isAuthenticated()")
    public DiscussionThread createThread(@RequestBody Map<String, String> body) {
        if (body.get("topicId") == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "topicId là bắt buộc");
        }
        Topic topic = topicAccess.loadTopicOr404(Long.valueOf(body.get("topicId")));
        topicAccess.requireMember(topic);

        User user = scope.requireCurrentUser();
        DiscussionThread t = new DiscussionThread();
        t.setTopic(topic);
        t.setTitle(body.get("title"));
        t.setCreatedBy(user);
        DiscussionThread saved = threadRepo.save(t);
        auditLogService.log("CREATE_THREAD", "DiscussionThread", saved.getId().toString(), "Title: " + saved.getTitle());
        events.genericEvent("THREAD_CREATED", saved.getId());
        return saved;
    }

    @GetMapping("/threads/{threadId}/posts")
    @PreAuthorize("isAuthenticated()")
    public List<DiscussionPost> listPosts(@PathVariable Long threadId) {
        DiscussionThread th = loadThread(threadId);
        topicAccess.requireMember(th.getTopic());
        return postRepo.findByThread_IdOrderByCreatedAtAsc(threadId);
    }

    @PostMapping("/threads/{threadId}/posts")
    @PreAuthorize("isAuthenticated()")
    public DiscussionPost addPost(@PathVariable Long threadId, @RequestBody Map<String, String> body) {
        DiscussionThread th = loadThread(threadId);
        topicAccess.requireMember(th.getTopic());

        User user = scope.requireCurrentUser();
        DiscussionPost p = new DiscussionPost();
        p.setThread(th);
        p.setAuthor(user);
        p.setContent(body.get("content"));
        DiscussionPost saved = postRepo.save(p);
        auditLogService.log("CREATE_POST", "DiscussionPost", saved.getId().toString(), "Thread " + th.getId());
        events.genericEvent("POST_ADDED", saved.getId());
        return saved;
    }

    private DiscussionThread loadThread(Long threadId) {
        return threadRepo.findById(threadId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy chủ đề thảo luận"));
    }
}
