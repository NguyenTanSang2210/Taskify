package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.dto.TopicRegistrationDTO;
import com.doanltmmt.Backend.dto.response.MessageDTO;
import com.doanltmmt.Backend.dto.response.NotificationDTO;
import com.doanltmmt.Backend.dto.response.ProgressReportDTO;
import com.doanltmmt.Backend.entity.Message;
import com.doanltmmt.Backend.entity.Notification;
import com.doanltmmt.Backend.entity.ProgressReport;
import com.doanltmmt.Backend.entity.TopicRegistration;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Đẩy sự kiện realtime qua WebSocket. Payload dựng ngay (còn trong transaction), nhưng chỉ GỬI sau khi transaction
 * commit — client nhận sự kiện rồi tải lại sẽ thấy dữ liệu mới, và không có sự kiện "ma" khi transaction rollback.
 */
@Service
public class EventPublisher {
    private final SimpMessagingTemplate template;

    public EventPublisher(SimpMessagingTemplate template) {
        this.template = template;
    }

    public void progressCreated(ProgressReport pr) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "PROGRESS_CREATED");
        payload.put("progress", ProgressReportDTO.from(pr));
        String dest = "/topic/progress/" + pr.getTopic().getId();
        send(dest, payload);
    }

    public void progressUpdated(ProgressReport pr) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "PROGRESS_UPDATED");
        payload.put("progress", ProgressReportDTO.from(pr));
        String dest = "/topic/progress/" + pr.getTopic().getId();
        send(dest, payload);
    }

    public void registrationChanged(TopicRegistration reg, String action) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", action);
        payload.put("registration", new TopicRegistrationDTO(reg));
        String dest = "/topic/registration/" + reg.getTopic().getId();
        send(dest, payload);
    }

    public void notificationCreated(Notification n) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "NOTIFICATION_CREATED");
        payload.put("notification", NotificationDTO.from(n));
        // Kênh riêng theo Principal: chỉ chính người dùng đó nhận được (/user/queue/notifications)
        String username = n.getUser().getUsername();
        afterCommit(() -> template.convertAndSendToUser(username, "/queue/notifications", payload));
    }

    public void messageCreated(Message m) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "MESSAGE_CREATED");
        payload.put("message", MessageDTO.from(m));

        String sender = m.getSender().getUsername();
        String recipient = m.getSender().getId().equals(m.getRecipient().getId()) ? null : m.getRecipient().getUsername();
        afterCommit(() -> {
            template.convertAndSendToUser(sender, "/queue/messages", payload);
            if (recipient != null) template.convertAndSendToUser(recipient, "/queue/messages", payload);
        });
    }

    private void send(String destination, Object payload) {
        afterCommit(() -> template.convertAndSend(destination, payload));
    }

    private static void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
