package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.entity.*;
import com.doanltmmt.Backend.repository.*;
import com.doanltmmt.Backend.service.EventPublisher;
import com.doanltmmt.Backend.service.SecurityScopeService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/discuss")
@CrossOrigin(origins = "http://localhost:5173")
@SuppressWarnings("null")
public class DiscussionController {

    private final DiscussionThreadRepository threadRepo;
    private final DiscussionPostRepository postRepo;
    private final TopicRepository topicRepo;
    private final UserRepository userRepo;
    private final com.doanltmmt.Backend.service.AuditLogService auditLogService;
    private final EventPublisher events;
    private final SecurityScopeService scope;

    public DiscussionController(DiscussionThreadRepository threadRepo,
                                DiscussionPostRepository postRepo,
                                TopicRepository topicRepo,
                                UserRepository userRepo,
                                com.doanltmmt.Backend.service.AuditLogService auditLogService,
                                EventPublisher events,
                                SecurityScopeService scope) {
        this.threadRepo = threadRepo;
        this.postRepo = postRepo;
        this.topicRepo = topicRepo;
        this.userRepo = userRepo;
        this.auditLogService = auditLogService;
        this.events = events;
        this.scope = scope;
    }

    @GetMapping("/threads")
    @PreAuthorize("isAuthenticated()")
    public List<DiscussionThread> listThreads(@RequestParam Long topicId) {
        if (scope.hasRole("DEPARTMENT_ADMIN") && !scope.hasRole("ADMIN")) {
            Topic topic = topicRepo.findById(topicId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
            if (topic.getWorkspace() == null || topic.getWorkspace().getDepartment() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic has no workspace/department");
            }
            scope.requireDepartmentAccess(topic.getWorkspace().getDepartment().getId());
        }
        return threadRepo.findByTopic_IdOrderByCreatedAtDesc(topicId);
    }

    @PostMapping("/threads")
    @PreAuthorize("isAuthenticated()")
    public DiscussionThread createThread(@RequestBody Map<String, String> body) {
        Long topicId = Long.valueOf(body.get("topicId"));
        Topic topic = topicRepo.findById(topicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
        if (scope.hasRole("DEPARTMENT_ADMIN") && !scope.hasRole("ADMIN")) {
            if (topic.getWorkspace() == null || topic.getWorkspace().getDepartment() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic has no workspace/department");
            }
            scope.requireDepartmentAccess(topic.getWorkspace().getDepartment().getId());
        }
        User user = userRepo.findByUsername(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

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
        if (scope.hasRole("DEPARTMENT_ADMIN") && !scope.hasRole("ADMIN")) {
            DiscussionThread th = threadRepo.findById(threadId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Thread not found"));
            if (th.getTopic() == null || th.getTopic().getWorkspace() == null || th.getTopic().getWorkspace().getDepartment() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thread has no topic/workspace/department");
            }
            scope.requireDepartmentAccess(th.getTopic().getWorkspace().getDepartment().getId());
        }
        return postRepo.findByThread_IdOrderByCreatedAtAsc(threadId);
    }

    @PostMapping("/threads/{threadId}/posts")
    @PreAuthorize("isAuthenticated()")
    public DiscussionPost addPost(@PathVariable Long threadId, @RequestBody Map<String, String> body) {
        DiscussionThread th = threadRepo.findById(threadId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Thread not found"));
        if (scope.hasRole("DEPARTMENT_ADMIN") && !scope.hasRole("ADMIN")) {
            if (th.getTopic() == null || th.getTopic().getWorkspace() == null || th.getTopic().getWorkspace().getDepartment() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thread has no topic/workspace/department");
            }
            scope.requireDepartmentAccess(th.getTopic().getWorkspace().getDepartment().getId());
        }
        User user = userRepo.findByUsername(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        DiscussionPost p = new DiscussionPost();
        p.setThread(th);
        p.setAuthor(user);
        p.setContent(body.get("content"));
        DiscussionPost saved = postRepo.save(p);
        auditLogService.log("CREATE_POST", "DiscussionPost", saved.getId().toString(), "Thread " + th.getId());
        events.genericEvent("POST_ADDED", saved.getId());
        return saved;
    }
}


