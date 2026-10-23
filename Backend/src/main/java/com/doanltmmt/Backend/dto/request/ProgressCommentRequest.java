package com.doanltmmt.Backend.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** Giảng viên nhận xét / đổi trạng thái / đặt hạn chót cho báo cáo — phải có ít nhất một trong ba. */
public record ProgressCommentRequest(
        @Size(max = 5000, message = "Nhận xét tối đa 5000 ký tự") String lecturerComment,
        String status,
        @FieldLabel("Hạn chót") LocalDateTime deadline) {

    public ProgressCommentRequest {
        lecturerComment = Texts.blankToNull(lecturerComment);
        status = Texts.blankToNull(status);
    }

    @AssertTrue(message = "Không có nội dung nào để cập nhật")
    boolean isNotEmpty() {
        return lecturerComment != null || status != null || deadline != null;
    }
}
