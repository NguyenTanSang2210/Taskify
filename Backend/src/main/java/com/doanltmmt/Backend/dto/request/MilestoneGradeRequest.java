package com.doanltmmt.Backend.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** P4-05: giảng viên chấm điểm một mốc cho một sinh viên. */
public record MilestoneGradeRequest(
        @FieldLabel("Điểm") @NotNull(message = "Điểm là bắt buộc")
        @DecimalMin(value = "0", message = "Điểm phải từ 0 đến 10") @DecimalMax(value = "10", message = "Điểm phải từ 0 đến 10") Double score,
        @Size(max = 5000, message = "Nhận xét tối đa 5000 ký tự") String feedback) {

    public MilestoneGradeRequest {
        feedback = Texts.blankToNull(feedback);
    }
}
