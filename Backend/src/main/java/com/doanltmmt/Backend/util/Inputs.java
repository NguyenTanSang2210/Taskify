package com.doanltmmt.Backend.util;

import com.doanltmmt.Backend.exception.ApiException;
import com.doanltmmt.Backend.exception.ErrorCodes;
import org.springframework.web.server.ResponseStatusException;

/**
 * Lỗi 400 cho dữ liệu đầu vào sai nghiệp vụ mà Bean Validation không diễn đạt được (cần dữ liệu trong CSDL,
 * vd. ngày ngoài thời gian của đợt). Kiểm tra định dạng/độ dài nằm ở request DTO ({@code dto.request}, P3-02).
 */
public final class Inputs {

    private Inputs() {
    }

    public static ResponseStatusException badRequest(String message) {
        return ApiException.badRequest(ErrorCodes.INVALID_INPUT, message);
    }
}
