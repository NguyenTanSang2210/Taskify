package com.doanltmmt.Backend.dto;

import jakarta.validation.constraints.NotBlank;

public record OtpResendRequest(@NotBlank(message = "Thiếu phiên xác thực") String preAuthToken) {
}
