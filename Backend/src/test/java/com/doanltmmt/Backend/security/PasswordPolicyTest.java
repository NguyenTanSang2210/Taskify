package com.doanltmmt.Backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordPolicyTest {

    @Test
    void acceptsReasonablePasswords() {
        assertTrue(PasswordPolicy.violations("Matkhau2026", "nguyenvan").isEmpty());
        assertTrue(PasswordPolicy.violations("mật khẩu số 1", "x").isEmpty());
        assertTrue(PasswordPolicy.violations("a".repeat(63) + "1", "x").isEmpty()); // đúng 64 ký tự
        assertDoesNotThrow(() -> PasswordPolicy.requireValid("Abcdefg1", "user"));
    }

    @Test
    void rejectsTooShortAndTooLong() {
        assertTrue(PasswordPolicy.violations("Ab1", "x").stream().anyMatch(s -> s.contains("ít nhất 8")));
        assertTrue(PasswordPolicy.violations("a".repeat(64) + "1", "x").stream().anyMatch(s -> s.contains("64")));
    }

    @Test
    void requiresLetterAndDigit() {
        assertTrue(PasswordPolicy.violations("12345678", "x").stream().anyMatch(s -> s.contains("chữ cái")));
        assertTrue(PasswordPolicy.violations("abcdefgh", "x").stream().anyMatch(s -> s.contains("chữ số")));
    }

    @Test
    void rejectsPasswordsOverBcryptByteLimitEvenIfShortInCharacters() {
        String vietnamese = "ế".repeat(30) + "1"; // 31 ký tự nhưng 91 byte UTF-8
        assertTrue(PasswordPolicy.violations(vietnamese, "x").stream().anyMatch(s -> s.contains("72 byte")));
    }

    @Test
    void rejectsPasswordContainingUsernameCaseInsensitive() {
        assertTrue(PasswordPolicy.violations("xxNguyenVanxx1", "nguyenvan").stream().anyMatch(s -> s.contains("tên đăng nhập")));
    }

    @Test
    void rejectsBlankAndNull() {
        assertThrows(ResponseStatusException.class, () -> PasswordPolicy.requireValid("", "x"));
        assertThrows(ResponseStatusException.class, () -> PasswordPolicy.requireValid(null, "x"));
    }

    @Test
    void errorMessageListsAllProblems() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> PasswordPolicy.requireValid("a", "x"));
        assertTrue(ex.getReason().contains("ít nhất 8") && ex.getReason().contains("chữ số"));
    }
}
