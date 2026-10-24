package com.doanltmmt.Backend.dto.request;

import jakarta.validation.constraints.NotNull;

/** P4-06: cài đặt email thông báo của người dùng hiện tại. */
public record NotificationSettingsRequest(
        @NotNull(message = "Thiếu chế độ gửi (emailDigest)") Boolean emailDigest,
        @NotNull(message = "Thiếu cài đặt nhóm Kết quả đăng ký đề tài") Boolean registration,
        @NotNull(message = "Thiếu cài đặt nhóm Báo cáo tiến độ & nhận xét") Boolean progress,
        @NotNull(message = "Thiếu cài đặt nhóm Điểm & chấm lại") Boolean grade,
        @NotNull(message = "Thiếu cài đặt nhóm Nhắc hạn tự động") Boolean reminder) {
}
