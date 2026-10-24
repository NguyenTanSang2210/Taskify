package com.doanltmmt.Backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Quản trị viên tạo tài khoản. Không có {@code password} → gửi mã kích hoạt qua email (khi đó email bắt buộc).
 * Sinh viên cần {@code studentCode} và lớp ({@code academicClassId} hoặc {@code className}); giảng viên có thể kèm
 * {@code degree}/{@code speciality}. Các ràng buộc phụ thuộc vai trò kiểm tra ở UserController.
 */
public record UserCreateRequest(
        @NotBlank(message = "Tên đăng nhập là bắt buộc") @Size(max = 50, message = "Tên đăng nhập tối đa 50 ký tự") String username,
        @Size(max = 128, message = "Mật khẩu tối đa 128 ký tự") String password,
        @NotBlank(message = "Họ tên là bắt buộc") @Size(max = 100, message = "Họ tên tối đa 100 ký tự") String fullName,
        @Size(max = 100, message = "Email tối đa 100 ký tự") String email,
        @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự") String phone,
        @Positive(message = "roleId không hợp lệ") Long roleId,
        @Positive(message = "departmentId không hợp lệ") Long departmentId,
        @FieldLabel("Trạng thái hoạt động") Boolean active,
        @Size(max = 50, message = "Mã sinh viên tối đa 50 ký tự") String studentCode,
        @Size(max = 100, message = "Tên lớp tối đa 100 ký tự") String className,
        @Positive(message = "academicClassId không hợp lệ") Long academicClassId,
        @Size(max = 100, message = "Học vị tối đa 100 ký tự") String degree,
        @Size(max = 255, message = "Chuyên ngành tối đa 255 ký tự") String speciality) {

    public UserCreateRequest {
        username = Texts.trim(username);
        password = Texts.blankToNull(password);
        fullName = Texts.trim(fullName);
        email = Texts.blankToNull(email);
        phone = Texts.blankToNull(phone);
        studentCode = Texts.blankToNull(studentCode);
        className = Texts.blankToNull(className);
        degree = Texts.blankToNull(degree);
        speciality = Texts.blankToNull(speciality);
    }
}
