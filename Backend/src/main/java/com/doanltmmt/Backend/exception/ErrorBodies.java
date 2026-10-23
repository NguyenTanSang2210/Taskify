package com.doanltmmt.Backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** Thân phản hồi lỗi cùng dạng với lỗi mặc định của Spring Boot (timestamp, status, error, message, path). */
final class ErrorBodies {

    private ErrorBodies() {
    }

    static ResponseEntity<Map<String, Object>> of(HttpStatus status, String message, HttpServletRequest req) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("timestamp", OffsetDateTime.now().toString());
        b.put("status", status.value());
        b.put("error", status.getReasonPhrase());
        b.put("message", message);
        b.put("path", req.getRequestURI());
        return ResponseEntity.status(status).body(b);
    }
}
