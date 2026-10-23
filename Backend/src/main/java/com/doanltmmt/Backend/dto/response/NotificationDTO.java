package com.doanltmmt.Backend.dto.response;

import com.doanltmmt.Backend.entity.enums.Severity;
import com.doanltmmt.Backend.entity.enums.NotificationType;
import com.doanltmmt.Backend.entity.Notification;
import java.time.LocalDateTime;

public record NotificationDTO(Long id, NotificationType type, String title, String content, LocalDateTime dueAt,
                              LocalDateTime readAt, LocalDateTime createdAt, Severity severity, String refType,
                              Long refId, String linkPath) {
    public static NotificationDTO from(Notification n) {
        return n == null ? null : new NotificationDTO(n.getId(), n.getType(), n.getTitle(), n.getContent(),
                n.getDueAt(), n.getReadAt(), n.getCreatedAt(), n.getSeverity(), n.getRefType(), n.getRefId(),
                n.getLinkPath());
    }
}
