package com.doanltmmt.Backend.dto.response;

import com.doanltmmt.Backend.entity.ProgressReport;
import java.time.LocalDateTime;

public record ProgressReportDTO(Long id, String title, String content, String status, String fileUrl,
                                LocalDateTime deadline, LocalDateTime createdAt, String lecturerComment,
                                TopicRefDTO topic, StudentSummaryDTO student, MilestoneRefDTO milestone) {
    public static ProgressReportDTO from(ProgressReport p) {
        return p == null ? null : new ProgressReportDTO(p.getId(), p.getTitle(), p.getContent(), p.getStatus(),
                p.getFileUrl(), p.getDeadline(), p.getCreatedAt(), p.getLecturerComment(),
                TopicRefDTO.from(p.getTopic()), StudentSummaryDTO.from(p.getStudent()),
                MilestoneRefDTO.from(p.getMilestone()));
    }
}
