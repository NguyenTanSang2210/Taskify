package com.doanltmmt.Backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Bài viết trả lời trong chủ đề thảo luận. */
public record DiscussionPostRequest(
        @NotBlank(message = "Nội dung bài viết không được để trống")
        @Size(max = 5000, message = "Nội dung bài viết tối đa 5000 ký tự") String content) {

    public DiscussionPostRequest {
        content = Texts.trim(content);
    }
}
