package com.doanltmmt.Backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Chấm điểm một đăng ký. {@code score} nhận chuỗi hoặc số ("7,5" / 7.5); thang 0–10, bước 0,25 kiểm tra ở
 * {@code GradeService}. {@code reason} bắt buộc khi sửa điểm đã có.
 */
public record GradeRequest(
        @NotBlank(message = "Vui lòng nhập điểm") String score,
        @Size(max = 5000, message = "Nhận xét tối đa 5000 ký tự") String feedback,
        @Size(max = 2000, message = "Lý do chỉnh sửa tối đa 2000 ký tự") String reason) {

    public GradeRequest {
        score = Texts.trim(score);
        feedback = Texts.blankToNull(feedback);
        reason = Texts.blankToNull(reason);
    }
}
