package com.doanltmmt.Backend.dto.response;

import com.doanltmmt.Backend.entity.User;

/** Thông tin tối thiểu của người dùng khi được nhúng trong phản hồi khác — KHÔNG có email, SĐT, vai trò. */
public record UserSummaryDTO(Long id, String username, String fullName) {
    public static UserSummaryDTO from(User u) {
        return u == null ? null : new UserSummaryDTO(u.getId(), u.getUsername(), u.getFullName());
    }
}
