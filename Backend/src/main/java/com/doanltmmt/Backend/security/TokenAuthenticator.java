package com.doanltmmt.Backend.security;

import com.doanltmmt.Backend.security.JwtTokenProvider.Scope;
import com.doanltmmt.Backend.security.JwtTokenProvider.TokenClaims;
import com.doanltmmt.Backend.service.CustomUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Quy tắc DUY NHẤT quyết định một access token có còn hiệu lực hay không — dùng chung cho filter HTTP
 * và interceptor WebSocket để hai nơi không bao giờ lệch nhau.
 *
 * <p>Token hợp lệ khi: đúng chữ ký/hạn/issuer, {@code scope=ACCESS}, người dùng còn tồn tại, tài khoản còn
 * bật (không khóa, đã kích hoạt) và {@code tv} khớp {@code tokenVersion} hiện tại (chưa bị thu hồi).
 */
@Component
public class TokenAuthenticator {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;

    public TokenAuthenticator(JwtTokenProvider tokenProvider, CustomUserDetailsService userDetailsService) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
    }

    /** @param rawToken chuỗi JWT (không kèm tiền tố "Bearer ") */
    public Optional<AuthUser> authenticate(String rawToken) {
        Optional<TokenClaims> claims = tokenProvider.parse(rawToken, Scope.ACCESS);
        if (claims.isEmpty()) {
            return Optional.empty();
        }
        UserDetails details;
        try {
            details = userDetailsService.loadUserByUsername(claims.get().username());
        } catch (UsernameNotFoundException ex) {
            return Optional.empty(); // token hợp lệ nhưng người dùng đã bị xóa
        }
        if (!(details instanceof AuthUser user)) {
            return Optional.empty();
        }
        if (!user.isEnabled() || user.getTokenVersion() != claims.get().tokenVersion()) {
            return Optional.empty();
        }
        return Optional.of(user);
    }

    /** Tách token khỏi giá trị header "Authorization: Bearer ...". */
    public static Optional<String> extractBearer(String headerValue) {
        if (headerValue != null && headerValue.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String token = headerValue.substring(7).trim();
            return token.isEmpty() ? Optional.empty() : Optional.of(token);
        }
        return Optional.empty();
    }
}
