package com.doanltmmt.Backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Tạo/sửa đợt đồ án. {@code departmentId} bắt buộc với ADMIN (quản trị khoa luôn tạo trong khoa mình — kiểm tra
 * ở WorkspaceService; thứ tự ngày kiểm tra ở đó vì khi sửa phải gộp với giá trị cũ). Khi sửa: trường không gửi được giữ nguyên; tên/loại/học kỳ/thời gian chỉ sửa khi đợt là bản nháp.
 */
public record WorkspaceRequest(
        @Positive(message = "Khoa (departmentId) không hợp lệ") Long departmentId,
        @NotBlank(groups = OnCreate.class, message = "Tên đợt không được để trống")
        @Size(max = 255, message = "Tên đợt tối đa 255 ký tự") String name,
        @Size(max = 50, message = "Loại đợt tối đa 50 ký tự") String type,
        @Size(max = 50, message = "Học kỳ tối đa 50 ký tự") String semester,
        @FieldLabel("Trạng thái hoạt động") Boolean active,
        @FieldLabel("Ngày bắt đầu") LocalDateTime startAt,
        @FieldLabel("Ngày kết thúc") LocalDateTime endAt) {

    public WorkspaceRequest {
        name = Texts.blankToNull(name);
        type = Texts.trim(type);
        semester = Texts.trim(semester);
    }

    /** Có sửa thông tin chỉ được đổi khi đợt còn là bản nháp không. */
    public boolean touchesDraftOnlyFields() {
        return name != null || type != null || semester != null || startAt != null || endAt != null;
    }
}
