package com.doanltmmt.Backend.dto;

import com.doanltmmt.Backend.entity.Department;
import com.doanltmmt.Backend.entity.Role;
import com.doanltmmt.Backend.entity.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserDTO {
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private Role role;
    private Department department;
    private boolean active;

    public UserDTO(User user) {
        if (user == null) return;
        this.id = user.getId();
        this.username = user.getUsername();
        this.fullName = user.getFullName();
        this.email = user.getEmail();
        this.phone = user.getPhone();
        this.role = user.getRole();
        this.department = user.getDepartment();
        this.active = user.isActive();
    }

    /**
     * Hồ sơ rút gọn để người khác xem: CHỈ id và họ tên — không có email, số điện thoại, vai trò, khoa.
     * Dùng khi người gọi không phải chính chủ / quản trị hệ thống / quản trị khoa cùng khoa.
     */
    public static UserDTO limited(User user) {
        UserDTO dto = new UserDTO();
        if (user == null) return dto;
        dto.id = user.getId();
        dto.fullName = user.getFullName();
        return dto;
    }
}
