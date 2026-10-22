package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.repository.UserRepository;
import com.doanltmmt.Backend.security.AuthUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Trả về {@link AuthUser} (mang thêm id, role, tokenVersion). */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return toAuthUser(user);
    }

    public static AuthUser toAuthUser(User user) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        String roleName = null;
        if (user.getRole() != null) {
            roleName = user.getRole().getName();
            authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));
            user.getRole().getPrivileges().forEach(p -> authorities.add(new SimpleGrantedAuthority(p.getName())));
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_STUDENT"));
        }

        // Chỉ đăng nhập/dùng token được khi tài khoản không bị khóa VÀ đã kích hoạt
        boolean enabled = user.isActive() && user.isActivated();
        return new AuthUser(user.getId(), user.getUsername(), user.getPassword(), authorities,
                enabled, user.getTokenVersion(), roleName);
    }
}
