package com.doanltmmt.Backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * P3-08 — gắn mã yêu cầu cho mỗi request: nhận {@code X-Request-Id} từ proxy/client nếu hợp lệ, không thì tự sinh.
 * Mã nằm trong MDC ({@code requestId}) nên có trong mọi dòng log của request, được trả lại ở header phản hồi và
 * trong body lỗi — người dùng báo lỗi kèm mã này là tra được log tương ứng.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";
    /** Chỉ nhận mã ngắn, ký tự an toàn — không để client chèn ký tự xuống dòng/giả dòng log. */
    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String incoming = request.getHeader(HEADER);
        String requestId = incoming != null && VALID.matcher(incoming).matches()
                ? incoming : UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        MDC.put(MDC_KEY, requestId);
        request.setAttribute(MDC_KEY, requestId); // cho trang /error (chạy sau khi filter đã dọn MDC)
        response.setHeader(HEADER, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    /** Mã yêu cầu hiện tại (hoặc {@code null} ngoài request, vd. scheduler). */
    public static String current() {
        return MDC.get(MDC_KEY);
    }
}
