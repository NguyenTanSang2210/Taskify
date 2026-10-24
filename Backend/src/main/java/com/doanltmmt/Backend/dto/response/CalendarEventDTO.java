package com.doanltmmt.Backend.dto.response;

import com.doanltmmt.Backend.entity.enums.CalendarEventType;
import com.doanltmmt.Backend.entity.CalendarEvent;
import com.doanltmmt.Backend.entity.Milestone;
import java.time.LocalDateTime;

public record CalendarEventDTO(Long id, String title, String description, LocalDateTime startTime,
                               LocalDateTime endTime, CalendarEventType type, TopicRefDTO topic, Long milestoneId) {
    public static CalendarEventDTO from(CalendarEvent e) {
        return e == null ? null : new CalendarEventDTO(e.getId(), e.getTitle(), e.getDescription(),
                e.getStartTime(), e.getEndTime(), e.getType(), TopicRefDTO.from(e.getTopic()), null);
    }

    /** P4-05: mốc của đề tài hiện trên lịch như một sự kiện MILESTONE (chỉ đọc — sửa ở bảng mốc). */
    public static CalendarEventDTO fromMilestone(Milestone m) {
        return new CalendarEventDTO(null, m.getTitle(), m.getDescription(), m.getStartDate() != null ? m.getStartDate() : m.getDeadline(),
                m.getDeadline(), CalendarEventType.MILESTONE, TopicRefDTO.from(m.getTopic()), m.getId());
    }
}
