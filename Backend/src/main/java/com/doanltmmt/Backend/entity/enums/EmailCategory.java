package com.doanltmmt.Backend.entity.enums;

/** P4-06: nhóm email thông báo người dùng tự bật/tắt. Email bảo mật (OTP, kích hoạt, đặt lại mật khẩu) không thuộc nhóm nào. */
public enum EmailCategory {
    REGISTRATION("Kết quả đăng ký đề tài"),
    PROGRESS("Báo cáo tiến độ & nhận xét"),
    GRADE("Điểm & chấm lại"),
    REMINDER("Nhắc hạn tự động");

    private final String label;

    EmailCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
