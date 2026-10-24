package com.doanltmmt.Backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** P4-05: điểm của một sinh viên cho một mốc (milestone) — mỗi cặp (mốc, sinh viên) một bản ghi. */
@Entity
@Table(name = "milestone_grades", uniqueConstraints = @UniqueConstraint(name = "uk_milestone_grade", columnNames = {"milestone_id", "student_id"}))
@Getter
@Setter
public class MilestoneGrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "milestone_id", nullable = false)
    private Milestone milestone;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false)
    private Double score;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graded_by_id")
    private User gradedBy;

    @Column(name = "graded_at", nullable = false)
    private LocalDateTime gradedAt;
}
