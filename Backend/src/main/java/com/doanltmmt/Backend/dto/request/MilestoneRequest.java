package com.doanltmmt.Backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Tạo/sửa mốc tiến độ. Khi sửa: trường không gửi (hoặc {@code null}) giữ nguyên; {@code description: ""} xóa mô tả.
 * Thứ tự ngày và việc nằm trong thời gian của đợt kiểm tra ở MilestoneController (cần giá trị cũ và đợt).
 */
public record MilestoneRequest(
        @NotNull(groups = OnCreate.class, message = "topicId là bắt buộc") @Positive(message = "topicId không hợp lệ") Long topicId,
        @NotBlank(groups = OnCreate.class, message = "Tên mốc không được để trống")
        @Size(min = 1, message = "Tên mốc không được để trống")
        @Size(max = 255, message = "Tên mốc tối đa 255 ký tự") String title,
        @Size(max = 5000, message = "Mô tả tối đa 5000 ký tự") String description,
        @FieldLabel("Ngày bắt đầu") LocalDateTime startDate,
        @FieldLabel("Hạn chót") @NotNull(groups = OnCreate.class, message = "Hạn chót là bắt buộc") LocalDateTime deadline,
        @FieldLabel("Trọng số") @Min(value = 0, message = "Trọng số từ 0 đến 100") @Max(value = 100, message = "Trọng số từ 0 đến 100") Integer weight) {

    public MilestoneRequest {
        title = Texts.trim(title);
        description = Texts.trim(description);
    }
}
