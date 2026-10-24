package com.doanltmmt.Backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Đặt lại mật khẩu bằng mã nhận qua email. */
public record ResetPasswordRequest(
        @NotBlank(message = "Vui lòng nhập tên đăng nhập hoặc email") @Size(max = 255) String identifier,
        @NotBlank(message = "Vui lòng nhập mã xác nhận") @Size(max = 10, message = "Mã xác nhận không hợp lệ") String code,
        @NotBlank(message = "Vui lòng nhập mật khẩu mới") @Size(max = 255) String newPassword
) {
}
