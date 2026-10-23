package com.doanltmmt.Backend.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Gán / bỏ gán một lớp cho đợt. */
public record WorkspaceClassRequest(
        @NotNull(message = "workspaceId là bắt buộc") @Positive(message = "workspaceId không hợp lệ") Long workspaceId,
        @NotNull(message = "classId là bắt buộc") @Positive(message = "classId không hợp lệ") Long classId) {
}
