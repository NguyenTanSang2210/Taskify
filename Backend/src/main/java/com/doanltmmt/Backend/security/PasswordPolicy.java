package com.doanltmmt.Backend.security;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Chính sách mật khẩu: 8–64 ký tự, có ít nhất một chữ cái và một chữ số, không chứa tên đăng nhập.
 * (Không trùng mật khẩu cũ được kiểm tra riêng ở nơi đổi mật khẩu vì cần so băm.)
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 64;
    public static final int MAX_BYTES = 72;

    private PasswordPolicy() {}

    /** Danh sách lỗi (tiếng Việt); rỗng nếu mật khẩu hợp lệ. */
    public static List<String> violations(String password, String username) {
        List<String> errors = new ArrayList<>();
        if (password == null || password.isEmpty()) {
            errors.add("Mật khẩu không được để trống");
            return errors;
        }
        if (password.length() < MIN_LENGTH) {
            errors.add("Mật khẩu phải có ít nhất " + MIN_LENGTH + " ký tự");
        }
        if (password.length() > MAX_LENGTH) {
            errors.add("Mật khẩu không được dài quá " + MAX_LENGTH + " ký tự");
        }
        // bcrypt chỉ xử lý tối đa 72 byte; ký tự tiếng Việt chiếm nhiều byte nên đếm theo UTF-8
        if (password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_BYTES) {
            errors.add("Mật khẩu quá dài (tối đa " + MAX_BYTES + " byte; ký tự có dấu chiếm nhiều byte hơn)");
        }
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (!hasLetter) {
            errors.add("Mật khẩu phải có ít nhất một chữ cái");
        }
        if (!hasDigit) {
            errors.add("Mật khẩu phải có ít nhất một chữ số");
        }
        if (username != null && !username.isBlank()
                && password.toLowerCase(Locale.ROOT).contains(username.trim().toLowerCase(Locale.ROOT))) {
            errors.add("Mật khẩu không được chứa tên đăng nhập");
        }
        return errors;
    }

    /** @throws ResponseStatusException 400 kèm thông báo gộp nếu mật khẩu không đạt. */
    public static void requireValid(String password, String username) {
        List<String> errors = violations(password, username);
        if (!errors.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.join("; ", errors));
        }
    }
}
