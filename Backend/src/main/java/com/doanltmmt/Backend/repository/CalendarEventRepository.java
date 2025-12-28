package com.doanltmmt.Backend.repository;

import com.doanltmmt.Backend.entity.CalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {
    List<CalendarEvent> findByTopic_IdOrderByStartTimeAsc(Long topicId);
}
