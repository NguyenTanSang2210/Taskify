package com.doanltmmt.Backend.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * Đọc và kiểm tra dữ liệu người dùng gửi lên dạng {@code Map} — sai thì trả 400 với thông báo tiếng Việt
 * thay vì để lỗi parse/NullPointerException thành 500. (P3-02 sẽ thay dần bằng request DTO + Bean Validation.)
 */
public final class Inputs {

    private Inputs() {
    }

    public static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    /** Ngày giờ dạng {@code yyyy-MM-ddTHH:mm[:ss]} hoặc ngày {@code yyyy-MM-dd} (đầu ngày); rỗng → {@code null}. */
    public static LocalDateTime dateTime(Object raw, String label) {
        if (raw == null || raw.toString().isBlank()) return null;
        String value = raw.toString().trim();
        try {
            return value.length() == 10 ? LocalDate.parse(value).atStartOfDay() : LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            throw badRequest(label + " không đúng định dạng ngày giờ (yyyy-MM-ddTHH:mm)");
        }
    }

    /** Mã định danh bắt buộc (số nguyên dương). */
    public static Long requiredId(Object raw, String label) {
        if (raw == null || raw.toString().isBlank()) throw badRequest(label + " là bắt buộc");
        try {
            long id = Long.parseLong(raw.toString().trim());
            if (id <= 0) throw badRequest(label + " không hợp lệ");
            return id;
        } catch (NumberFormatException e) {
            throw badRequest(label + " không hợp lệ");
        }
    }

    /** Chuỗi bắt buộc, đã cắt khoảng trắng, độ dài 1..{@code max}. */
    public static String requiredText(Object raw, String label, int max) {
        String value = raw == null ? "" : raw.toString().trim();
        if (value.isEmpty()) throw badRequest(label + " không được để trống");
        if (value.length() > max) throw badRequest(label + " tối đa " + max + " ký tự");
        return value;
    }

    /** Chuỗi không bắt buộc (rỗng → {@code null}), tối đa {@code max} ký tự. */
    public static String optionalText(Object raw, String label, int max) {
        String value = raw == null ? "" : raw.toString().trim();
        if (value.isEmpty()) return null;
        if (value.length() > max) throw badRequest(label + " tối đa " + max + " ký tự");
        return value;
    }
}
