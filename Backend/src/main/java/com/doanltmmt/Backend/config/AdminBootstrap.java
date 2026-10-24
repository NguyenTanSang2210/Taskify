package com.doanltmmt.Backend.config;

import com.doanltmmt.Backend.entity.OtpPurpose;
import com.doanltmmt.Backend.entity.Role;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.repository.RoleRepository;
import com.doanltmmt.Backend.repository.UserRepository;
import com.doanltmmt.Backend.service.AuditLogService;
import com.doanltmmt.Backend.service.OtpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Tạo tài khoản ADMIN đầu tiên cho bản cài production mới (seed demo chỉ chạy ở profile dev).
 * <p>
 * Chỉ chạy khi đặt {@code app.bootstrap.admin.username} + {@code app.bootstrap.admin.email} VÀ hệ thống chưa có
 * tài khoản ADMIN nào. Tài khoản được tạo ở trạng thái <b>chưa kích hoạt</b> và nhận mã kích hoạt qua email để tự đặt
 * mật khẩu (cùng luồng với tài khoản do Admin tạo) — không có mật khẩu nào nằm trong biến môi trường.
 * Khi đã có ADMIN, các biến này bị bỏ qua (nên xóa sau lần chạy đầu).
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);
    private static final String ADMIN = "ADMIN";

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final AuditLogService auditLogService;
    private final String username;
    private final String email;
    private final String fullName;

    public AdminBootstrap(UserRepository userRepo, RoleRepository roleRepo, PasswordEncoder passwordEncoder,
                          OtpService otpService, AuditLogService auditLogService,
                          @Value("${app.bootstrap.admin.username:}") String username,
                          @Value("${app.bootstrap.admin.email:}") String email,
                          @Value("${app.bootstrap.admin.full-name:Quản trị viên}") String fullName) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.auditLogService = auditLogService;
        this.username = username == null ? "" : username.trim();
        this.email = email == null ? "" : email.trim();
        this.fullName = fullName == null || fullName.isBlank() ? "Quản trị viên" : fullName.trim();
    }

    @Override
    public void run(ApplicationArguments args) {
        if (username.isEmpty() && email.isEmpty()) return;
        if (userRepo.existsByRole_Name(ADMIN)) {
            log.info("Đã có tài khoản ADMIN — bỏ qua APP_BOOTSTRAP_ADMIN_* (có thể xóa các biến này).");
            return;
        }
        if (username.isEmpty() || !email.contains("@")) {
            log.warn("Bỏ qua tạo ADMIN đầu tiên: cần cả APP_BOOTSTRAP_ADMIN_USERNAME và APP_BOOTSTRAP_ADMIN_EMAIL hợp lệ.");
            return;
        }
        if (userRepo.findByUsername(username).isPresent() || userRepo.findByEmail(email).isPresent()) {
            log.warn("Bỏ qua tạo ADMIN đầu tiên: tên đăng nhập hoặc email đã thuộc một tài khoản khác.");
            return;
        }
        Role role = roleRepo.findByName(ADMIN).orElseThrow(() -> new IllegalStateException("Thiếu vai trò ADMIN (migration V2)"));

        User admin = new User();
        admin.setUsername(username);
        // Chưa kích hoạt: mật khẩu ngẫu nhiên không ai biết, đăng nhập bị chặn cho tới khi kích hoạt bằng mã qua email
        admin.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        admin.setActivated(false);
        admin.setFullName(fullName);
        admin.setEmail(email);
        admin.setRole(role);
        User saved = userRepo.save(admin);
        auditLogService.log("BOOTSTRAP_ADMIN", "User", saved.getId().toString(), "username=" + username, "SYSTEM");
        otpService.issue(saved, OtpPurpose.ACTIVATION);
        log.info("Đã tạo tài khoản ADMIN đầu tiên '{}' — mã kích hoạt đã gửi tới email đã cấu hình.", username);
    }
}
