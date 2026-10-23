package com.doanltmmt.Backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Thông báo: có {@code topicId} → thông báo của đề tài; không có → toàn hệ thống (chỉ ADMIN). */
public record AnnouncementRequest(
        @Positive(message = "topicId không hợp lệ") Long topicId,
        @NotBlank(message = "Tiêu đề thông báo không được để trống")
        @Size(max = 255, message = "Tiêu đề thông báo tối đa 255 ký tự") String title,
        @NotBlank(message = "Nội dung thông báo không được để trống")
        @Size(max = 5000, message = "Nội dung thông báo tối đa 5000 ký tự") String content) {

    public AnnouncementRequest {
        title = Texts.trim(title);
        content = Texts.trim(content);
    }
}
