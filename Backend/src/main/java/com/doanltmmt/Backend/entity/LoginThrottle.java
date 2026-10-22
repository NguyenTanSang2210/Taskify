package com.doanltmmt.Backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Bộ đếm đăng nhập sai theo cặp (username, IP). Khóa là SHA-256 của "username|ip" nên có độ dài cố định
 * (username do kẻ tấn công tự chọn có thể rất dài) và không lưu dữ liệu định danh dạng thô.
 *
 * <p>Khóa theo CẶP nên kẻ xấu gõ sai mật khẩu từ IP của họ không khóa được tài khoản của nạn nhân khi nạn nhân
 * đăng nhập từ IP khác. Lưu DB nên trạng thái khóa còn nguyên sau khi restart backend.
 */
@Entity
@Table(name = "login_throttle")
@Getter
@Setter
@NoArgsConstructor
public class LoginThrottle {

    @Id
    @Column(name = "throttle_key", length = 64)
    private String throttleKey;

    /** Số lần sai trong cửa sổ hiện tại. */
    @Column(nullable = false)
    private int failures;

    @Column(name = "window_start", nullable = false)
    private LocalDateTime windowStart;

    /** Đang bị khóa tới thời điểm này (null = không khóa). */
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    public LoginThrottle(String throttleKey, LocalDateTime windowStart) {
        this.throttleKey = throttleKey;
        this.windowStart = windowStart;
    }
}
