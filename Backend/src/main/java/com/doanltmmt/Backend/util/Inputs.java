package com.doanltmmt.Backend.util;

import com.doanltmmt.Backend.exception.ErrorCodes;
import com.doanltmmt.Backend.exception.ApiException;
import com.doanltmmt.Backend.util.Inputs;
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
        return ApiException.badRequest(ErrorCodes.INVALID_INPUT, message);
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

    /** Mã định danh không bắt buộc: rỗng → {@code null}, sai định dạng → 400. */
    public static Long optionalId(Object raw, String label) {
        return raw == null || raw.toString().isBlank() ? null : requiredId(raw, label);
    }

    /** Số nguyên không âm không bắt buộc: rỗng → {@code defaultValue}, sai → 400. */
    public static int optionalNonNegativeInt(Object raw, String label, int defaultValue) {
        if (raw == null || raw.toString().isBlank()) return defaultValue;
        try {
            int value = Integer.parseInt(raw.toString().trim());
            if (value < 0) throw badRequest(label + " không được âm");
            return value;
        } catch (NumberFormatException e) {
            throw badRequest(label + " phải là số nguyên");
        }
    }

    /** Chuỗi đã cắt khoảng trắng; thiếu hoặc JSON {@code null} → chuỗi rỗng (không thành chữ "null"). */
    public static String trimmed(Object raw) {
        return raw == null ? "" : raw.toString().trim();
    }

    /** Như {@link #trimmed} nhưng giữ {@code null} khi không gửi — dùng cho trường sửa từng phần. */
    public static String trimmedOrNull(Object raw) {
        return raw == null ? null : raw.toString().trim();
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
