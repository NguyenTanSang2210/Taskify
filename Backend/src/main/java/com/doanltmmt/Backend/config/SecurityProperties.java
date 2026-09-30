package com.doanltmmt.Backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Ngưỡng giới hạn tần suất và khóa đăng nhập. Giá trị mặc định là mức AN TOÀN cho production;
 * profile dev nới lỏng (xem application-dev.properties) để kiểm thử/Swagger thủ công không bị chặn.
 *
 * <pre>
 * app.security.rate.login-per-minute=10            # theo IP
 * app.security.login.max-failures=5                # theo cặp (username, IP)
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    private final Rate rate = new Rate();
    private final Login login = new Login();

    public Rate getRate() {
        return rate;
    }

    public Login getLogin() {
        return login;
    }

    /** Giới hạn số request theo IP (cửa sổ cố định, lưu trong RAM). */
    public static class Rate {
        private int loginPerMinute = 10;
        private int otpVerifyPerMinute = 10;
        private int otpResendPer10Minutes = 5;
        private int activatePer10Minutes = 10;
        private int uploadPer10Minutes = 30;

        public int getLoginPerMinute() { return loginPerMinute; }
        public void setLoginPerMinute(int v) { this.loginPerMinute = v; }
        public int getOtpVerifyPerMinute() { return otpVerifyPerMinute; }
        public void setOtpVerifyPerMinute(int v) { this.otpVerifyPerMinute = v; }
        public int getOtpResendPer10Minutes() { return otpResendPer10Minutes; }
        public void setOtpResendPer10Minutes(int v) { this.otpResendPer10Minutes = v; }
        public int getActivatePer10Minutes() { return activatePer10Minutes; }
        public void setActivatePer10Minutes(int v) { this.activatePer10Minutes = v; }
        public int getUploadPer10Minutes() { return uploadPer10Minutes; }
        public void setUploadPer10Minutes(int v) { this.uploadPer10Minutes = v; }
    }

    /** Khóa tạm theo cặp (username, IP) — lưu DB nên bền qua restart. */
    public static class Login {
        private int maxFailures = 5;
        private int windowMinutes = 15;
        private int lockMinutes = 10;

        public int getMaxFailures() { return maxFailures; }
        public void setMaxFailures(int v) { this.maxFailures = v; }
        public int getWindowMinutes() { return windowMinutes; }
        public void setWindowMinutes(int v) { this.windowMinutes = v; }
        public int getLockMinutes() { return lockMinutes; }
        public void setLockMinutes(int v) { this.lockMinutes = v; }
    }
}
