package com.doanltmmt.Backend.entity;

import com.doanltmmt.Backend.entity.enums.NotificationType;
import com.doanltmmt.Backend.entity.enums.Severity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR) // cột varchar (không dùng kiểu ENUM của MySQL) để thêm giá trị mới không phải ALTER
    @Column(length = 50, nullable = false)
    private NotificationType type = NotificationType.INFO;

    @Column(length = 200, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDateTime dueAt;

    private LocalDateTime readAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR) // cột varchar (không dùng kiểu ENUM của MySQL) để thêm giá trị mới không phải ALTER
    @Column(length = 30)
    private Severity severity;

    @Column(length = 50)
    private String refType; // PROGRESS_REPORT, TOPIC_REGISTRATION, TOPIC, USER

    private Long refId;

    @Column(length = 255)
    private String linkPath; // frontend path để điều hướng nhanh

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (severity == null) severity = Severity.INFO;
        if (type == null) type = NotificationType.INFO;
    }
}
