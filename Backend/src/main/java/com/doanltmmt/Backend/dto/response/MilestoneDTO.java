package com.doanltmmt.Backend.dto.response;

import com.doanltmmt.Backend.entity.Milestone;
import java.time.LocalDateTime;

public record MilestoneDTO(Long id, String title, String description, LocalDateTime startDate,
                           LocalDateTime deadline, LocalDateTime createdAt, TopicRefDTO topic) {
    public static MilestoneDTO from(Milestone m) {
        return m == null ? null : new MilestoneDTO(m.getId(), m.getTitle(), m.getDescription(), m.getStartDate(),
                m.getDeadline(), m.getCreatedAt(), TopicRefDTO.from(m.getTopic()));
    }
}
