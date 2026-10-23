package com.doanltmmt.Backend.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** P3-03 — kiểm tra trực tiếp ánh xạ ngoại lệ → mã lỗi và thông báo (không lộ chi tiết nội bộ). */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/x");

    @SuppressWarnings("unchecked")
    private static Map<String, Object> body(ResponseEntity<Object> res) {
        return (Map<String, Object>) res.getBody();
    }

    @Test
    void apiExceptionKeepsItsCode() {
        ResponseEntity<Object> res = handler.handleStatus(ApiException.conflict(ErrorCodes.TOPIC_FULL, "Đầy"), req);
        assertEquals(409, res.getStatusCode().value());
        assertEquals(ErrorCodes.TOPIC_FULL, body(res).get("code"));
        assertEquals("Đầy", body(res).get("message"));
        assertEquals("/api/x", body(res).get("path"));
    }

    @Test
    void plainStatusExceptionGetsCodeFromStatus() {
        ResponseEntity<Object> res = handler.handleStatus(new ResponseStatusException(HttpStatus.NOT_FOUND), req);
        assertEquals(ErrorCodes.NOT_FOUND, body(res).get("code"));
        assertEquals(ErrorCodes.defaultMessage(HttpStatus.NOT_FOUND), body(res).get("message"));
    }

    @Test
    void headersSuchAsRetryAfterAreKept() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Chậm lại") {
            @Override
            public HttpHeaders getHeaders() {
                HttpHeaders h = new HttpHeaders();
                h.set(HttpHeaders.RETRY_AFTER, "30");
                return h;
            }
        };
        ResponseEntity<Object> res = handler.handleStatus(ex, req);
        assertEquals("30", res.getHeaders().getFirst(HttpHeaders.RETRY_AFTER));
        assertEquals(ErrorCodes.TOO_MANY_REQUESTS, body(res).get("code"));
    }

    @Test
    void integrityViolationIs409WithoutSql() {
        ResponseEntity<Object> res = handler.handleIntegrity(
                new DataIntegrityViolationException("Duplicate entry 'x' for key 'users.username'"), req);
        assertEquals(409, res.getStatusCode().value());
        assertEquals(ErrorCodes.DATA_CONFLICT, body(res).get("code"));
        assertFalse(String.valueOf(body(res).get("message")).contains("Duplicate"));
    }

    @Test
    void otherDataAccessErrorIs500Generic() {
        ResponseEntity<Object> res = handler.handleDataAccess(new QueryTimeoutException("select * from secret"), req);
        assertEquals(500, res.getStatusCode().value());
        assertEquals(ErrorCodes.INTERNAL_ERROR, body(res).get("code"));
        assertFalse(String.valueOf(body(res).get("message")).contains("select"));
    }

    @Test
    void wrongTypeInBodyIs400() {
        ResponseEntity<Object> res = handler.handleBadInput(new ClassCastException("java.lang.Integer cannot be cast"), req);
        assertEquals(400, res.getStatusCode().value());
        assertEquals(ErrorCodes.INVALID_INPUT, body(res).get("code"));
        assertFalse(String.valueOf(body(res).get("message")).contains("java."));
    }

    @Test
    void unexpectedIs500Generic() {
        ResponseEntity<Object> res = handler.handleUnexpected(new IllegalStateException("NPE inside"), req);
        assertEquals(500, res.getStatusCode().value());
        assertFalse(String.valueOf(body(res).get("message")).contains("NPE"));
    }
}
