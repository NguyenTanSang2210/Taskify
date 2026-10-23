package com.doanltmmt.Backend.config;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.TimeZone;

/**
 * Múi giờ của ứng dụng — mặc định giờ Việt Nam, đổi bằng biến môi trường {@code APP_TIME_ZONE}.
 *
 * <p>Mọi {@code LocalDateTime.now()} (thời điểm tạo, hạn OTP, lịch nhắc, mở khóa điểm…) và giờ trả cho giao diện đều
 * theo múi giờ này. Trong CSDL thời gian vẫn lưu theo <b>UTC</b>: chuỗi kết nối dùng {@code serverTimezone=UTC}, driver
 * MySQL tự quy đổi khi ghi/đọc — nên dữ liệu cũ không phải chuyển đổi. (Trước đây container Docker chạy JVM theo UTC nên
 * giao diện hiện giờ UTC, lệch 7 tiếng.)
 */
public final class AppTimeZone {

    public static final String DEFAULT = "Asia/Ho_Chi_Minh";

    private AppTimeZone() {
    }

    /** Đặt múi giờ mặc định của JVM — gọi ở {@code main} trước khi Spring khởi động. */
    public static ZoneId apply() {
        String configured = System.getProperty("APP_TIME_ZONE", System.getenv("APP_TIME_ZONE"));
        String id = configured == null || configured.isBlank() ? DEFAULT : configured.trim();
        ZoneId zone;
        try {
            zone = ZoneId.of(id);
        } catch (DateTimeException e) {
            throw new IllegalStateException("APP_TIME_ZONE không hợp lệ: '" + id + "' (ví dụ đúng: " + DEFAULT + ")", e);
        }
        TimeZone.setDefault(TimeZone.getTimeZone(zone));
        return zone;
    }
}
