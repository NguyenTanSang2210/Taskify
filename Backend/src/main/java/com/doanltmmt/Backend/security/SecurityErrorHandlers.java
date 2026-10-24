package com.doanltmmt.Backend.security;

import com.doanltmmt.Backend.exception.ErrorBodies;
import com.doanltmmt.Backend.exception.ErrorCodes;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

/**
 * Phản hồi lỗi bảo mật dạng JSON cùng định dạng với mọi lỗi API khác ({@link ErrorBodies}, mã {@code UNAUTHORIZED}/{@code FORBIDDEN}).
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
            write(request, response, HttpStatus.UNAUTHORIZED, ErrorCodes.UNAUTHORIZED,
                    ErrorCodes.defaultMessage(HttpStatus.UNAUTHORIZED));
        };
    }

    public static AccessDeniedHandler forbidden() {
        return (request, response, ex) -> write(request, response, HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN,
                ErrorCodes.defaultMessage(HttpStatus.FORBIDDEN));
    }

    private static final ObjectMapper JSON = new ObjectMapper();

    private static void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String code,
                              String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        JSON.writeValue(response.getWriter(), ErrorBodies.body(status, code, message, request.getRequestURI(), null));
    }
}
