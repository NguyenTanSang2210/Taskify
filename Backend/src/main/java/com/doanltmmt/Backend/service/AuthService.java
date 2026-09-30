package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.config.SecurityProperties;
import com.doanltmmt.Backend.dto.LoginRequest;
import com.doanltmmt.Backend.dto.LoginResponse;
import com.doanltmmt.Backend.dto.OtpResendResponse;
import com.doanltmmt.Backend.entity.OtpPurpose;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.repository.UserRepository;
import com.doanltmmt.Backend.security.JwtTokenProvider;
import com.doanltmmt.Backend.security.JwtTokenProvider.Scope;
import com.doanltmmt.Backend.security.JwtTokenProvider.TokenClaims;
import com.doanltmmt.Backend.security.RateLimiter;
import com.doanltmmt.Backend.service.OtpService.VerifyOutcome;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * Luồng đăng nhập nhiều bước.
 *
 * <pre>
 *  mật khẩu đúng ─┬─ vai trò cần OTP ──► token OTP_PENDING (chưa gọi được API) ─► nhập OTP đúng ─► token ACCESS
 *                 └─ còn lại ──────────► token ACCESS
 * </pre>
 * Nguyên tắc: token đầy đủ quyền chỉ được cấp khi MỌI bước đã hoàn tất.
 */
@Service
public class AuthService {

    /** Thông báo dùng chung cho sai mật khẩu / sai tên / tài khoản khóa / chưa kích hoạt — không cho dò tài khoản. */
    static final String INVALID_CREDENTIALS = "Sai tên đăng nhập hoặc mật khẩu";
    static final String PREAUTH_EXPIRED = "Phiên xác thực đã hết hạn. Vui lòng đăng nhập lại.";

