package com.doanltmmt.Backend.dto;

public class LoginResponse {
    private String token;
    private Long userId;
    private String username;
    private String fullName;
    private String role;
    private boolean otpRequired;

    public LoginResponse(String token, Long userId, String username, String fullName, String role, boolean otpRequired) {
        this.token = token; this.userId = userId; this.username = username; this.fullName = fullName; this.role = role; this.otpRequired = otpRequired;
    }

    public String getToken() { return token; }
    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }
    public boolean isOtpRequired() { return otpRequired; }
}
