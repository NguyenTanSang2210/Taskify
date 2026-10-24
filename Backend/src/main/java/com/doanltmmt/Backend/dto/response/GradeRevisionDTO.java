package com.doanltmmt.Backend.dto.response;

import com.doanltmmt.Backend.entity.enums.GradeRevisionStatus;
import com.doanltmmt.Backend.entity.GradeRevisionRequest;
import com.doanltmmt.Backend.entity.TopicRegistration;
import com.doanltmmt.Backend.entity.User;

import java.time.LocalDateTime;

/** Yêu cầu chấm lại kèm thông tin đủ để người duyệt quyết định. */
public record GradeRevisionDTO(Long id, Long registrationId, Long topicId, String topicTitle, String workspaceName,
                               String studentName, String studentCode, Double currentScore,
                               String requestedByName, String reason, GradeRevisionStatus status,
                               String reviewedByName, LocalDateTime reviewedAt, String reviewNote,
                               LocalDateTime unlockUntil, LocalDateTime createdAt) {

    public static GradeRevisionDTO from(GradeRevisionRequest r) {
        if (r == null) return null;
        TopicRegistration reg = r.getRegistration();
        var topic = reg.getTopic();
        var student = reg.getStudent();
        return new GradeRevisionDTO(r.getId(), reg.getId(), topic.getId(), topic.getTitle(),
                topic.getWorkspace() != null ? topic.getWorkspace().getName() : null,
                student.getUser() != null ? student.getUser().getFullName() : null, student.getStudentCode(),
                reg.getScore(), nameOf(r.getRequestedBy()), r.getReason(), r.getStatus(),
                nameOf(r.getReviewedBy()), r.getReviewedAt(), r.getReviewNote(), r.getUnlockUntil(), r.getCreatedAt());
    }

    private static String nameOf(User u) {
        return u == null ? null : u.getFullName();
    }
}
