package com.doanltmmt.Backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Kích hoạt tài khoản do quản trị tạo: tên đăng nhập (hoặc email) + mã nhận qua email + mật khẩu mới. */
public record ActivateRequest(
        @NotBlank(message = "Vui lòng nhập tên đăng nhập hoặc email") @Size(max = 255) String identifier,
        @NotBlank(message = "Vui lòng nhập mã kích hoạt") @Size(max = 10, message = "Mã kích hoạt không hợp lệ") String code,
        @NotBlank(message = "Vui lòng nhập mật khẩu mới") String newPassword
) {
}
