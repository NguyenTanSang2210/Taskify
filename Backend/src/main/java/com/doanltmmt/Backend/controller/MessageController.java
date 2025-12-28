package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.entity.Message;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.repository.MessageRepository;
import com.doanltmmt.Backend.repository.TopicRepository;
import com.doanltmmt.Backend.repository.UserRepository;
import com.doanltmmt.Backend.service.AuditLogService;
import com.doanltmmt.Backend.service.EventPublisher;
import com.doanltmmt.Backend.service.SecurityScopeService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = "http://localhost:5173")
public class MessageController {

    private final MessageRepository repo;
    private final UserRepository userRepo;
    private final TopicRepository topicRepo;
    private final AuditLogService auditLogService;
    private final EventPublisher events;
    private final SecurityScopeService scope;

    public MessageController(MessageRepository repo,
                             UserRepository userRepo,
                             TopicRepository topicRepo,
                             AuditLogService auditLogService,
                             EventPublisher events,
                             SecurityScopeService scope) {
        this.repo = repo;
        this.userRepo = userRepo;
        this.topicRepo = topicRepo;
        this.auditLogService = auditLogService;
        this.events = events;
        this.scope = scope;
    }

    @GetMapping("/inbox")
    @PreAuthorize("isAuthenticated()")
    public List<Message> inbox() {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User me = userRepo.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return repo.findBySender_IdOrRecipient_IdOrderByCreatedAtDesc(me.getId(), me.getId());
    }

    @PostMapping("/send")
    @PreAuthorize("isAuthenticated()")
    public Message send(@RequestBody Map<String, String> body) {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User me = userRepo.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Long recipientId = Long.valueOf(body.get("recipientId"));
        User recipient = userRepo.findById(recipientId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipient not found"));
        Message m = new Message();
        m.setSender(me);
        m.setRecipient(recipient);
        m.setContent(body.get("content"));
        if (body.get("topicId") != null) {
            Topic topic = topicRepo.findById(Long.valueOf(body.get("topicId")))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
            if (scope.hasRole("DEPARTMENT_ADMIN") && !scope.hasRole("ADMIN")) {
                if (topic.getWorkspace() == null || topic.getWorkspace().getDepartment() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic has no workspace/department");
                }
                scope.requireDepartmentAccess(topic.getWorkspace().getDepartment().getId());
            }
            m.setTopic(topic);
        }
        Message saved = repo.save(m);
        auditLogService.log("SEND_MESSAGE", "Message", saved.getId().toString(), "To: " + recipient.getUsername());
        events.genericEvent("MESSAGE_SENT", saved.getId());
        return saved;
    }
}
