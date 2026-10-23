package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.dto.request.MessageRequest;
import com.doanltmmt.Backend.dto.response.*;
import com.doanltmmt.Backend.entity.Message;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.repository.MessageRepository;
import com.doanltmmt.Backend.repository.UserRepository;
import com.doanltmmt.Backend.service.AuditLogService;
import com.doanltmmt.Backend.service.EventPublisher;
import com.doanltmmt.Backend.service.SecurityScopeService;
import com.doanltmmt.Backend.service.TopicAccessService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tin nhắn trong phạm vi một đề tài: cả NGƯỜI GỬI lẫn NGƯỜI NHẬN đều phải là thành viên của đề tài
 * (trước đây gửi được cho bất kỳ người dùng nào trong hệ thống).
 */
@RestController
@RequestMapping("/api/messages")
@SuppressWarnings("null")
public class MessageController {


    private final MessageRepository repo;
    private final UserRepository userRepo;
    private final AuditLogService auditLogService;
    private final EventPublisher events;
    private final SecurityScopeService scope;
    private final TopicAccessService topicAccess;

    public MessageController(MessageRepository repo,
                             UserRepository userRepo,
                             AuditLogService auditLogService,
                             EventPublisher events,
                             SecurityScopeService scope,
                             TopicAccessService topicAccess) {
        this.repo = repo;
        this.userRepo = userRepo;
        this.auditLogService = auditLogService;
        this.events = events;
        this.scope = scope;
        this.topicAccess = topicAccess;
    }

    @GetMapping("/inbox")
    @PreAuthorize("isAuthenticated()")
    public List<Map<String, Object>> inbox() {
        User me = scope.requireCurrentUser();
        // Lỗi CSDL để DataAccessErrorHandler xử lý (trước đây bị nuốt và trả danh sách rỗng, che mất sự cố)
        List<Map<String, Object>> result = new ArrayList<>();
        for (MessageRepository.InboxRow row : repo.findInboxRowsByUserId(me.getId())) {
            Map<String, Object> sender = new HashMap<>();
            sender.put("id", row.getSenderId());
            sender.put("fullName", row.getSenderName());

            Map<String, Object> recipient = new HashMap<>();
            recipient.put("id", row.getRecipientId());

            Map<String, Object> item = new HashMap<>();
            item.put("id", row.getId());
            item.put("sender", sender);
            item.put("recipient", recipient);
            item.put("topic", row.getTopicId() == null ? null : Map.of("id", row.getTopicId()));
            item.put("content", row.getContent());
            item.put("createdAt", row.getCreatedAt());
            result.add(item);
        }
        return result;
    }

    @PostMapping("/send")
    @PreAuthorize("isAuthenticated()")
    public MessageDTO send(@Valid @RequestBody MessageRequest body) {
        User me = scope.requireCurrentUser();
        Long recipientId = body.recipientId();
        Long topicId = body.topicId();
        String content = body.content();

        Topic topic = topicAccess.loadTopicOr404(topicId);
        if (topic.getWorkspace() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đề tài chưa thuộc đợt nào.");
        }
        User recipient = userRepo.findById(recipientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người nhận."));

        // Cả hai đầu đều phải thuộc đề tài: chặn nhắn tin cho người ngoài đề tài
        topicAccess.requireMember(topic);
        if (!topicAccess.isMember(topic, recipient)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Người nhận không thuộc đề tài này.");
        }

        Message m = new Message();
        m.setSender(me);
        m.setRecipient(recipient);
        m.setContent(content);
        m.setTopic(topic);
        m.setWorkspace(topic.getWorkspace());
        Message saved = repo.save(m);
        events.messageCreated(saved);
        auditLogService.log("SEND_MESSAGE", "Message", saved.getId().toString(), "To: " + recipient.getUsername());
        return MessageDTO.from(saved);
    }
}
