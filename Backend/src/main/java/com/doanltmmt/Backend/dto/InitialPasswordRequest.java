package com.doanltmmt.Backend.dto;

import jakarta.validation.constraints.NotBlank;

/** Đổi mật khẩu bắt buộc lần đầu (đang dùng mật khẩu tạm). Người dùng lấy từ preAuthToken phạm vi PWD_CHANGE. */
public record InitialPasswordRequest(
        @NotBlank(message = "Thiếu phiên xác thực") String preAuthToken,
        @NotBlank(message = "Vui lòng nhập mật khẩu mới") String newPassword
) {
}
