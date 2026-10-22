package com.doanltmmt.Backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Bước 2 đăng nhập: username lấy TỪ preAuthToken (không nhận từ client) nên không thể thử OTP của người khác. */
public record OtpVerifyRequest(
        @NotBlank(message = "Thiếu phiên xác thực") String preAuthToken,
        @NotBlank(message = "Vui lòng nhập mã OTP") @Size(max = 10, message = "Mã OTP không hợp lệ") String code
) {
}
