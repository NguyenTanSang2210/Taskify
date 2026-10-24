package com.doanltmmt.Backend.config;

import com.doanltmmt.Backend.entity.OtpPurpose;
import com.doanltmmt.Backend.entity.Role;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.repository.RoleRepository;
import com.doanltmmt.Backend.repository.UserRepository;
import com.doanltmmt.Backend.service.AuditLogService;
import com.doanltmmt.Backend.service.OtpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Tạo ADMIN đầu tiên cho bản cài mới: chỉ khi chưa có ADMIN, tài khoản chưa kích hoạt + mã kích hoạt qua email. */
class AdminBootstrapTest {

    private final UserRepository userRepo = mock(UserRepository.class);
    private final RoleRepository roleRepo = mock(RoleRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final OtpService otpService = mock(OtpService.class);
    private final AuditLogService audit = mock(AuditLogService.class);
    private final Role adminRole = new Role();

    @BeforeEach
    void setUp() {
        adminRole.setName("ADMIN");
        when(roleRepo.findByName("ADMIN")).thenReturn(Optional.of(adminRole));
        when(encoder.encode(anyString())).thenReturn("{hash}");
        when(userRepo.findByUsername(anyString())).thenReturn(Optional.empty());
        when(userRepo.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepo.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
    }

    private AdminBootstrap bootstrap(String username, String email) {
        return new AdminBootstrap(userRepo, roleRepo, encoder, otpService, audit, username, email, "");
    }

    @Test
    void noAdminYet_createsInactiveAdmin_andSendsActivationCode() {
        when(userRepo.existsByRole_Name("ADMIN")).thenReturn(false);

        bootstrap(" truongkhoa ", "it@truong.edu.vn").run(null);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(saved.capture());
        User u = saved.getValue();
        assertEquals("truongkhoa", u.getUsername());
        assertEquals("it@truong.edu.vn", u.getEmail());
        assertEquals(adminRole, u.getRole());
        assertFalse(u.isActivated(), "phải kích hoạt bằng mã qua email, không có mật khẩu trong cấu hình");
        assertTrue(u.isActive());
        assertEquals("Quản trị viên", u.getFullName());
        verify(otpService).issue(u, OtpPurpose.ACTIVATION);
    }

    @Test
    void adminAlreadyExists_doesNothing() {
        when(userRepo.existsByRole_Name("ADMIN")).thenReturn(true);

        bootstrap("truongkhoa", "it@truong.edu.vn").run(null);

        verify(userRepo, never()).save(any());
        verify(otpService, never()).issue(any(), any());
    }

    @Test
    void notConfigured_doesNotEvenQuery() {
        bootstrap("", "").run(null);

        verify(userRepo, never()).existsByRole_Name(anyString());
        verify(userRepo, never()).save(any());
    }

    @Test
    void incompleteOrConflictingConfig_isSkipped() {
        when(userRepo.existsByRole_Name("ADMIN")).thenReturn(false);
        bootstrap("truongkhoa", "").run(null);
        bootstrap("", "it@truong.edu.vn").run(null);
        bootstrap("truongkhoa", "khong-phai-email").run(null);

        when(userRepo.findByUsername("student")).thenReturn(Optional.of(new User()));
        bootstrap("student", "it@truong.edu.vn").run(null);

        verify(userRepo, never()).save(any());
        verify(otpService, never()).issue(any(), any());
    }
}
