package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.dto.ActivateRequest;
import com.doanltmmt.Backend.dto.ForgotPasswordRequest;
import com.doanltmmt.Backend.dto.InitialPasswordRequest;
import com.doanltmmt.Backend.dto.LoginRequest;
import com.doanltmmt.Backend.dto.LoginResponse;
import com.doanltmmt.Backend.dto.OtpResendRequest;
import com.doanltmmt.Backend.dto.OtpResendResponse;
import com.doanltmmt.Backend.dto.OtpVerifyRequest;
import com.doanltmmt.Backend.dto.ResetPasswordRequest;
import com.doanltmmt.Backend.security.SessionCookies;
import com.doanltmmt.Backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Xác thực. Logic đăng nhập nhiều bước nằm ở {@link AuthService}; controller chỉ nhận request, lấy IP client
 * và trả kết quả.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final SessionCookies sessions;

    public AuthController(AuthService authService, SessionCookies sessions) {
        this.authService = authService;
        this.sessions = sessions;
    }

    /** Bước 1: tên đăng nhập + mật khẩu. Vai trò cần OTP chỉ nhận token tạm, chưa gọi được API nghiệp vụ. */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http, HttpServletResponse res) {
        return sessions.start(authService.login(request, clientIp(http)), http, res);
    }

    /** Bước 2: nhập OTP. Người dùng được xác định từ preAuthToken, không nhận username từ client. */
    @PostMapping("/otp/verify")
    public LoginResponse verifyOtp(@Valid @RequestBody OtpVerifyRequest request, HttpServletRequest http, HttpServletResponse res) {
        return sessions.start(authService.verifyOtp(request.preAuthToken(), request.code(), clientIp(http)), http, res);
    }

    /** Gửi lại OTP (chờ tối thiểu 60 giây giữa hai lần; mã cũ bị hủy). */
    @PostMapping("/otp/resend")
    public OtpResendResponse resendOtp(@Valid @RequestBody OtpResendRequest request, HttpServletRequest http) {
        return authService.resendOtp(request.preAuthToken(), clientIp(http));
    }

    /** Đăng xuất mọi thiết bị: tăng tokenVersion → mọi token đang lưu hành của người dùng này lập tức vô hiệu. */
    @PostMapping("/logout-all")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> logoutAll(HttpServletResponse res) {
        authService.logoutAll(SecurityContextHolder.getContext().getAuthentication().getName());
        sessions.clear(res);
        return Map.of("loggedOut", true);
    }

    /**
     * P3-14: đổi refresh token (cookie HttpOnly) lấy access token mới; cookie được xoay vòng. Gọi khi access token
     * hết hạn hoặc khi tải lại trang (access token chỉ giữ trong bộ nhớ).
     */
    @PostMapping("/refresh")
    public LoginResponse refresh(HttpServletRequest http, HttpServletResponse res) {
        return sessions.refresh(http, res);
    }

    /** P3-14: đăng xuất phiên hiện tại — thu hồi refresh token và xóa cookie (không cần access token còn hạn). */
    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletRequest http, HttpServletResponse res) {
        sessions.logout(http, res);
        return Map.of("loggedOut", true);
    }

    /** Đổi mật khẩu bắt buộc lần đầu (dùng token PWD_CHANGE nhận ở bước đăng nhập). */
    @PostMapping("/password/initial")
    public LoginResponse changeInitialPassword(@Valid @RequestBody InitialPasswordRequest request, HttpServletRequest http,
                                               HttpServletResponse res) {
        return sessions.start(authService.changeInitialPassword(request.preAuthToken(), request.newPassword(), clientIp(http)), http, res);
    }

    /** Kích hoạt tài khoản do quản trị tạo: tên đăng nhập/email + mã nhận qua email + mật khẩu mới. */
    @PostMapping("/activate")
    public Map<String, Object> activate(@Valid @RequestBody ActivateRequest request, HttpServletRequest http) {
        authService.activate(request.identifier(), request.code(), request.newPassword(), clientIp(http));
        return Map.of("activated", true);
    }

    /** Quên mật khẩu — gửi mã qua email. Luôn trả cùng một thông báo (không tiết lộ tài khoản có tồn tại hay không). */
    @PostMapping("/password/forgot")
    public Map<String, Object> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest http) {
        authService.requestPasswordReset(request.identifier(), clientIp(http));
        return Map.of("message", AuthService.RESET_REQUESTED);
    }

    /** Quên mật khẩu — đặt mật khẩu mới bằng mã nhận qua email; mọi phiên đăng nhập cũ bị thu hồi. */
    @PostMapping("/password/reset")
    public Map<String, Object> resetPassword(@Valid @RequestBody ResetPasswordRequest request, HttpServletRequest http) {
        authService.resetPassword(request.identifier(), request.code(), request.newPassword(), clientIp(http));
        return Map.of("reset", true);
    }

    /** IP client (đã qua {@code server.forward-headers-strategy=native} nên là IP thật khi đứng sau nginx). */
    private static String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
