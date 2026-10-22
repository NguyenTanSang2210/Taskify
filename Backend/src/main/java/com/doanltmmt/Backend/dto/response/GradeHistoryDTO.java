package com.doanltmmt.Backend.dto.response;

import com.doanltmmt.Backend.entity.GradeHistory;

import java.time.LocalDateTime;

public record GradeHistoryDTO(Long id, Double oldScore, Double newScore, String oldFeedback, String newFeedback,
                              String changedByName, String reason, Long revisionRequestId, LocalDateTime changedAt) {

    public static GradeHistoryDTO from(GradeHistory h) {
        return new GradeHistoryDTO(h.getId(), h.getOldScore(), h.getNewScore(), h.getOldFeedback(), h.getNewFeedback(),
                h.getChangedBy() != null ? h.getChangedBy().getFullName() : null, h.getReason(),
                h.getRevisionRequest() != null ? h.getRevisionRequest().getId() : null, h.getChangedAt());
    }
}
