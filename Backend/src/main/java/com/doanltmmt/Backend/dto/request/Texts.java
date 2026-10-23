package com.doanltmmt.Backend.dto.request;


/** Chuẩn hóa chuỗi trong compact constructor của request DTO (chạy trước Bean Validation). */
public final class Texts {

    private Texts() {
    }

    /** Cắt khoảng trắng hai đầu; {@code null} giữ nguyên. */
    public static String trim(String value) {
        return value == null ? null : value.trim();
    }

    /** Cắt khoảng trắng; chuỗi rỗng → {@code null} (trường không bắt buộc). */
    public static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
