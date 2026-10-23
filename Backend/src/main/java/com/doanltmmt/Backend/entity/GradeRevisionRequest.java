package com.doanltmmt.Backend.entity;

import com.doanltmmt.Backend.entity.enums.GradeRevisionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * P2-13 (D-08) — yêu cầu mở khóa điểm của một sinh viên khi đợt đã đóng (vòng đời: {@link GradeRevisionStatus}).
 */
@Entity
@Table(name = "grade_revision_requests",
        indexes = @Index(name = "idx_grade_revision_reg_status", columnList = "registration_id, status"))
@Getter
@Setter
public class GradeRevisionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "registration_id", nullable = false)
    private TopicRegistration registration;

    @ManyToOne(optional = false)
    @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR) // cột varchar (không dùng kiểu ENUM của MySQL) để thêm giá trị mới không phải ALTER
    @Column(length = 20, nullable = false)
    private GradeRevisionStatus status = GradeRevisionStatus.PENDING;

    @ManyToOne
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_note", length = 1000)
    private String reviewNote;

    @Column(name = "unlock_until")
    private LocalDateTime unlockUntil;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
