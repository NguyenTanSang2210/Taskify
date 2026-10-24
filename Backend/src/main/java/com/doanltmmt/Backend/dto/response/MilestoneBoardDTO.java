package com.doanltmmt.Backend.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * P4-05 — bảng mốc của đề tài: mỗi sinh viên × mỗi mốc là một ô (đã nộp chưa, đúng hạn hay trễ, điểm).
 * {@code weightedScore}: điểm đề xuất = Σ điểm × trọng số / Σ trọng số của các mốc có trọng số ĐÃ chấm;
 * {@code complete} = mọi mốc có trọng số đều đã chấm. Sinh viên chỉ thấy dòng của mình.
 */
public record MilestoneBoardDTO(List<MilestoneDTO> milestones, int weightTotal, boolean canGrade, List<Row> students) {

    public record Cell(Long milestoneId, Long reportId, LocalDateTime submittedAt, boolean late, Double score, String feedback) {
    }

    public record Row(Long studentId, String fullName, String studentCode, List<Cell> cells, Double weightedScore, boolean complete) {
    }
}
