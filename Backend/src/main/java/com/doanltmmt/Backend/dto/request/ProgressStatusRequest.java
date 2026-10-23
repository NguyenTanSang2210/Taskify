package com.doanltmmt.Backend.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Sinh viên đổi trạng thái báo cáo: TODO / IN_PROGRESS / DONE (kiểm tra ở ProgressStatus.parse). */
public record ProgressStatusRequest(@NotBlank(message = "Trạng thái là bắt buộc") String status) {
}
