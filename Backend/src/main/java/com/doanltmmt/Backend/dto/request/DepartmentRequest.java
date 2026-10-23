package com.doanltmmt.Backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Tạo/sửa khoa. Khi sửa, trường bỏ trống được giữ nguyên. */
public record DepartmentRequest(
        @NotBlank(groups = OnCreate.class, message = "Mã khoa không được để trống")
        @Size(max = 50, message = "Mã khoa tối đa 50 ký tự") String code,
        @NotBlank(groups = OnCreate.class, message = "Tên khoa không được để trống")
        @Size(max = 255, message = "Tên khoa tối đa 255 ký tự") String name,
        @FieldLabel("Trạng thái hoạt động") Boolean active) {

    public DepartmentRequest {
        code = Texts.blankToNull(code);
        name = Texts.blankToNull(name);
    }
}
