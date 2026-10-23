package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.service.UserService;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

/** Kiểm tra email khi cập nhật hồ sơ — gồm trường hợp chuỗi dài độc hại (ReDoS, cảnh báo CodeQL java/polynomial-redos). */
class UserEmailValidationTest {

    @Test
    void acceptsNormalAddresses() {
        assertTrue(UserService.isValidEmail("sv001@hutech.edu.vn"));
        assertTrue(UserService.isValidEmail("ten+admin@gmail.com"));
        assertTrue(UserService.isValidEmail("a.b_c-d@sub.domain.vn"));
    }

    @Test
    void rejectsMalformedAddresses() {
        assertFalse(UserService.isValidEmail(null));
        assertFalse(UserService.isValidEmail("khongcoacong"));
        assertFalse(UserService.isValidEmail("a@b"));
        assertFalse(UserService.isValidEmail("a@b."));
        assertFalse(UserService.isValidEmail("a@.b"));
        assertFalse(UserService.isValidEmail("a@b..c"));
        assertFalse(UserService.isValidEmail("a b@c.d"));
        assertFalse(UserService.isValidEmail("a@b@c.d"));
        assertFalse(UserService.isValidEmail("a@" + "b".repeat(250) + ".vn"));
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void maliciousInputIsRejectedQuickly() {
        // Với regex cũ ^[^@\s]+@[^@\s]+\.[^@\s]+$ chuỗi này khiến bộ so khớp quay lui O(n^2)
        assertFalse(UserService.isValidEmail("a@" + ".".repeat(240) + "!"));
        assertFalse(UserService.isValidEmail("a@" + "a.".repeat(120) + " "));
    }
}
