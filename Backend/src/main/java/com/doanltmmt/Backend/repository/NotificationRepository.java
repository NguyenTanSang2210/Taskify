package com.doanltmmt.Backend.repository;

import com.doanltmmt.Backend.entity.Notification;
import com.doanltmmt.Backend.entity.enums.NotificationType;
import com.doanltmmt.Backend.entity.enums.Severity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    /** Chống nhắc trùng: đã có thông báo cùng loại cho cùng đối tượng kể từ {@code since}. */
    boolean existsByUser_IdAndTypeAndRefTypeAndRefIdAndSeverityAndCreatedAtAfter(
            Long userId, NotificationType type, String refType, Long refId, Severity severity, LocalDateTime since);
    Page<Notification> findByUser_Id(Long userId, Pageable pageable);
    long countByUser_IdAndReadAtIsNull(Long userId);
    List<Notification> findByUser_IdAndReadAtIsNull(Long userId);
}
