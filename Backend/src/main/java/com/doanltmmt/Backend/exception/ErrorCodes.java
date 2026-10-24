package com.doanltmmt.Backend.exception;

import org.springframework.http.HttpStatusCode;

/** Mã lỗi trả trong trường {@code code} của mọi phản hồi lỗi (mô tả: {@code docs/error-codes.md}). */
public final class ErrorCodes {

    private ErrorCodes() {
    }

    // --- chung, theo mã HTTP
    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED";
    public static final String CONFLICT = "CONFLICT";
    public static final String PAYLOAD_TOO_LARGE = "PAYLOAD_TOO_LARGE";
    public static final String UNSUPPORTED_MEDIA_TYPE = "UNSUPPORTED_MEDIA_TYPE";
    public static final String TOO_MANY_REQUESTS = "TOO_MANY_REQUESTS";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    // --- dữ liệu đầu vào
    public static final String INVALID_INPUT = "INVALID_INPUT";
    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String MALFORMED_REQUEST = "MALFORMED_REQUEST";
    public static final String DATA_CONFLICT = "DATA_CONFLICT";

    // --- nghiệp vụ
    public static final String INVALID_TRANSITION = "INVALID_TRANSITION";
    public static final String WORKSPACE_PHASE = "WORKSPACE_PHASE";
    public static final String TOPIC_FULL = "TOPIC_FULL";
    public static final String DUPLICATE_REGISTRATION = "DUPLICATE_REGISTRATION";
    public static final String GRADE_LOCKED = "GRADE_LOCKED";
    /** File import (P4-03) không đọc được: sai định dạng, thiếu cột bắt buộc, quá số dòng. */
    public static final String IMPORT_FILE_INVALID = "IMPORT_FILE_INVALID";

    /** Mã mặc định cho một mã HTTP (khi lỗi không mang mã riêng). */
    public static String forStatus(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> BAD_REQUEST;
            case 401 -> UNAUTHORIZED;
            case 403 -> FORBIDDEN;
            case 404 -> NOT_FOUND;
            case 405 -> METHOD_NOT_ALLOWED;
            case 409 -> CONFLICT;
            case 413 -> PAYLOAD_TOO_LARGE;
            case 415 -> UNSUPPORTED_MEDIA_TYPE;
            case 429 -> TOO_MANY_REQUESTS;
            default -> status.is5xxServerError() ? INTERNAL_ERROR : BAD_REQUEST;
        };
    }

    /** Thông báo tiếng Việt mặc định khi lỗi không kèm thông báo riêng. */
    public static String defaultMessage(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "Yêu cầu không hợp lệ.";
            case 401 -> "Phiên đăng nhập không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại.";
            case 403 -> "Bạn không có quyền thực hiện thao tác này.";
            case 404 -> "Không tìm thấy dữ liệu yêu cầu.";
            case 405 -> "Phương thức không được hỗ trợ cho đường dẫn này.";
            case 409 -> "Dữ liệu bị xung đột hoặc trùng lặp.";
            case 413 -> "Dữ liệu gửi lên quá lớn.";
            case 415 -> "Định dạng dữ liệu không được hỗ trợ.";
            case 429 -> "Bạn thao tác quá nhanh, vui lòng thử lại sau.";
            default -> "Lỗi hệ thống, vui lòng thử lại sau.";
        };
    }
}
