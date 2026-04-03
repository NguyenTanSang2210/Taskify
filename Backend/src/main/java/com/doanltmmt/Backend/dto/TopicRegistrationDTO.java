package com.doanltmmt.Backend.dto;

import com.doanltmmt.Backend.entity.TopicRegistration;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TopicRegistrationDTO {
    private Long id;
    private Boolean approved;
    private String rejectReason;
    private LocalDateTime registeredAt;
    private LocalDateTime reviewedAt;
    private Double score;
    private String feedback;
    private TopicDTO topic;
    private String studentCode;
    private String studentName;
    private Long studentId;
    private Long reviewerId;
    private String reviewerName;

    public TopicRegistrationDTO(TopicRegistration reg) {
        if (reg == null) return;
        this.id = reg.getId();
        this.approved = reg.getApproved();
        this.rejectReason = reg.getRejectReason();
        this.registeredAt = reg.getRegisteredAt();
        this.reviewedAt = reg.getReviewedAt();
        this.score = reg.getScore();
        this.feedback = reg.getFeedback();
        
        if (reg.getTopic() != null) {
            this.topic = new TopicDTO(reg.getTopic());
        }
        
        if (reg.getStudent() != null) {
            this.studentId = reg.getStudent().getId();
            this.studentCode = reg.getStudent().getStudentCode();
            if (reg.getStudent().getUser() != null) {
                this.studentName = reg.getStudent().getUser().getFullName();
            }
        }
        
        if (reg.getReviewer() != null) {
            this.reviewerId = reg.getReviewer().getId();
            if (reg.getReviewer().getUser() != null) {
                this.reviewerName = reg.getReviewer().getUser().getFullName();
            }
        }
    }
}
