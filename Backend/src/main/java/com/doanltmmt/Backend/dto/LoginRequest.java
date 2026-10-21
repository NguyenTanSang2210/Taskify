package com.doanltmmt.Backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequest {
    // Giới hạn độ dài: chuỗi quá dài từng làm ghi audit log lỗi (cột 255) -> HTTP 500
    @NotBlank
    @Size(max = 255)
    private String username;
    @NotBlank
    @Size(max = 255)
    private String password;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

