package com.doanltmmt.Backend.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Set;

/** Sửa mô tả và danh sách quyền của vai trò; {@code privilegeIds} không gửi → giữ nguyên quyền. */
public record RoleUpdateRequest(
        @Size(max = 255, message = "Mô tả tối đa 255 ký tự") String description,
        @FieldLabel("Danh sách quyền (privilegeIds)")
        Set<@NotNull(message = "privilegeIds không hợp lệ") @Positive(message = "privilegeIds không hợp lệ") Long> privilegeIds) {

    public RoleUpdateRequest {
        description = Texts.trim(description);
    }
}
