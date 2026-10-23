package com.doanltmmt.Backend.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Định dạng lỗi thống nhất của API (P3-03):
 * <pre>{"timestamp", "status", "code", "error", "message", "path", "errors": [{"field", "message"}]}</pre>
 * {@code errors} chỉ có khi lỗi theo từng trường (VALIDATION_FAILED).
 */
public final class ErrorBodies {

    private ErrorBodies() {
    }

    public record FieldError(String field, String message) {
    }

    public static Map<String, Object> body(HttpStatusCode status, String code, String message, String path,
                                           List<FieldError> errors) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("timestamp", OffsetDateTime.now().toString());
        b.put("status", status.value());
        b.put("code", code);
        b.put("error", status instanceof HttpStatus hs ? hs.getReasonPhrase() : String.valueOf(status.value()));
        b.put("message", message);
        b.put("path", path);
        if (errors != null && !errors.isEmpty()) b.put("errors", errors);
        return b;
    }

    static ResponseEntity<Object> of(HttpStatusCode status, String code, String message, String path,
                                     List<FieldError> errors, HttpHeaders headers) {
        return ResponseEntity.status(status).headers(headers).body(body(status, code, message, path, errors));
    }
}
