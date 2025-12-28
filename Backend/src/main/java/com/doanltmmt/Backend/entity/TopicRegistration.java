package com.doanltmmt.Backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "topic_registration")
@Getter
@Setter
public class TopicRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt;

    @Column(nullable = true)
    private Boolean approved; // null: chờ duyệt, true: đã duyệt, false: từ chối

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    // Nhật ký duyệt/từ chối
    @ManyToOne
    @JoinColumn(name = "reviewer_id")
    private Lecturer reviewer; // giảng viên thực hiện duyệt/từ chối

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "score")
    private Double score;

    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;
}
