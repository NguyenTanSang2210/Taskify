package com.doanltmmt.Backend.dto.response;

import com.doanltmmt.Backend.entity.enums.ProgressStatus;
import com.doanltmmt.Backend.entity.ProgressReport;
import java.time.LocalDateTime;

/**
 * Báo cáo tiến độ. {@code similarityScore}/{@code similarToReportId} chỉ có ở bản dành cho người chấm
 * ({@link #forReviewer}); sinh viên nhận {@code null} để không lộ báo cáo của bạn khác.
 */
public record ProgressReportDTO(Long id, String title, String content, ProgressStatus status, String fileUrl,
                                LocalDateTime deadline, LocalDateTime createdAt, String lecturerComment,
                                TopicRefDTO topic, StudentSummaryDTO student, MilestoneRefDTO milestone,
                                Double similarityScore, Long similarToReportId) {
    public static ProgressReportDTO from(ProgressReport p) {
        return of(p, false);
    }

    public static ProgressReportDTO forReviewer(ProgressReport p) {
        return of(p, true);
    }

    private static ProgressReportDTO of(ProgressReport p, boolean reviewer) {
        return p == null ? null : new ProgressReportDTO(p.getId(), p.getTitle(), p.getContent(), p.getStatus(),
                p.getFileUrl(), p.getDeadline(), p.getCreatedAt(), p.getLecturerComment(),
                TopicRefDTO.from(p.getTopic()), StudentSummaryDTO.from(p.getStudent()),
                MilestoneRefDTO.from(p.getMilestone()),
                reviewer ? p.getSimilarityScore() : null, reviewer ? p.getSimilarToReportId() : null);
    }
}
