package com.doanltmmt.Backend.entity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "progress_report")
@Getter
@Setter
public class ProgressReport {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String title;

    @NotBlank
    @Size(max = 5000)
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @ManyToOne
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "created_at", nullable = false)
    private java.time.LocalDateTime createdAt;

    @Size(max = 5000)
    @Column(columnDefinition = "TEXT")
    private String lecturerComment;

    @Column(length = 20, nullable = false)
    private String status; // TODO, IN_PROGRESS, DONE

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Column(name = "deadline")
    private java.time.LocalDateTime deadline;

    @ManyToOne
    @JoinColumn(name = "milestone_id")
    private Milestone milestone;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = java.time.LocalDateTime.now();
        }
        if (status == null || status.isBlank()) {
            status = "TODO";
        }
    }
}
