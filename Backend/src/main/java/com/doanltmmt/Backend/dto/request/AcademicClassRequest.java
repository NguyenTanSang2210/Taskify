package com.doanltmmt.Backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Tạo/sửa lớp. {@code departmentId} chỉ ADMIN được chọn (quản trị khoa luôn tạo trong khoa của mình).
 * Khi sửa, trường bỏ trống được giữ nguyên.
 */
public record AcademicClassRequest(
        @Positive(message = "departmentId không hợp lệ") Long departmentId,
        @NotBlank(groups = OnCreate.class, message = "Mã lớp không được để trống")
        @Size(max = 50, message = "Mã lớp tối đa 50 ký tự") String code,
        @NotBlank(groups = OnCreate.class, message = "Tên lớp không được để trống")
        @Size(max = 255, message = "Tên lớp tối đa 255 ký tự") String name,
        @FieldLabel("Trạng thái hoạt động") Boolean active) {

    public AcademicClassRequest {
        code = Texts.blankToNull(code);
        name = Texts.blankToNull(name);
    }
}
