package com.doanltmmt.Backend.security;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Objects;

/**
 * Principal của Spring Security. Khác {@code org.springframework.security.core.userdetails.User}
 * ở chỗ mang thêm {@code id}, {@code roleName} và {@code tokenVersion} — cần để (1) so khớp phiên bản token,
 * (2) kiểm tra quyền theo đối tượng mà không phải truy vấn lại (kể cả trong luồng WebSocket, nơi không có
 * SecurityContext của request).
 *
 * <p>{@code getName()} của Authentication vẫn trả về username (Spring lấy từ {@link UserDetails#getUsername()}),
 * nên mã cũ dùng {@code auth.getName()} hoạt động như trước.
 */
public class AuthUser implements UserDetails, CredentialsContainer {

    private final Long id;
    private final String username;
    private String passwordHash;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean enabled;
    private final int tokenVersion;
    private final String roleName;

    public AuthUser(Long id, String username, String passwordHash,
                    Collection<? extends GrantedAuthority> authorities,
                    boolean enabled, int tokenVersion, String roleName) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.authorities = authorities;
        this.enabled = enabled;
        this.tokenVersion = tokenVersion;
        this.roleName = roleName;
    }

    public Long getId() {
        return id;
    }

    public int getTokenVersion() {
        return tokenVersion;
    }

    /** Tên vai trò (ADMIN, LECTURER, ...) hoặc null nếu người dùng chưa có vai trò. */
    public String getRoleName() {
        return roleName;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** false nếu tài khoản bị khóa hoặc chưa kích hoạt. */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /** Xóa băm mật khẩu khỏi bộ nhớ sau khi xác thực xong (giống hành vi của lớp User chuẩn của Spring). */
    @Override
    public void eraseCredentials() {
        this.passwordHash = null;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AuthUser other && Objects.equals(username, other.username);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(username);
    }

    @Override
    public String toString() {
        return "AuthUser[id=" + id + ", username=" + username + ", role=" + roleName + "]";
    }
}
