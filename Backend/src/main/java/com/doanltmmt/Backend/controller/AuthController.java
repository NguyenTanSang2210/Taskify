package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.dto.LoginRequest;
import com.doanltmmt.Backend.dto.LoginResponse;
import com.doanltmmt.Backend.entity.Student;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.repository.RoleRepository;
import com.doanltmmt.Backend.repository.StudentRepository;
import com.doanltmmt.Backend.repository.UserRepository;
import com.doanltmmt.Backend.security.JwtTokenProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Instant;
import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final com.doanltmmt.Backend.service.OtpService otpService;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final StudentRepository studentRepository;

    private static final ConcurrentHashMap<String, Integer> ATTEMPTS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> LOCKED_UNTIL = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(10);

    public AuthController(AuthenticationManager authenticationManager, JwtTokenProvider tokenProvider,
                          UserRepository userRepository,
                          com.doanltmmt.Backend.service.OtpService otpService,
                          PasswordEncoder passwordEncoder,
                          RoleRepository roleRepository,
                          StudentRepository studentRepository) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userRepository = userRepository;
        this.otpService = otpService;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.studentRepository = studentRepository;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        try {
            String username = request.getUsername();
            Long now = Instant.now().toEpochMilli();
            Long lockUntil = LOCKED_UNTIL.getOrDefault(username, 0L);
            if (now < lockUntil) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,
                        "Tài khoản tạm khóa do đăng nhập sai nhiều lần. Vui lòng thử lại sau."
                );
            }

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            User user = userRepository.findByUsername(username).orElseThrow();
            String roleName = user.getRole() != null ? user.getRole().getName() : "STUDENT";
            Map<String, Object> claims = new HashMap<>();
            claims.put("role", roleName);
            claims.put("userId", user.getId());

            String token = tokenProvider.generateToken(user.getUsername(), claims);
            ATTEMPTS.remove(username);

            boolean otpRequired = "ADMIN".equals(roleName) || "LECTURER".equals(roleName) || "DEPARTMENT_ADMIN".equals(roleName);
            if (otpRequired) {
                otpService.sendOtp(username);
            }
            return new LoginResponse(token, user.getId(), user.getUsername(), user.getFullName(), roleName, otpRequired);
        } catch (org.springframework.security.core.AuthenticationException ex) {
            String username = request.getUsername();
            int n = ATTEMPTS.getOrDefault(username, 0) + 1;
            ATTEMPTS.put(username, n);
            if (n >= MAX_ATTEMPTS) {
                LOCKED_UNTIL.put(username, Instant.now().plus(LOCK_DURATION).toEpochMilli());
                ATTEMPTS.remove(username);
            }
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "Sai tên đăng nhập hoặc mật khẩu"
            );
        }
    }

    @PostMapping("/register")
    public User register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String fullName = body.get("fullName");
        String email = body.get("email");
        String studentCode = body.get("studentCode");
        String className = body.get("className");

        if (userRepository.findByUsername(username).isPresent()) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên đăng nhập đã tồn tại");
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

        return savedUser;
    }

    @PostMapping("/otp/verify")
    public Map<String, Object> verifyOtp(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String code = body.get("code");
        boolean ok = otpService.verifyOtp(username, code);
        Map<String, Object> resp = new HashMap<>();
        resp.put("verified", ok);
        return resp;
    }
}
