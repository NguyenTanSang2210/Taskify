package com.doanltmmt.Backend.dto.response;

import com.doanltmmt.Backend.entity.enums.CalendarEventType;
import com.doanltmmt.Backend.entity.CalendarEvent;
import java.time.LocalDateTime;

public record CalendarEventDTO(Long id, String title, String description, LocalDateTime startTime,
                               LocalDateTime endTime, CalendarEventType type, TopicRefDTO topic) {
    public static CalendarEventDTO from(CalendarEvent e) {
        return e == null ? null : new CalendarEventDTO(e.getId(), e.getTitle(), e.getDescription(),
                e.getStartTime(), e.getEndTime(), e.getType(), TopicRefDTO.from(e.getTopic()));
    }
}
