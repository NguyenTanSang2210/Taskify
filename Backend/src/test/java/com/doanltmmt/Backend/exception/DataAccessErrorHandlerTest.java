package com.doanltmmt.Backend.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Phản hồi lỗi CSDL không được lộ câu lệnh SQL / tên cột. */
class DataAccessErrorHandlerTest {

    private static final String SQL_DETAIL =
            "could not execute statement [Data too long for column 'actor_username'] [insert into audit_logs (action) values (?)]";

    private final DataAccessErrorHandler handler = new DataAccessErrorHandler();
    private final MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/x");

    @Test
    void integrityViolation_returns409_withoutSql() {
        ResponseEntity<Map<String, Object>> r = handler.integrity(new DataIntegrityViolationException(SQL_DETAIL), req);
        assertEquals(409, r.getStatusCode().value());
        assertNoSql(r);
    }

    @Test
    void otherDataAccessError_returns500_withoutSql() {
        ResponseEntity<Map<String, Object>> r = handler.dataAccess(new InvalidDataAccessResourceUsageException(SQL_DETAIL), req);
        assertEquals(500, r.getStatusCode().value());
        assertNoSql(r);
    }

    private static void assertNoSql(ResponseEntity<Map<String, Object>> r) {
        String body = String.valueOf(r.getBody());
        assertFalse(body.contains("insert"), body);
        assertFalse(body.contains("actor_username"), body);
    }
}
