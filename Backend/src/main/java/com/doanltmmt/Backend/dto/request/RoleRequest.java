package com.doanltmmt.Backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Tạo vai trò / quyền hạn: tên (định dạng vai trò kiểm tra ở RoleService) và mô tả. */
public record RoleRequest(
        @NotBlank(message = "Tên không được để trống") @Size(max = 100, message = "Tên tối đa 100 ký tự") String name,
        @Size(max = 255, message = "Mô tả tối đa 255 ký tự") String description) {

    public RoleRequest {
        name = Texts.trim(name);
        description = Texts.blankToNull(description);
    }
}
