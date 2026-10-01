package com.doanltmmt.Backend.dto.response;

import com.doanltmmt.Backend.entity.Announcement;
import java.time.LocalDateTime;

public record AnnouncementDTO(Long id, String title, String content, LocalDateTime createdAt, TopicRefDTO topic,
                              UserSummaryDTO author) {
    public static AnnouncementDTO from(Announcement a) {
        return a == null ? null : new AnnouncementDTO(a.getId(), a.getTitle(), a.getContent(), a.getCreatedAt(),
                TopicRefDTO.from(a.getTopic()), UserSummaryDTO.from(a.getAuthor()));
    }
}
