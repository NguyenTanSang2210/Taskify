package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.dto.LoginRequest;
import com.doanltmmt.Backend.dto.LoginResponse;
import com.doanltmmt.Backend.dto.OtpResendRequest;
import com.doanltmmt.Backend.dto.OtpResendResponse;
import com.doanltmmt.Backend.dto.OtpVerifyRequest;
import com.doanltmmt.Backend.dto.UserDTO;
import com.doanltmmt.Backend.entity.Student;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.repository.RoleRepository;
import com.doanltmmt.Backend.repository.StudentRepository;
import com.doanltmmt.Backend.repository.UserRepository;
import com.doanltmmt.Backend.service.AuditLogService;
import com.doanltmmt.Backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Xác thực. Logic đăng nhập nhiều bước nằm ở {@link AuthService}; controller chỉ nhận request, lấy IP client
 * và trả kết quả.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final StudentRepository studentRepository;
    private final AuditLogService auditLogService;

    public AuthController(AuthService authService,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          RoleRepository roleRepository,
                          StudentRepository studentRepository,
                          AuditLogService auditLogService) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.studentRepository = studentRepository;
        this.auditLogService = auditLogService;
    }

    /** Bước 1: tên đăng nhập + mật khẩu. Vai trò cần OTP chỉ nhận token tạm, chưa gọi được API nghiệp vụ. */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return authService.login(request, clientIp(http));
    }

    /** Bước 2: nhập OTP. Người dùng được xác định từ preAuthToken, không nhận username từ client. */
    @PostMapping("/otp/verify")
    public LoginResponse verifyOtp(@Valid @RequestBody OtpVerifyRequest request, HttpServletRequest http) {
        return authService.verifyOtp(request.preAuthToken(), request.code(), clientIp(http));
    }

    /** Gửi lại OTP (chờ tối thiểu 60 giây giữa hai lần; mã cũ bị hủy). */
    @PostMapping("/otp/resend")
    public OtpResendResponse resendOtp(@Valid @RequestBody OtpResendRequest request, HttpServletRequest http) {
        return authService.resendOtp(request.preAuthToken(), clientIp(http));
    }

    /** Đăng xuất mọi thiết bị: tăng tokenVersion → mọi token đang lưu hành của người dùng này lập tức vô hiệu. */
    @PostMapping("/logout-all")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> logoutAll() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
        auditLogService.log("LOGOUT_ALL", "User", user.getId().toString(), "Đăng xuất mọi thiết bị");
        return Map.of("loggedOut", true);
    }

    // TODO(P1-09): xóa endpoint đăng ký công khai (quyết định D-02: tài khoản do quản trị tạo, người dùng tự kích hoạt)
    @PostMapping("/register")
    public UserDTO register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String fullName = body.get("fullName");
        String email = body.get("email");
        String studentCode = body.get("studentCode");
        String className = body.get("className");

        if (userRepository.findByUsername(username).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên đăng nhập đã tồn tại");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setEmail(email);
        user.setActive(true);
        user.setRole(roleRepository.findByName("STUDENT").orElseThrow(() -> new RuntimeException("Role STUDENT not found")));

        User savedUser = userRepository.save(user);

        Student student = new Student();
        student.setUser(savedUser);
        student.setStudentCode(studentCode);
        student.setClassName(className);
        studentRepository.save(student);

        return new UserDTO(savedUser);
    }

    /** IP client (đã qua {@code server.forward-headers-strategy=native} nên là IP thật khi đứng sau nginx). */
    private static String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
