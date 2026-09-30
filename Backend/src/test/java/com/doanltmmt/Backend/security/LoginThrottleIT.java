package com.doanltmmt.Backend.security;

import com.doanltmmt.Backend.entity.Department;
import com.doanltmmt.Backend.entity.LoginThrottle;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.repository.AuditLogRepository;
import com.doanltmmt.Backend.repository.LoginThrottleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P1-10 — khóa đăng nhập theo cặp (username, IP), lưu DB, không bị lợi dụng để khóa tài khoản người khác.
 * IP giả lập qua {@code request.setRemoteAddr} (đúng thứ backend đọc sau khi Tomcat xử lý X-Forwarded-For).
 */
@SuppressWarnings("null")
class LoginThrottleIT extends AbstractSecurityIT {

    private static final String IP_A = "10.20.30.1";
    private static final String IP_B = "10.20.30.2";

    @Autowired private LoginThrottleRepository throttleRepo;
    @Autowired private AuditLogRepository auditRepo;

    private User student;
    private User otherStudent;

    @BeforeEach
    void setUp() {
        Department dept = newDepartment("THR");
        student = newUser("stu", "STUDENT", dept);
        otherStudent = newUser("stu2", "STUDENT", dept);
    }

    private static RequestPostProcessor from(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    private ResultActions login(String username, String password, String ip) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).with(from(ip))
                .content(json.writeValueAsString(Map.of("username", username, "password", password))));
    }

    private void failTimes(String username, String ip, int times) throws Exception {
        for (int i = 0; i < times; i++) {
            login(username, "sai-mat-khau", ip).andExpect(status().isUnauthorized());
        }
    }

    private LoginThrottle theOnlyThrottleRow() {
        List<LoginThrottle> rows = throttleRepo.findAll();
        assertTrue(rows.size() >= 1, "chưa có bộ đếm nào");
        return rows.get(rows.size() - 1);
    }

    @Test
    void fiveWrongPasswords_lockThePair_evenTheCorrectPasswordIsThenRefused() throws Exception {
        failTimes(student.getUsername(), IP_A, 5);

        login(student.getUsername(), PASSWORD, IP_A)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(errorMessageContains("quá nhiều lần"));
    }

    @Test
    void fourWrongPasswords_doNotLockYet() throws Exception {
        failTimes(student.getUsername(), IP_A, 4);

        login(student.getUsername(), PASSWORD, IP_A).andExpect(status().isOk());
    }

    @Test
    void lock_isPerUsernameAndIpPair_soAnAttackerCannotLockTheVictimOutElsewhere() throws Exception {
        failTimes(student.getUsername(), IP_A, 5);   // kẻ tấn công gõ sai từ IP_A
        login(student.getUsername(), PASSWORD, IP_A).andExpect(status().isTooManyRequests());

        login(student.getUsername(), PASSWORD, IP_B).andExpect(status().isOk());        // nạn nhân từ IP khác vẫn vào được
        login(otherStudent.getUsername(), PASSWORD, IP_A).andExpect(status().isOk());   // user khác từ IP bị khóa vẫn vào được
    }

    @Test
    void successfulLogin_clearsTheFailureCounter() throws Exception {
        failTimes(student.getUsername(), IP_A, 4);
        login(student.getUsername(), PASSWORD, IP_A).andExpect(status().isOk());

        failTimes(student.getUsername(), IP_A, 4);   // đếm lại từ 0 nên 4 lần nữa vẫn chưa khóa
        login(student.getUsername(), PASSWORD, IP_A).andExpect(status().isOk());
    }

    @Test
    void lock_expiresByItself() throws Exception {
        failTimes(student.getUsername(), IP_A, 5);
        LoginThrottle row = theOnlyThrottleRow();
        row.setLockedUntil(LocalDateTime.now().minusSeconds(1));
        throttleRepo.saveAndFlush(row);

        login(student.getUsername(), PASSWORD, IP_A).andExpect(status().isOk());
    }

    @Test
    void oldFailures_outsideTheWindow_areNotCounted() throws Exception {
        failTimes(student.getUsername(), IP_A, 4);
        LoginThrottle row = theOnlyThrottleRow();
        row.setWindowStart(LocalDateTime.now().minusMinutes(30));   // ngoài cửa sổ 15 phút
        throttleRepo.saveAndFlush(row);

        failTimes(student.getUsername(), IP_A, 1);                  // chỉ được tính là lần sai thứ 1
        login(student.getUsername(), PASSWORD, IP_A).andExpect(status().isOk());
    }

    @Test
    void usernameIsMatchedCaseInsensitively() throws Exception {
        String upper = student.getUsername().toUpperCase();
        failTimes(upper, IP_A, 3);
        failTimes(student.getUsername(), IP_A, 2);

        login(student.getUsername(), PASSWORD, IP_A).andExpect(status().isTooManyRequests());
    }

    @Test
    void lockState_isStoredInTheDatabase_withAnOpaqueKey() throws Exception {
        failTimes(student.getUsername(), IP_A, 5);

        LoginThrottle row = theOnlyThrottleRow();
        assertNotNull(row.getLockedUntil());
        assertTrue(row.getLockedUntil().isAfter(LocalDateTime.now()), "phải còn bị khóa");
        assertTrue(row.getThrottleKey().matches("[0-9a-f]{64}"), "khóa phải là SHA-256 hex: " + row.getThrottleKey());
        assertFalse(row.getThrottleKey().contains(student.getUsername()), "không lưu username thô trong khóa");
    }

    @Test
    void lockingIsWrittenToTheAuditLog() throws Exception {
        failTimes(student.getUsername(), IP_A, 5);

        boolean audited = auditRepo.findAll().stream().anyMatch(l ->
                "LOGIN_LOCKED".equals(l.getAction()) && student.getUsername().equals(l.getActorUsername()));
        assertTrue(audited, "thiếu LOGIN_LOCKED trong audit log");
    }
}
