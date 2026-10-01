package com.doanltmmt.Backend.dto.response;

import com.doanltmmt.Backend.entity.DiscussionThread;
import java.time.LocalDateTime;

public record DiscussionThreadDTO(Long id, String title, LocalDateTime createdAt, TopicRefDTO topic,
                                  UserSummaryDTO createdBy) {
    public static DiscussionThreadDTO from(DiscussionThread t) {
        return t == null ? null : new DiscussionThreadDTO(t.getId(), t.getTitle(), t.getCreatedAt(),
                TopicRefDTO.from(t.getTopic()), UserSummaryDTO.from(t.getCreatedBy()));
    }
}
