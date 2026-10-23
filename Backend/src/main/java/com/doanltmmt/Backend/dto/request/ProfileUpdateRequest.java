package com.doanltmmt.Backend.dto.request;

import jakarta.validation.constraints.Size;

/** Sửa hồ sơ cá nhân; trường không gửi được giữ nguyên, gửi chuỗi rỗng → lỗi. Định dạng email kiểm tra ở UserController. */
public record ProfileUpdateRequest(
        @Size(min = 1, message = "Họ tên không được để trống")
        @Size(max = 100, message = "Họ tên tối đa 100 ký tự") String fullName,
        @Size(min = 1, message = "Email không được để trống")
        @Size(max = 100, message = "Email tối đa 100 ký tự") String email) {

    public ProfileUpdateRequest {
        fullName = Texts.trim(fullName);
        email = Texts.trim(email);
    }
}
