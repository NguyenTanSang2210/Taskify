package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.dto.TopicRegistrationDTO;
import com.doanltmmt.Backend.dto.response.MessageDTO;
import com.doanltmmt.Backend.dto.response.NotificationDTO;
import com.doanltmmt.Backend.dto.response.ProgressReportDTO;
import com.doanltmmt.Backend.entity.ProgressReport;
import com.doanltmmt.Backend.entity.TopicRegistration;
import com.doanltmmt.Backend.entity.Notification;
import com.doanltmmt.Backend.entity.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

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
        template.convertAndSend(dest, payload);
    }

    public void progressUpdated(ProgressReport pr) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "PROGRESS_UPDATED");
        payload.put("progress", ProgressReportDTO.from(pr));
        String dest = "/topic/progress/" + pr.getTopic().getId();
        template.convertAndSend(dest, payload);
    }

    public void registrationChanged(TopicRegistration reg, String action) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", action);
        payload.put("registration", new TopicRegistrationDTO(reg));
        String dest = "/topic/registration/" + reg.getTopic().getId();
        template.convertAndSend(dest, payload);
    }

    public void notificationCreated(Notification n) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "NOTIFICATION_CREATED");
        payload.put("notification", NotificationDTO.from(n));
        // Kênh riêng theo Principal: chỉ chính người dùng đó nhận được (/user/queue/notifications)
        template.convertAndSendToUser(n.getUser().getUsername(), "/queue/notifications", payload);
    }

    public void messageCreated(Message m) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "MESSAGE_CREATED");
        payload.put("message", MessageDTO.from(m));

        template.convertAndSendToUser(m.getSender().getUsername(), "/queue/messages", payload);
        if (!m.getSender().getId().equals(m.getRecipient().getId())) {
            template.convertAndSendToUser(m.getRecipient().getUsername(), "/queue/messages", payload);
        }
    }
}
