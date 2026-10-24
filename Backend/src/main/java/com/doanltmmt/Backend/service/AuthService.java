package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.config.SecurityProperties;
import com.doanltmmt.Backend.dto.LoginRequest;
import com.doanltmmt.Backend.dto.LoginResponse;
import com.doanltmmt.Backend.dto.OtpResendResponse;
import com.doanltmmt.Backend.entity.OtpPurpose;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.repository.UserRepository;
import com.doanltmmt.Backend.security.JwtTokenProvider.Scope;
import com.doanltmmt.Backend.security.JwtTokenProvider.TokenClaims;
import com.doanltmmt.Backend.security.JwtTokenProvider;
import com.doanltmmt.Backend.security.PasswordPolicy;
import com.doanltmmt.Backend.security.RateLimiter;
import com.doanltmmt.Backend.service.OtpService.VerifyOutcome;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    static final String ACTIVATION_FAILED = "Mã kích hoạt không đúng hoặc đã hết hạn.";
    static final String RESET_FAILED = "Mã xác nhận không đúng hoặc đã hết hạn.";
    /** Trả về như nhau dù tài khoản có tồn tại hay không (chống dò tài khoản). */
    public static final String RESET_REQUESTED = "Nếu tài khoản tồn tại, mã đặt lại mật khẩu đã được gửi tới email đăng ký.";
    private static final int RESET_REQUESTS_PER_IDENTIFIER = 3;
    private static final int RESET_REQUESTS_PER_IP = 10;
    private static final Duration RESET_WINDOW = Duration.ofMinutes(15);

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
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokens;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtTokenProvider tokenProvider,
                       UserRepository userRepository,
                       OtpService otpService,
                       AuditLogService auditLogService,
                       LoginThrottleService throttle,
                       RateLimiter rateLimiter,
                       SecurityProperties props,
                       PasswordEncoder passwordEncoder,
                       RefreshTokenService refreshTokens) {
        this.passwordEncoder = passwordEncoder;
        this.refreshTokens = refreshTokens;
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

    // ------------------------------------------------------------------ đổi mật khẩu bắt buộc / kích hoạt

    /** Đặt mật khẩu mới khi đang dùng mật khẩu tạm, rồi tiếp tục luồng đăng nhập (có thể sang bước OTP). */
    public LoginResponse changeInitialPassword(String preAuthToken, String newPassword, String clientIp) {
        rateLimiter.check("pwd-initial", clientIp, props.getRate().getOtpVerifyPerMinute(), Duration.ofMinutes(1));
        User user = requirePreAuthUser(preAuthToken, Scope.PWD_CHANGE);
        if (!user.isMustChangePassword()) {
            throw unauthorized(PREAUTH_EXPIRED);
        }
        PasswordPolicy.requireValid(newPassword, user.getUsername());
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu mới không được trùng mật khẩu hiện tại");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        user.setTokenVersion(user.getTokenVersion() + 1); // token PWD_CHANGE vừa dùng cũng hết hiệu lực
        userRepository.save(user);
        auditLogService.log("PASSWORD_INITIAL_SET", "User", user.getId().toString(), "ip=" + clientIp, user.getUsername());
        return continueAfterPassword(user, clientIp);
    }

    /**
     * Kích hoạt tài khoản do quản trị tạo (chưa có mật khẩu). Mọi lỗi về tài khoản/mã trả CÙNG một thông báo
     * để không dò được tên đăng nhập hay email nào tồn tại.
     */
    public void activate(String identifier, String code, String newPassword, String clientIp) {
        rateLimiter.check("activate", clientIp, props.getRate().getActivatePer10Minutes(), Duration.ofMinutes(10));
        String id = identifier.trim();
        User user = findByIdentifier(id);
        // Kiểm tra mật khẩu TRƯỚC khi tiêu thụ mã: mật khẩu yếu không làm mất mã kích hoạt của người dùng
        PasswordPolicy.requireValid(newPassword, user != null ? user.getUsername() : id);
        if (user == null || user.isActivated() || !user.isActive()) {
            throw unauthorized(ACTIVATION_FAILED);
        }
        VerifyOutcome outcome = otpService.verify(user, OtpPurpose.ACTIVATION, code);
        if (outcome.result() != OtpService.VerifyResult.OK) {
            auditLogService.log("ACTIVATION_FAILED", "User", user.getId().toString(), "ip=" + clientIp + ", " + outcome.result(), user.getUsername());
            throw unauthorized(ACTIVATION_FAILED);
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setActivated(true);
        user.setMustChangePassword(false);
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
        auditLogService.log("ACTIVATE_ACCOUNT", "User", user.getId().toString(), "ip=" + clientIp, user.getUsername());
    }

    /**
     * Quên mật khẩu — bước 1: gửi mã RESET_PASSWORD (10 phút) tới email của tài khoản.
     * Không tiết lộ tài khoản có tồn tại hay không: luôn trả cùng một kết quả; tài khoản không hợp lệ hoặc
     * đang trong thời gian chờ gửi lại thì bỏ qua im lặng.
     */
    public void requestPasswordReset(String identifier, String clientIp) {
        String id = identifier.trim();
        rateLimiter.check("pwd-reset-ip", clientIp, RESET_REQUESTS_PER_IP, RESET_WINDOW);
        rateLimiter.check("pwd-reset-id", id.toLowerCase(), RESET_REQUESTS_PER_IDENTIFIER, RESET_WINDOW);
        User user = findByIdentifier(id);
        if (user == null || !user.isActive() || !user.isActivated() || user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }
        if (otpService.secondsUntilResendAllowed(user, OtpPurpose.RESET_PASSWORD) > 0) {
            return;
        }
        otpService.issue(user, OtpPurpose.RESET_PASSWORD);
        auditLogService.log("PASSWORD_RESET_REQUESTED", "User", user.getId().toString(), "ip=" + clientIp, user.getUsername());
    }

    /**
     * Quên mật khẩu — bước 2: mã đúng → đặt mật khẩu mới và thu hồi mọi token cũ (tokenVersion + 1).
     * Mọi lỗi về tài khoản/mã trả CÙNG một thông báo.
     */
    public void resetPassword(String identifier, String code, String newPassword, String clientIp) {
        rateLimiter.check("pwd-reset-ip", clientIp, RESET_REQUESTS_PER_IP, RESET_WINDOW);
        String id = identifier.trim();
        User user = findByIdentifier(id);
        // Kiểm tra mật khẩu TRƯỚC khi tiêu thụ mã: mật khẩu yếu không làm mất mã của người dùng
        PasswordPolicy.requireValid(newPassword, user != null ? user.getUsername() : id);
        if (user == null || !user.isActive() || !user.isActivated()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, RESET_FAILED);
        }
        VerifyOutcome outcome = otpService.verify(user, OtpPurpose.RESET_PASSWORD, code);
        if (outcome.result() != OtpService.VerifyResult.OK) {
            auditLogService.log("PASSWORD_RESET_FAILED", "User", user.getId().toString(), "ip=" + clientIp + ", " + outcome.result(), user.getUsername());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, RESET_FAILED);
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
        refreshTokens.revokeAll(user.getId());
        auditLogService.log("PASSWORD_RESET_DONE", "User", user.getId().toString(), "ip=" + clientIp, user.getUsername());
    }

    // ------------------------------------------------------------------ nội bộ

    private User findByIdentifier(String id) {
        return userRepository.findByUsername(id).or(() -> userRepository.findByEmail(id)).orElse(null);
    }

    /** Sau khi mật khẩu đúng: quyết định bước kế tiếp. */
    private LoginResponse continueAfterPassword(User user, String clientIp) {
        String role = roleName(user);
        if (user.isMustChangePassword()) {
            // Đang dùng mật khẩu tạm do quản trị đặt: phải đổi trước (token PWD_CHANGE chỉ gọi được /auth/password/initial)
            String preAuth = tokenProvider.generatePreAuthToken(user.getUsername(), user.getId(), user.getTokenVersion(), Scope.PWD_CHANGE);
            return LoginResponse.passwordChangeRequired(preAuth, user, tokenProvider.getPreAuthTtlSeconds());
        }
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

    /** Đăng xuất mọi thiết bị: tăng tokenVersion → mọi token đang lưu hành của người dùng lập tức vô hiệu. */
    @Transactional
    public void logoutAll(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
        refreshTokens.revokeAll(user.getId());
        auditLogService.log("LOGOUT_ALL", "User", user.getId().toString(), "Đăng xuất mọi thiết bị");
    }

    // ------------------------------------------------------------------ P3-14: làm mới phiên

    /** Access token mới + refresh token mới (đã xoay vòng) cho phiên đang có. */
    public record Refreshed(LoginResponse response, String refreshToken) {
    }

    public Refreshed refresh(String refreshToken, String userAgent, String clientIp) {
        rateLimiter.check("refresh", clientIp, props.getRate().getRefreshPerMinute(), Duration.ofMinutes(1));
        RefreshTokenService.Issued next = refreshTokens.rotate(refreshToken, userAgent, clientIp);
        User user = next.user();
        String role = roleName(user);
        String access = tokenProvider.generateAccessToken(user.getUsername(), user.getId(), role, user.getTokenVersion());
        return new Refreshed(LoginResponse.authenticated(access, user, role), next.rawToken());
    }
}
