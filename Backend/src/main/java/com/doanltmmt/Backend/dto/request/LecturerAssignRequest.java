package com.doanltmmt.Backend.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.Locale;

/** Phân công giảng viên cho đợt. {@code type}: MAIN (mặc định) hoặc ASSISTANT; {@code quotaMaxGroups} 0 = không giới hạn. */
public record LecturerAssignRequest(
        @NotNull(message = "workspaceId là bắt buộc") @Positive(message = "workspaceId không hợp lệ") Long workspaceId,
        @NotNull(message = "lecturerId là bắt buộc") @Positive(message = "lecturerId không hợp lệ") Long lecturerId,
        @Pattern(regexp = "MAIN|ASSISTANT", message = "Loại phân công không hợp lệ (MAIN hoặc ASSISTANT).") String type,
        @FieldLabel("Hạn mức nhóm") @PositiveOrZero(message = "Hạn mức nhóm không được âm") Integer quotaMaxGroups,
        @Size(max = 255, message = "Lý do tối đa 255 ký tự") String reason) {

    public LecturerAssignRequest {
        type = type == null || type.isBlank() ? "MAIN" : type.trim().toUpperCase(Locale.ROOT);
        quotaMaxGroups = quotaMaxGroups == null ? 0 : quotaMaxGroups;
        reason = Texts.blankToNull(reason);
    }
}
