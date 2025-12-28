package com.doanltmmt.Backend.repository;

import com.doanltmmt.Backend.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUser_IdOrderByCreatedAtDesc(Long userId);
    long countByUser_IdAndReadAtIsNull(Long userId);
    List<Notification> findByUser_IdAndReadAtIsNull(Long userId);
}
