package com.doanltmmt.Backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Giảng viên xin mở khóa điểm để chấm lại. */
public record GradeRevisionRequestBody(
        @NotBlank(message = "Lý do chấm lại không được để trống")
        @Size(max = 2000, message = "Lý do chấm lại tối đa 2000 ký tự") String reason) {

    public GradeRevisionRequestBody {
        reason = Texts.trim(reason);
    }
}
