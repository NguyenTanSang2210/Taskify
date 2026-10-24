package com.doanltmmt.Backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** P2-13 — mỗi lần chấm hoặc sửa điểm: ai, khi nào, điểm cũ → mới, lý do, theo yêu cầu chấm lại nào. */
@Entity
@Table(name = "grade_history", indexes = @Index(name = "idx_grade_history_reg", columnList = "registration_id"))
@Getter
@Setter
public class GradeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "registration_id", nullable = false)
    private TopicRegistration registration;

    @Column(name = "old_score")
    private Double oldScore;

    @Column(name = "new_score", nullable = false)
    private Double newScore;

    @Column(name = "old_feedback", columnDefinition = "TEXT")
    private String oldFeedback;

    @Column(name = "new_feedback", columnDefinition = "TEXT")
    private String newFeedback;

    @ManyToOne(optional = false)
    @JoinColumn(name = "changed_by", nullable = false)
    private User changedBy;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @ManyToOne
    @JoinColumn(name = "revision_request_id")
    private GradeRevisionRequest revisionRequest;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @PrePersist
    void prePersist() {
        if (changedAt == null) changedAt = LocalDateTime.now();
    }
}