    /** Các vai trò bắt buộc xác thực 2 bước bằng OTP. */
    private static final Set<String> OTP_ROLES = Set.of("ADMIN", "LECTURER", "DEPARTMENT_ADMIN");

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final OtpService otpService;
    private final AuditLogService auditLogService;
    private final LoginThrottleService throttle;
    private final RateLimiter rateLimiter;
    private final SecurityProperties props;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtTokenProvider tokenProvider,
                       UserRepository userRepository,
                       OtpService otpService,
                       AuditLogService auditLogService,
                       LoginThrottleService throttle,
                       RateLimiter rateLimiter,
                       SecurityProperties props) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userRepository = userRepository;
        this.otpService = otpService;
        this.auditLogService = auditLogService;
        this.throttle = throttle;
        this.rateLimiter = rateLimiter;
        this.props = props;
    }

    // ------------------------------------------------------------------ đăng nhập

    public LoginResponse login(LoginRequest request, String clientIp) {
        String username = request.getUsername().trim();

        // Lớp 1: giới hạn theo IP; lớp 2: khóa theo cặp (username, IP) — cả hai chặn TRƯỚC khi kiểm tra mật khẩu
        rateLimiter.check("login", clientIp, props.getRate().getLoginPerMinute(), Duration.ofMinutes(1));
        throttle.assertNotLocked(username, clientIp);

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, request.getPassword()));
        } catch (AuthenticationException ex) {
            boolean lockedNow = throttle.recordFailure(username, clientIp);
            auditLogService.log("LOGIN_FAILED", "User", username, "ip=" + clientIp, username);
            if (lockedNow) {
                auditLogService.log("LOGIN_LOCKED", "User", username,
                        "ip=" + clientIp + ", khóa " + props.getLogin().getLockMinutes() + " phút", username);
            }
            throw unauthorized(INVALID_CREDENTIALS);
        }
        throttle.recordSuccess(username, clientIp);

        User user = userRepository.findByUsername(username).orElseThrow(() -> unauthorized(INVALID_CREDENTIALS));
        return continueAfterPassword(user, clientIp);
    }

    // ------------------------------------------------------------------ OTP

    public LoginResponse verifyOtp(String preAuthToken, String code, String clientIp) {
        rateLimiter.check("otp-verify", clientIp, props.getRate().getOtpVerifyPerMinute(), Duration.ofMinutes(1));
        User user = requirePreAuthUser(preAuthToken, Scope.OTP_PENDING);
        VerifyOutcome outcome = otpService.verify(user, OtpPurpose.LOGIN, code);
        switch (outcome.result()) {
            case OK:
                return issueSession(user, clientIp);
            case INVALID:
                auditLogService.log("OTP_FAILED", "User", user.getId().toString(), "ip=" + clientIp, user.getUsername());
                throw unauthorized("Mã OTP không đúng. Bạn còn " + outcome.remainingAttempts() + " lần thử.");
            case LOCKED:
                auditLogService.log("OTP_LOCKED", "User", user.getId().toString(), "ip=" + clientIp, user.getUsername());
                throw unauthorized("Bạn đã nhập sai quá nhiều lần. Vui lòng đăng nhập lại để nhận mã mới.");
            case EXPIRED:
                throw unauthorized("Mã OTP đã hết hạn. Vui lòng gửi lại mã mới.");
            case NONE:
            default:
                throw unauthorized("Không có mã OTP đang hiệu lực. Vui lòng đăng nhập lại.");
        }
    }

    public OtpResendResponse resendOtp(String preAuthToken, String clientIp) {
        rateLimiter.check("otp-resend", clientIp, props.getRate().getOtpResendPer10Minutes(), Duration.ofMinutes(10));
        User user = requirePreAuthUser(preAuthToken, Scope.OTP_PENDING);
        otpService.issueWithCooldown(user, OtpPurpose.LOGIN);
        return new OtpResendResponse(true, OtpService.RESEND_COOLDOWN_SECONDS, maskEmail(user.getEmail()));
    }

    // ------------------------------------------------------------------ nội bộ

    /** Sau khi mật khẩu đúng: quyết định bước kế tiếp. */
    private LoginResponse continueAfterPassword(User user, String clientIp) {
        String role = roleName(user);
        if (OTP_ROLES.contains(role)) {
            otpService.issue(user, OtpPurpose.LOGIN);
            String preAuth = tokenProvider.generatePreAuthToken(user.getUsername(), user.getId(), user.getTokenVersion(), Scope.OTP_PENDING);
            return LoginResponse.otpRequired(preAuth, user, tokenProvider.getPreAuthTtlSeconds(),
                    maskEmail(user.getEmail()), OtpService.RESEND_COOLDOWN_SECONDS);
        }
        return issueSession(user, clientIp);
    }

    /** Cấp access token đầy đủ quyền — điểm DUY NHẤT trong luồng đăng nhập cấp token loại này. */
    private LoginResponse issueSession(User user, String clientIp) {
        String role = roleName(user);
        String token = tokenProvider.generateAccessToken(user.getUsername(), user.getId(), role, user.getTokenVersion());
        auditLogService.log("LOGIN_SUCCESS", "User", user.getId().toString(), "ip=" + clientIp, user.getUsername());
        return LoginResponse.authenticated(token, user, role);
    }

    /**
     * Xác thực token tạm và trả về người dùng tương ứng. Username lấy từ token (không tin client).
     * Từ chối nếu token sai loại/hết hạn, tài khoản bị khóa/chưa kích hoạt, hoặc token đã bị thu hồi (tokenVersion lệch).
     */
    private User requirePreAuthUser(String preAuthToken, Scope scope) {
        Optional<TokenClaims> claims = tokenProvider.parse(preAuthToken, scope);
        if (claims.isEmpty()) {
            throw unauthorized(PREAUTH_EXPIRED);
        }
        User user = userRepository.findByUsername(claims.get().username()).orElseThrow(() -> unauthorized(PREAUTH_EXPIRED));
        if (!user.isActive() || !user.isActivated() || user.getTokenVersion() != claims.get().tokenVersion()) {
            throw unauthorized(PREAUTH_EXPIRED);
        }
        return user;
    }

    private static String roleName(User user) {
        return user.getRole() != null ? user.getRole().getName() : "STUDENT";
    }

    private static ResponseStatusException unauthorized(String message) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
    }

    /** "nguyen.van@gmail.com" → "n***@gmail.com" (đủ để người dùng nhận ra hộp thư, không lộ địa chỉ). */
    static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
