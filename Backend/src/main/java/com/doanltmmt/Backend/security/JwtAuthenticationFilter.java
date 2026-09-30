package com.doanltmmt.Backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Xác thực request bằng access token trong header Authorization.
 * Token không hợp lệ / hết hạn / bị thu hồi / sai loại → request đi tiếp KHÔNG xác thực; các endpoint được bảo vệ
 * sẽ trả 401 qua {@code authenticationEntryPoint} (xem {@code SecurityConfig}).
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenAuthenticator tokenAuthenticator;

    public JwtAuthenticationFilter(TokenAuthenticator tokenAuthenticator) {
        this.tokenAuthenticator = tokenAuthenticator;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        TokenAuthenticator.extractBearer(request.getHeader("Authorization"))
                .flatMap(tokenAuthenticator::authenticate)
                .ifPresent(user -> {
                    var authorities = user.getAuthorities();
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(user, null, authorities);
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    user.eraseCredentials(); // không giữ băm mật khẩu trong bộ nhớ suốt vòng đời request
                    SecurityContextHolder.getContext().setAuthentication(auth);
                });

        filterChain.doFilter(request, response);
    }
}
