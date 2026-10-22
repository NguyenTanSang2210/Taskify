package com.doanltmmt.Backend.dto;

import com.doanltmmt.Backend.entity.User;

/**
 * Kết quả một bước của luồng đăng nhập.
 *
 * <p>{@code nextStep}:
 * <ul>
 *   <li>{@code NONE} — đăng nhập xong: có {@code token} (access token) và thông tin người dùng.</li>
 *   <li>{@code OTP} — cần nhập mã OTP: chỉ có {@code preAuthToken} (không gọi được API nghiệp vụ), {@code maskedEmail}.</li>
 *   <li>{@code CHANGE_PASSWORD} — đang dùng mật khẩu tạm, phải đặt mật khẩu mới: chỉ có {@code preAuthToken}.</li>
 * </ul>
 * Khi {@code nextStep != NONE}: {@code token}, {@code userId}, {@code fullName}, {@code role} đều rỗng.
 * {@code otpRequired} giữ lại để tương thích với frontend cũ.
 */
public record LoginResponse(
        String token,
        String preAuthToken,
        String nextStep,
        boolean otpRequired,
        boolean mustChangePassword,
        Long userId,
        String username,
        String fullName,
        String role,
        String maskedEmail,
        Long expiresInSeconds,
        Integer resendCooldownSeconds
) {

    public static final String STEP_NONE = "NONE";
    public static final String STEP_OTP = "OTP";
    public static final String STEP_CHANGE_PASSWORD = "CHANGE_PASSWORD";

    public static LoginResponse authenticated(String accessToken, User user, String roleName) {
        return new LoginResponse(accessToken, null, STEP_NONE, false, false,
                user.getId(), user.getUsername(), user.getFullName(), roleName, null, null, null);
    }

    public static LoginResponse otpRequired(String preAuthToken, User user, long expiresInSeconds,
                                            String maskedEmail, int resendCooldownSeconds) {
        return new LoginResponse(null, preAuthToken, STEP_OTP, true, false,
                null, user.getUsername(), null, null, maskedEmail, expiresInSeconds, resendCooldownSeconds);
    }

    public static LoginResponse passwordChangeRequired(String preAuthToken, User user, long expiresInSeconds) {
        return new LoginResponse(null, preAuthToken, STEP_CHANGE_PASSWORD, false, true,
                null, user.getUsername(), null, null, null, expiresInSeconds, null);
    }
}
