package com.doanltmmt.Backend.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/** Đổi mật khẩu khi đã đăng nhập. Không cắt khoảng trắng mật khẩu; chính sách mật khẩu kiểm tra ở PasswordPolicy. */
public record ChangePasswordRequest(
        @NotEmpty(message = "Vui lòng nhập mật khẩu hiện tại") @Size(max = 128, message = "Mật khẩu hiện tại không đúng") String oldPassword,
        @NotEmpty(message = "Vui lòng nhập mật khẩu mới") @Size(max = 128, message = "Mật khẩu tối đa 128 ký tự") String newPassword) {
}
