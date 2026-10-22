package com.doanltmmt.Backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Yêu cầu mã đặt lại mật khẩu: tên đăng nhập hoặc email. */
public record ForgotPasswordRequest(
        @NotBlank(message = "Vui lòng nhập tên đăng nhập hoặc email") @Size(max = 255) String identifier
) {
}
