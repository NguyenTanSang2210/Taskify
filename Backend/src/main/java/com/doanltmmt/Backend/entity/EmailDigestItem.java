package com.doanltmmt.Backend.entity;

import com.doanltmmt.Backend.entity.enums.EmailCategory;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/** P4-06: một email thông báo đang chờ gửi trong thư tổng hợp hằng ngày (nội dung đã chuyển sang văn bản thuần). */
@Entity
@Table(name = "email_digest_items")
@Getter
@Setter
public class EmailDigestItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 20, nullable = false)
    private EmailCategory category;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
