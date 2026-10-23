package com.doanltmmt.Backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Mở chủ đề thảo luận trong đề tài. */
public record DiscussionThreadRequest(
        @NotNull(message = "topicId là bắt buộc") @Positive(message = "topicId không hợp lệ") Long topicId,
        @NotBlank(message = "Tiêu đề chủ đề không được để trống")
        @Size(max = 255, message = "Tiêu đề chủ đề tối đa 255 ký tự") String title) {

    public DiscussionThreadRequest {
        title = Texts.trim(title);
    }
}
