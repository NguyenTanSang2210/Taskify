package com.doanltmmt.Backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * P2-13 (D-08) — yêu cầu mở khóa điểm của một sinh viên khi đợt đã đóng.
 * {@link #PENDING} → {@link #APPROVED} (mở khóa đến {@code unlockUntil}) → {@link #USED} khi GV sửa điểm,
 * hoặc {@link #EXPIRED} khi hết hạn; {@link #REJECTED} nếu bị từ chối.
 */
@Entity
@Table(name = "grade_revision_requests",
        indexes = @Index(name = "idx_grade_revision_reg_status", columnList = "registration_id, status"))
@Getter
@Setter
public class GradeRevisionRequest {

    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";
    public static final String USED = "USED";
    public static final String EXPIRED = "EXPIRED";

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

    @Column(length = 20, nullable = false)
    private String status = PENDING;

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
