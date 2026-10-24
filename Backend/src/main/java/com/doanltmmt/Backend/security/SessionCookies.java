package com.doanltmmt.Backend.security;

import com.doanltmmt.Backend.dto.LoginResponse;
import com.doanltmmt.Backend.service.AuthService;
import com.doanltmmt.Backend.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * P3-14 — nối phiên đăng nhập với cookie refresh token (tầng web): đăng nhập xong thì đặt cookie, làm mới thì
 * xoay vòng cookie, đăng xuất thì thu hồi + xóa cookie. Logic token nằm ở {@link RefreshTokenService}.
 */
@Component
public class SessionCookies {

    private final RefreshTokenService refreshTokens;
    private final RefreshCookie cookie;
    private final AuthService authService;

    public SessionCookies(RefreshTokenService refreshTokens, RefreshCookie cookie, AuthService authService) {
        this.refreshTokens = refreshTokens;
        this.cookie = cookie;
        this.authService = authService;
    }

    /** Bước đăng nhập trả access token (đăng nhập xong) → mở phiên refresh mới. Bước trung gian (OTP...) giữ nguyên. */
    public LoginResponse start(LoginResponse response, HttpServletRequest request, HttpServletResponse http) {
        if (response.token() != null && response.userId() != null) {
            String raw = refreshTokens.issue(response.userId(), request.getHeader(HttpHeaders.USER_AGENT), request.getRemoteAddr());
            cookie.write(http, raw, refreshTokens.getTtl());
        }
        return response;
    }

    /** Mở phiên refresh mới cho người dùng đang đăng nhập (vd. sau khi tự đổi mật khẩu: phiên cũ đã bị thu hồi). */
    public void restart(Long userId, HttpServletRequest request, HttpServletResponse http) {
        cookie.write(http, refreshTokens.issue(userId, request.getHeader(HttpHeaders.USER_AGENT), request.getRemoteAddr()),
                refreshTokens.getTtl());
    }

    public LoginResponse refresh(HttpServletRequest request, HttpServletResponse http) {
        cookie.requireSameOrigin(request);
        try {
            AuthService.Refreshed next = authService.refresh(cookie.read(request).orElse(null),
                    request.getHeader(HttpHeaders.USER_AGENT), request.getRemoteAddr());
            cookie.write(http, next.refreshToken(), refreshTokens.getTtl());
            return next.response();
        } catch (ResponseStatusException e) {
            cookie.clear(http); // cookie hỏng/hết hạn: xóa để trình duyệt không gửi lại
            throw e;
        }
    }

    public void logout(HttpServletRequest request, HttpServletResponse http) {
        cookie.requireSameOrigin(request);
        cookie.read(request).ifPresent(refreshTokens::revoke);
        cookie.clear(http);
    }

    public void clear(HttpServletResponse http) {
        cookie.clear(http);
    }
}
