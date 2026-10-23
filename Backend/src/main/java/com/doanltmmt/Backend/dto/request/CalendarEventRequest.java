package com.doanltmmt.Backend.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** Sự kiện lịch của đề tài. {@code type}: DEADLINE/MEETING/... (sai → mặc định, xem CalendarEventType). */
public record CalendarEventRequest(
        @NotNull(message = "topicId là bắt buộc") @Positive(message = "topicId không hợp lệ") Long topicId,
        @NotBlank(message = "Tiêu đề sự kiện không được để trống")
        @Size(max = 255, message = "Tiêu đề sự kiện tối đa 255 ký tự") String title,
        @Size(max = 5000, message = "Mô tả tối đa 5000 ký tự") String description,
        @FieldLabel("Thời gian bắt đầu") LocalDateTime startTime,
        @FieldLabel("Thời gian kết thúc") LocalDateTime endTime,
        String type) {

    public CalendarEventRequest {
        title = Texts.trim(title);
        description = Texts.blankToNull(description);
    }

    @AssertTrue(message = "Thời gian kết thúc phải sau thời gian bắt đầu")
    boolean isTimeRangeValid() {
        return startTime == null || endTime == null || !endTime.isBefore(startTime);
    }
}
