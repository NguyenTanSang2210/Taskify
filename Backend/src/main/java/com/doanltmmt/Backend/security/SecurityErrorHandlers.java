package com.doanltmmt.Backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Phản hồi lỗi bảo mật dạng JSON cùng khuôn với lỗi mặc định của Spring Boot
 * ({@code timestamp, status, error, message, path}) để frontend đọc {@code message} như mọi lỗi khác.
 *
 * <ul>
 *   <li><b>401</b>: chưa đăng nhập / token sai, hết hạn, bị thu hồi → frontend cần đăng nhập lại.</li>
 *   <li><b>403</b>: đã đăng nhập nhưng không đủ quyền → frontend chỉ báo lỗi, KHÔNG đăng xuất.</li>
 * </ul>
 * (Trước đây Spring trả 403 cho cả hai trường hợp nên frontend không phân biệt được.)
 */
public final class SecurityErrorHandlers {

    private SecurityErrorHandlers() {
    }

    public static AuthenticationEntryPoint unauthorized() {
        return (request, response, ex) -> {
            response.setHeader("WWW-Authenticate", "Bearer");
            write(request, response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized",
                    "Phiên đăng nhập không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại.");
        };
    }

    public static AccessDeniedHandler forbidden() {
        return (request, response, ex) -> write(request, response, HttpServletResponse.SC_FORBIDDEN, "Forbidden",
                "Bạn không có quyền thực hiện thao tác này.");
    }

    private static void write(HttpServletRequest request, HttpServletResponse response,
                              int status, String error, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"timestamp\":\"" + LocalDateTime.now() + "\","
                + "\"status\":" + status + ","
                + "\"error\":\"" + error + "\","
                + "\"message\":\"" + escape(message) + "\","
                + "\"path\":\"" + escape(request.getRequestURI()) + "\"}");
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length() + 8);
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
