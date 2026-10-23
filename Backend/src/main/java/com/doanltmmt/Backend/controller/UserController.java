package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.dto.UserDTO;
import com.doanltmmt.Backend.entity.Role;
import com.doanltmmt.Backend.entity.AcademicClass;
import com.doanltmmt.Backend.entity.Department;
import com.doanltmmt.Backend.entity.Lecturer;
import com.doanltmmt.Backend.entity.Student;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.repository.AcademicClassRepository;
import com.doanltmmt.Backend.repository.DepartmentRepository;
import com.doanltmmt.Backend.repository.LecturerRepository;
import com.doanltmmt.Backend.repository.StudentRepository;
import com.doanltmmt.Backend.repository.RoleRepository;
import com.doanltmmt.Backend.repository.UserRepository;
import com.doanltmmt.Backend.entity.OtpPurpose;
import com.doanltmmt.Backend.security.JwtTokenProvider;
import com.doanltmmt.Backend.security.PasswordPolicy;
import com.doanltmmt.Backend.service.OtpService;
import com.doanltmmt.Backend.service.SecurityScopeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.doanltmmt.Backend.util.Inputs;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@SuppressWarnings("null")
public class UserController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StudentRepository studentRepository;
    private final LecturerRepository lecturerRepository;
    private final DepartmentRepository departmentRepository;
    private final AcademicClassRepository classRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.doanltmmt.Backend.service.AuditLogService auditLogService;
    private final SecurityScopeService scope;
    private final JwtTokenProvider tokenProvider;
    private final OtpService otpService;

    public UserController(UserRepository userRepository,
                          RoleRepository roleRepository,
                          StudentRepository studentRepository,
                          LecturerRepository lecturerRepository,
                          DepartmentRepository departmentRepository,
                          AcademicClassRepository classRepository,
                          PasswordEncoder passwordEncoder,
                          com.doanltmmt.Backend.service.AuditLogService auditLogService,
                          SecurityScopeService scope,
                          JwtTokenProvider tokenProvider,
                          OtpService otpService) {
        this.otpService = otpService;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.studentRepository = studentRepository;
        this.lecturerRepository = lecturerRepository;
        this.departmentRepository = departmentRepository;
        this.classRepository = classRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
        this.scope = scope;
        this.tokenProvider = tokenProvider;
    }

    // Tên miền tách theo dấu chấm (mỗi phần không chứa '.') để regex không quay lui đa thức (ReDoS)
    private static final java.util.regex.Pattern EMAIL = java.util.regex.Pattern.compile("^[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)+$");

    static boolean isValidEmail(String email) {
        return email != null && email.length() <= 254 && EMAIL.matcher(email).matches();
    }

    @PutMapping("/{id}/profile")
    @PreAuthorize("isAuthenticated()")
    public UserDTO updateProfile(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        String currentUsername = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        boolean isAdmin = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        
        if (!user.getUsername().equals(currentUsername) && !isAdmin) {
             throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to update other user's profile");
        }

        if (body.containsKey("fullName")) {
            String fullName = body.get("fullName") == null ? "" : body.get("fullName").trim();
            if (fullName.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Họ tên không được để trống");
            }
            user.setFullName(fullName);
        }
        if (body.containsKey("email")) {
            String email = body.get("email") == null ? "" : body.get("email").trim();
            if (email.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email không được để trống");
            }
            if (!isValidEmail(email)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email không hợp lệ");
            }
            boolean usedByAnother = userRepository.findByEmail(email)
                    .filter(other -> !other.getId().equals(user.getId())).isPresent();
            if (usedByAnother) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email đã được sử dụng bởi tài khoản khác");
            }
            user.setEmail(email);
        }
        
        User saved = userRepository.save(user);
        auditLogService.log("UPDATE_PROFILE", "User", saved.getId().toString(), "Updated profile info");
        return new UserDTO(saved);
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("isAuthenticated()")
    public Map<String, String> changePassword(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        String currentUsername = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        if (!user.getUsername().equals(currentUsername)) {
             throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to change other user's password");
        }
        
        String oldPass = body.get("oldPassword");
        String newPass = body.get("newPassword");
        
        if (oldPass == null || !passwordEncoder.matches(oldPass, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu hiện tại không đúng");
        }
        PasswordPolicy.requireValid(newPass, user.getUsername());
        if (passwordEncoder.matches(newPass, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu mới không được trùng mật khẩu hiện tại");
        }
        
        user.setPassword(passwordEncoder.encode(newPass));
        // Vô hiệu MỌI token cũ (kể cả token của phiên đang gọi API này)
        user.setTokenVersion(user.getTokenVersion() + 1);
        User saved = userRepository.save(user);
        auditLogService.log("CHANGE_PASSWORD", "User", saved.getId().toString(), "Password changed");
        // ... nên cấp token mới cho chính phiên hiện tại để người dùng không bị đăng xuất giữa chừng
        String roleName = saved.getRole() != null ? saved.getRole().getName() : "STUDENT";
        return Map.of("token", tokenProvider.generateAccessToken(saved.getUsername(), saved.getId(), roleName, saved.getTokenVersion()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public UserDTO getUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Hồ sơ ĐẦY ĐỦ (email, SĐT, vai trò, khoa) chỉ cho: chính chủ, quản trị hệ thống, quản trị khoa CÙNG khoa.
        // Người khác chỉ thấy id + họ tên.
        User me = scope.requireCurrentUser();
        boolean self = me.getId().equals(user.getId());
        boolean admin = scope.hasRole("ADMIN");
        boolean sameDepartmentAdmin = scope.hasRole("DEPARTMENT_ADMIN")
                && me.getDepartment() != null && user.getDepartment() != null
                && me.getDepartment().getId().equals(user.getDepartment().getId());
        if (self || admin || sameDepartmentAdmin) {
            return new UserDTO(user);
        }
        return UserDTO.limited(user);
    }






    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserDTO> getAll() {
        return userRepository.findAll().stream().map(UserDTO::new).toList();
    }

    @PostMapping({ "", "/create" })
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserDTO create(@RequestBody Map<String, Object> body) {
        String username = Inputs.trimmed(body.get("username"));
        String password = Inputs.trimmed(body.get("password"));
        String fullName = Inputs.trimmed(body.get("fullName"));
        String email = Inputs.trimmedOrNull(body.get("email"));
        String phone = Inputs.trimmedOrNull(body.get("phone"));
        Long roleId = Inputs.optionalId(body.get("roleId"), "roleId");
        Long departmentId = Inputs.optionalId(body.get("departmentId"), "departmentId");
        boolean active = body.get("active") == null || Boolean.parseBoolean(String.valueOf(body.get("active")));

        if (username.isBlank() || fullName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username, fullName are required");
        }
        // Có mật khẩu tạm: bắt buộc đổi ở lần đăng nhập đầu. Không có: gửi mã kích hoạt qua email để người dùng tự đặt.
        boolean sendActivation = password.isBlank();
        if (sendActivation) {
            if (email == null || email.isBlank() || !email.contains("@")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cần nhập email hợp lệ để gửi mã kích hoạt (hoặc đặt mật khẩu tạm cho người dùng)");
            }
        } else {
            PasswordPolicy.requireValid(password, username);
        }
        if (roleId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "roleId is required");
        }
        if (userRepository.findByUsername(username).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already exists");
        }
        if (email != null && !email.isBlank() && userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exists");
        }

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
        String roleName = role.getName() != null ? role.getName().trim().toUpperCase() : "";

        Department dept = null;
        if (departmentId != null) {
            dept = departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found"));
        }
        if (dept == null && !roleName.equals("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "departmentId is required for non-admin user");
        }

        String studentCode = null;
        String className = null;
        Long academicClassId = null;
        AcademicClass academicClass = null;

        if (roleName.equals("STUDENT")) {
            studentCode = Inputs.trimmed(body.get("studentCode"));
            className = Inputs.trimmedOrNull(body.get("className"));
            academicClassId = Inputs.optionalId(body.get("academicClassId"), "academicClassId");

            if (studentCode.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "studentCode is required for STUDENT");
            }
            if ((className == null || className.isBlank()) && academicClassId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "className or academicClassId is required for STUDENT");
            }

            if (academicClassId != null) {
                academicClass = classRepository.findById(academicClassId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "AcademicClass not found"));
                if (dept != null && academicClass.getDepartment() != null && !dept.getId().equals(academicClass.getDepartment().getId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AcademicClass is not in user's department");
                }
            }
        }

        User u = new User();
        u.setUsername(username);
        // Chưa kích hoạt: mật khẩu ngẫu nhiên không ai biết (đăng nhập bị chặn cho tới khi kích hoạt)
        u.setPassword(passwordEncoder.encode(sendActivation ? java.util.UUID.randomUUID().toString() : password));
        u.setActivated(!sendActivation);
        u.setMustChangePassword(!sendActivation);
        u.setFullName(fullName);
        if (email != null && !email.isBlank()) u.setEmail(email);
        if (phone != null && !phone.isBlank()) u.setPhone(phone);
        u.setDepartment(dept);
        u.setRole(role);
        u.setActive(active);
        User saved = userRepository.save(u);

        if (roleName.equals("STUDENT") && !studentRepository.existsById(saved.getId())) {
            Student s = new Student();
            s.setUser(userRepository.getReferenceById(saved.getId()));
            s.setStudentCode(studentCode);
            if (className != null && !className.isBlank()) s.setClassName(className);
            if (academicClass != null) s.setAcademicClass(academicClass);
            studentRepository.save(s);
        }

        if (roleName.equals("LECTURER") && !lecturerRepository.existsById(saved.getId())) {
            Lecturer l = new Lecturer();
            l.setUser(userRepository.getReferenceById(saved.getId()));
            l.setDepartment(dept);
            String degree = Inputs.trimmedOrNull(body.get("degree"));
            String speciality = Inputs.trimmedOrNull(body.get("speciality"));
            if (degree != null && !degree.isBlank()) l.setDegree(degree);
            if (speciality != null && !speciality.isBlank()) l.setSpeciality(speciality);
            lecturerRepository.save(l);
        }

        auditLogService.log("CREATE_USER", "User", saved.getId().toString(), "username=" + saved.getUsername());
        if (sendActivation) {
            otpService.issue(saved, OtpPurpose.ACTIVATION);
        }
        return new UserDTO(saved);
    }

    /** Gửi lại mã kích hoạt cho tài khoản chưa kích hoạt (cách nhau tối thiểu 60 giây). */
    @PostMapping("/{id}/resend-activation")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> resendActivation(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.isActivated()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tài khoản này đã được kích hoạt");
        }
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tài khoản chưa có email để gửi mã kích hoạt");
        }
        otpService.issueWithCooldown(user, OtpPurpose.ACTIVATION);
        auditLogService.log("RESEND_ACTIVATION", "User", user.getId().toString(), "username=" + user.getUsername());
        return Map.of("sent", true, "cooldownSeconds", OtpService.RESEND_COOLDOWN_SECONDS);
    }

    @GetMapping("/by-department")
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_ADMIN')")
    public List<UserDTO> listByDepartment(@RequestParam(required = false) Long departmentId,
                                       @RequestParam(required = false) String role) {
        if (departmentId == null && !scope.hasRole("ADMIN")) {
            departmentId = scope.requireCurrentUser().getDepartment() != null ? scope.requireCurrentUser().getDepartment().getId() : null;
        }
        scope.requireDepartmentAccess(departmentId);
        if (role != null && !role.isBlank()) {
            return userRepository.findByDepartment_IdAndRole_NameOrderByIdDesc(departmentId, role.trim().toUpperCase()).stream().map(UserDTO::new).toList();
        }
        return userRepository.findByDepartment_IdOrderByIdDesc(departmentId).stream().map(UserDTO::new).toList();
    }

    @GetMapping("/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDTO updateRole(@PathVariable Long id, @RequestParam Long roleId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
        
        String oldRole = user.getRole() != null ? user.getRole().getName() : "None";
        user.setRole(role);
        user.setTokenVersion(user.getTokenVersion() + 1); // quyền đổi → token cũ (mang role cũ) không còn hiệu lực
        User saved = userRepository.save(user);
        
        auditLogService.log("UPDATE_USER_ROLE", "User", saved.getId().toString(), "Changed role from " + oldRole + " to " + role.getName());
        return new UserDTO(saved);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDTO updateStatus(@PathVariable Long id, @RequestParam Boolean active) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setActive(active);
        if (!active) {
            user.setTokenVersion(user.getTokenVersion() + 1); // khóa tài khoản → token đang dùng chết ngay
        }
        User saved = userRepository.save(user);
        auditLogService.log(active ? "UNLOCK_USER" : "LOCK_USER", "User", saved.getId().toString(), "Updated active status to " + active);
        return new UserDTO(saved);
    }


}



