package com.doanltmmt.Backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Lưới an toàn cho lỗi lập trình/dữ liệu chưa được kiểm tra riêng: trả thông báo chung tiếng Việt thay vì 500 kèm
 * thông điệp nội bộ của Java ({@code server.error.include-message=always} sẽ in ra tên lớp, tên biến, "For input string"…).
 * Chi tiết chỉ ghi log. Không bắt {@code ResponseStatusException} hay lỗi phân quyền — giữ nguyên 400/401/403/404 hiện có.
 */
@RestControllerAdvice
public class InputErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(InputErrorHandler.class);

    /** Sai kiểu dữ liệu trong body (gửi chuỗi chỗ cần số, mảng chỗ cần chuỗi…), số không đọc được, tham số không hợp lệ. */
    @ExceptionHandler({ClassCastException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, Object>> badInput(RuntimeException ex, HttpServletRequest req) {
        log.warn("Dữ liệu không hợp lệ tại {}: {}", req.getRequestURI(), ex.toString());
        return ErrorBodies.of(HttpStatus.BAD_REQUEST, "Dữ liệu gửi lên không hợp lệ hoặc sai định dạng.", req);
    }

    /** Lỗi lập trình — không để lộ thông điệp nội bộ ra phản hồi. */
    @ExceptionHandler({NullPointerException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, Object>> unexpected(RuntimeException ex, HttpServletRequest req) {
        log.error("Lỗi không mong đợi tại {}", req.getRequestURI(), ex);
        return ErrorBodies.of(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống, vui lòng thử lại sau.", req);
    }
}
