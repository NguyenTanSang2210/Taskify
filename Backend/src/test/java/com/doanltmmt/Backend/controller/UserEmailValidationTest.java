package com.doanltmmt.Backend.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Kiểm tra email khi cập nhật hồ sơ — gồm trường hợp chuỗi dài độc hại (ReDoS, cảnh báo CodeQL java/polynomial-redos). */
class UserEmailValidationTest {

    @Test
    void acceptsNormalAddresses() {
        assertTrue(UserController.isValidEmail("sv001@hutech.edu.vn"));
        assertTrue(UserController.isValidEmail("ten+admin@gmail.com"));
        assertTrue(UserController.isValidEmail("a.b_c-d@sub.domain.vn"));
    }

    @Test
    void rejectsMalformedAddresses() {
        assertFalse(UserController.isValidEmail(null));
        assertFalse(UserController.isValidEmail("khongcoacong"));
        assertFalse(UserController.isValidEmail("a@b"));
        assertFalse(UserController.isValidEmail("a@b."));
        assertFalse(UserController.isValidEmail("a@.b"));
        assertFalse(UserController.isValidEmail("a@b..c"));
        assertFalse(UserController.isValidEmail("a b@c.d"));
        assertFalse(UserController.isValidEmail("a@b@c.d"));
        assertFalse(UserController.isValidEmail("a@" + "b".repeat(250) + ".vn"));
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void maliciousInputIsRejectedQuickly() {
        // Với regex cũ ^[^@\s]+@[^@\s]+\.[^@\s]+$ chuỗi này khiến bộ so khớp quay lui O(n^2)
        assertFalse(UserController.isValidEmail("a@" + ".".repeat(240) + "!"));
        assertFalse(UserController.isValidEmail("a@" + "a.".repeat(120) + " "));
    }
}
