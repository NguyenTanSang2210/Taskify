package com.doanltmmt.Backend.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Thu hồi một phân công giảng viên. */
public record LecturerRevokeRequest(
        @NotNull(message = "assignmentId là bắt buộc") @Positive(message = "assignmentId không hợp lệ") Long assignmentId,
        @Size(max = 255, message = "Lý do tối đa 255 ký tự") String reason) {

    public LecturerRevokeRequest {
        reason = Texts.blankToNull(reason);
    }
}
