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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = "http://localhost:5173")
@SuppressWarnings("null")
public class MessageController {

    private static final Logger log = LoggerFactory.getLogger(MessageController.class);

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
    public List<Map<String, Object>> inbox() {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User me = userRepo.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        try {
            List<MessageRepository.InboxRow> rows = repo.findInboxRowsByUserId(me.getId());
            List<Map<String, Object>> result = new ArrayList<>();
            for (MessageRepository.InboxRow row : rows) {
                Map<String, Object> sender = new HashMap<>();
                sender.put("id", row.getSenderId());
                sender.put("fullName", row.getSenderName());

                Map<String, Object> recipient = new HashMap<>();
                recipient.put("id", row.getRecipientId());

                Map<String, Object> item = new HashMap<>();
                item.put("id", row.getId());
                item.put("sender", sender);
                item.put("recipient", recipient);
                item.put("content", row.getContent());
                item.put("createdAt", row.getCreatedAt());
                result.add(item);
            }
            return result;
        } catch (RuntimeException ex) {
            log.warn("Failed to load inbox for user {}. Returning empty list. Cause: {}", me.getId(), ex.getMessage());
            return List.of();
        }
    }

    @PostMapping("/send")
    @PreAuthorize("isAuthenticated()")
    public Message send(@RequestBody Map<String, String> body) {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User me = userRepo.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Long recipientId = Long.valueOf(body.get("recipientId"));
        User recipient = userRepo.findById(recipientId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipient not found"));
        String topicIdRaw = body.get("topicId");
        if (topicIdRaw == null || topicIdRaw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "topicId is required");
        }

        Topic topic = topicRepo.findById(Long.valueOf(topicIdRaw))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
        if (topic.getWorkspace() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic has no workspace");
        }

        Message m = new Message();
        m.setSender(me);
        m.setRecipient(recipient);
        m.setContent(body.get("content"));
        if (scope.hasRole("DEPARTMENT_ADMIN") && !scope.hasRole("ADMIN")) {
            if (topic.getWorkspace().getDepartment() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic has no workspace/department");
            }
            scope.requireDepartmentAccess(topic.getWorkspace().getDepartment().getId());
        }

        m.setTopic(topic);
        m.setWorkspace(topic.getWorkspace());
        Message saved = repo.save(m);
        events.messageCreated(saved);
        auditLogService.log("SEND_MESSAGE", "Message", saved.getId().toString(), "To: " + recipient.getUsername());
        events.genericEvent("MESSAGE_SENT", saved.getId());
        return saved;
    }
}
