package com.doanltmmt.Backend.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Lỗi đầu vào / lỗi lập trình không được trả 500 kèm thông điệp nội bộ của Java. */
class InputErrorHandlerTest {

    private final InputErrorHandler handler = new InputErrorHandler();
    private final MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/x");

    @Test
    void wrongTypes_andUnparsableNumbers_are400_withoutJavaDetails() {
        assertBadRequest(handler.badInput(new ClassCastException("class java.lang.Integer cannot be cast to class java.lang.String"), req));
        assertBadRequest(handler.badInput(new NumberFormatException("For input string: \"abc\""), req));
        assertBadRequest(handler.badInput(new IllegalArgumentException("The given id must not be null"), req));
    }

    @Test
    void programmingErrors_are500_withGenericMessage() {
        ResponseEntity<Map<String, Object>> r = handler.unexpected(
                new NullPointerException("Cannot invoke \"User.getId()\" because \"user\" is null"), req);
        assertEquals(500, r.getStatusCode().value());
        assertEquals("Lỗi hệ thống, vui lòng thử lại sau.", r.getBody().get("message"));
        assertFalse(String.valueOf(r.getBody()).contains("getId"));
    }

    private static void assertBadRequest(ResponseEntity<Map<String, Object>> r) {
        assertEquals(400, r.getStatusCode().value());
        String body = String.valueOf(r.getBody());
        assertFalse(body.contains("java.lang") || body.contains("For input string") || body.contains("must not be null"), body);
    }
}
