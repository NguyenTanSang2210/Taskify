package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.entity.ProgressReport;
import com.doanltmmt.Backend.entity.TopicRegistration;
import com.doanltmmt.Backend.entity.Notification;
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
        payload.put("progress", pr);
        String dest = "/topic/progress/" + pr.getTopic().getId();
        template.convertAndSend(dest, payload);
    }

    public void progressUpdated(ProgressReport pr) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "PROGRESS_UPDATED");
        payload.put("progress", pr);
        String dest = "/topic/progress/" + pr.getTopic().getId();
        template.convertAndSend(dest, payload);
    }

    public void registrationChanged(TopicRegistration reg, String action) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", action);
        payload.put("registration", reg);
        String dest = "/topic/registration/" + reg.getTopic().getId();
        template.convertAndSend(dest, payload);
    }

    public void notificationCreated(Notification n) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "NOTIFICATION_CREATED");
        payload.put("notification", n);
        String dest = "/topic/notifications/" + n.getUser().getId();
        template.convertAndSend(dest, payload);
    }

    public void genericEvent(String type, Long id) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", type);
        payload.put("id", id);
        String dest = "/topic/events";
        template.convertAndSend(dest, payload);
    }
}
