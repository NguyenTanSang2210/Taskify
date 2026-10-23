package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.dto.request.MessageRequest;
import com.doanltmmt.Backend.dto.response.*;
import com.doanltmmt.Backend.entity.Message;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.repository.MessageRepository;
import com.doanltmmt.Backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Tin nhắn riêng giữa các thành viên của cùng một đề tài. */
@Service
@SuppressWarnings("null")
public class MessageService {

    private final MessageRepository repo;
    private final UserRepository userRepo;
    private final AuditLogService auditLogService;
    private final EventPublisher events;
    private final SecurityScopeService scope;
    private final TopicAccessService topicAccess;

    public MessageService(MessageRepository repo,
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

    @Transactional(readOnly = true)
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

    @Transactional
    public MessageDTO send(MessageRequest body) {
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
