package com.doanltmmt.Backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    @JsonIgnore
    private String password; // sau sẽ mã hoá

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(unique = true, length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    /** false = bị quản trị viên khóa. */
    @Column(nullable = false)
    private boolean active = true;

    /**
     * Phiên bản token của người dùng. Mỗi JWT mang giá trị này (claim "tv"); tăng lên khi khóa tài khoản,
     * đổi role, đổi/đặt lại mật khẩu, "đăng xuất mọi nơi" → mọi token đang lưu hành lập tức vô hiệu.
     */
    @Column(name = "token_version", nullable = false)
    @ColumnDefault("0")
    private int tokenVersion = 0;

    /** false = tài khoản do quản trị viên tạo, người dùng chưa kích hoạt (chưa tự đặt mật khẩu). */
    @Column(nullable = false)
    @ColumnDefault("1")
    private boolean activated = true;

    /** true = đang dùng mật khẩu tạm do quản trị viên đặt, bắt buộc đổi ở lần đăng nhập đầu tiên. */
    @Column(name = "must_change_password", nullable = false)
    @ColumnDefault("0")
    private boolean mustChangePassword = false;
}
