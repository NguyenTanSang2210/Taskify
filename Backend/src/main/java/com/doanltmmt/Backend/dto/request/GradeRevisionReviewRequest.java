package com.doanltmmt.Backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;

/**
 * Khoa duyệt / từ chối yêu cầu chấm lại. Duyệt: {@code unlockHours} 1–168 (mặc định 48), {@code note} tùy chọn.
 * Từ chối: {@code note} (lý do) bắt buộc — kiểm tra theo nhóm {@link Reject}.
 */
public record GradeRevisionReviewRequest(
        @FieldLabel("Số giờ mở khóa")
        @Min(value = 1, message = "Số giờ mở khóa phải từ 1 đến 168")
        @Max(value = 168, message = "Số giờ mở khóa phải từ 1 đến 168") Integer unlockHours,
        @NotBlank(groups = Reject.class, message = "Lý do từ chối không được để trống")
        @Size(max = 1000, groups = {Default.class, Reject.class}, message = "Ghi chú tối đa 1000 ký tự") String note) {

    public interface Reject {
    }

    public GradeRevisionReviewRequest {
        note = Texts.blankToNull(note);
    }

    public static GradeRevisionReviewRequest empty() {
        return new GradeRevisionReviewRequest(null, null);
    }
}
