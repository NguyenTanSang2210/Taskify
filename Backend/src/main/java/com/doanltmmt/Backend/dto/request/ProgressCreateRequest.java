package com.doanltmmt.Backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** Sinh viên nộp báo cáo tiến độ. {@code fileUrl}: file đã tải lên qua /api/upload; {@code milestoneId} tùy chọn. */
public record ProgressCreateRequest(
        @NotBlank(message = "Tiêu đề báo cáo không được để trống")
        @Size(max = 200, message = "Tiêu đề báo cáo tối đa 200 ký tự") String title,
        @NotBlank(message = "Nội dung báo cáo không được để trống")
        @Size(max = 20_000, message = "Nội dung báo cáo tối đa 20000 ký tự") String content,
        @Size(max = 500, message = "Đường dẫn file không hợp lệ") String fileUrl,
        @FieldLabel("Hạn chót") LocalDateTime deadline,
        @Positive(message = "milestoneId không hợp lệ") Long milestoneId) {

    public ProgressCreateRequest {
        title = Texts.trim(title);
        content = Texts.trim(content);
        fileUrl = Texts.blankToNull(fileUrl);
    }
}
