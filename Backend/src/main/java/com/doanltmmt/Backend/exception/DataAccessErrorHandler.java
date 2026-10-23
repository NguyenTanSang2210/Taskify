package com.doanltmmt.Backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Lỗi tầng CSDL: trả thông báo chung, KHÔNG đưa câu lệnh SQL / tên bảng / tên cột ra phản hồi
 * ({@code server.error.include-message=always} sẽ in nguyên thông điệp của exception). Chi tiết chỉ ghi vào log.
 * Chỉ bắt {@link DataAccessException} để không đổi cách xử lý 401/403/400 hiện có.
 */
@RestControllerAdvice
public class DataAccessErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(DataAccessErrorHandler.class);

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> integrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Vi phạm ràng buộc dữ liệu tại {}: {}", req.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return ErrorBodies.of(HttpStatus.CONFLICT, "Dữ liệu không hợp lệ hoặc bị trùng. Vui lòng kiểm tra lại.", req);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> dataAccess(DataAccessException ex, HttpServletRequest req) {
        log.error("Lỗi truy cập CSDL tại {}", req.getRequestURI(), ex);
        return ErrorBodies.of(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống, vui lòng thử lại sau.", req);
    }
}
