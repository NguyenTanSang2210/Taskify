package com.doanltmmt.Backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Tin nhắn riêng trong phạm vi một đề tài (người gửi và người nhận đều phải là thành viên đề tài). */
public record MessageRequest(
        @NotNull(message = "Người nhận (recipientId) là bắt buộc")
        @Positive(message = "Người nhận (recipientId) không hợp lệ") Long recipientId,
        @NotNull(message = "topicId là bắt buộc") @Positive(message = "topicId không hợp lệ") Long topicId,
        @NotBlank(message = "Nội dung tin nhắn không được để trống")
        @Size(max = 5000, message = "Tin nhắn quá dài (tối đa 5000 ký tự)") String content) {

    public MessageRequest {
        content = Texts.trim(content);
    }
}
