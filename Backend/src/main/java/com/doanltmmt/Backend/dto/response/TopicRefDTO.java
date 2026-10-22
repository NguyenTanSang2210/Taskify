package com.doanltmmt.Backend.dto.response;

import com.doanltmmt.Backend.entity.Topic;

public record TopicRefDTO(Long id, String title) {
    public static TopicRefDTO from(Topic t) {
        return t == null ? null : new TopicRefDTO(t.getId(), t.getTitle());
    }
}
