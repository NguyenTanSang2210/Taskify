package com.doanltmmt.Backend.dto;

public record OtpResendResponse(boolean sent, int cooldownSeconds, String maskedEmail) {
}
