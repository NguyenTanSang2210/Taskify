package com.doanltmmt.Backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Lỗi nghiệp vụ có mã ({@code code}) để frontend xử lý theo từng loại — danh sách mã: {@code docs/error-codes.md}.
 * Kế thừa {@link ResponseStatusException} nên mọi chỗ đang bắt/kiểm tra ResponseStatusException vẫn đúng.
 */
public class ApiException extends ResponseStatusException {

    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(status, message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static ApiException badRequest(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND, message);
    }

    public static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN, message);
    }

    public static ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }
}
