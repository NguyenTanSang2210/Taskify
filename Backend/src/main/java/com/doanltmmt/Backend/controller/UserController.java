package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.dto.UserDTO;
import com.doanltmmt.Backend.dto.request.ChangePasswordRequest;
import com.doanltmmt.Backend.dto.request.ProfileUpdateRequest;
import com.doanltmmt.Backend.dto.request.UserCreateRequest;
import com.doanltmmt.Backend.entity.AcademicClass;
import com.doanltmmt.Backend.entity.Department;
import com.doanltmmt.Backend.entity.Lecturer;
import com.doanltmmt.Backend.entity.OtpPurpose;
import com.doanltmmt.Backend.entity.Role;
import com.doanltmmt.Backend.entity.Student;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.repository.AcademicClassRepository;
import com.doanltmmt.Backend.repository.DepartmentRepository;
import com.doanltmmt.Backend.repository.LecturerRepository;
import com.doanltmmt.Backend.repository.RoleRepository;
import com.doanltmmt.Backend.repository.StudentRepository;
import com.doanltmmt.Backend.repository.UserRepository;
import com.doanltmmt.Backend.security.JwtTokenProvider;
import com.doanltmmt.Backend.security.PasswordPolicy;
import com.doanltmmt.Backend.service.OtpService;
import com.doanltmmt.Backend.service.SecurityScopeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
    public UserDTO updateProfile(@PathVariable Long id, @Valid @RequestBody ProfileUpdateRequest body) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng."));
        
        String currentUsername = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        boolean isAdmin = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        
        if (!user.getUsername().equals(currentUsername) && !isAdmin) {
             throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không thể sửa hồ sơ của người khác.");
        }

        if (body.fullName() != null) {
            user.setFullName(body.fullName());
        }
        if (body.email() != null) {
            String email = body.email();
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
    public Map<String, String> changePassword(@PathVariable Long id, @Valid @RequestBody ChangePasswordRequest body) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng."));
        
        String currentUsername = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        if (!user.getUsername().equals(currentUsername)) {
             throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không thể đổi mật khẩu của người khác.");
        }
        
        String oldPass = body.oldPassword();
        String newPass = body.newPassword();

        if (!passwordEncoder.matches(oldPass, user.getPassword())) {
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng."));

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
    public UserDTO create(@Valid @RequestBody UserCreateRequest body) {
        String username = body.username();
        String password = body.password();
        String fullName = body.fullName();
        String email = body.email();
        String phone = body.phone();
        Long roleId = body.roleId();
        Long departmentId = body.departmentId();
        boolean active = body.active() == null || body.active();

        // Có mật khẩu tạm: bắt buộc đổi ở lần đăng nhập đầu. Không có: gửi mã kích hoạt qua email để người dùng tự đặt.
        boolean sendActivation = password == null;
        if (sendActivation) {
            if (email == null || !email.contains("@")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cần nhập email hợp lệ để gửi mã kích hoạt (hoặc đặt mật khẩu tạm cho người dùng)");
            }
        } else {
            PasswordPolicy.requireValid(password, username);
        }
        if (roleId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn vai trò.");
        }
        if (userRepository.findByUsername(username).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên đăng nhập đã tồn tại.");
        }
        if (email != null && userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email đã được sử dụng.");
        }

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy vai trò."));
        String roleName = role.getName() != null ? role.getName().trim().toUpperCase() : "";

        Department dept = null;
        if (departmentId != null) {
            dept = departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khoa."));
        }
        if (dept == null && !roleName.equals("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tài khoản không phải quản trị hệ thống phải thuộc một khoa (departmentId).");
        }

        String studentCode = null;
        String className = null;
        Long academicClassId = null;
        AcademicClass academicClass = null;

        if (roleName.equals("STUDENT")) {
            studentCode = body.studentCode();
            className = body.className();
            academicClassId = body.academicClassId();

            if (studentCode == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sinh viên phải có mã sinh viên.");
            }
            if (className == null && academicClassId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sinh viên phải thuộc một lớp.");
            }

            if (academicClassId != null) {
                academicClass = classRepository.findById(academicClassId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy lớp."));
                if (dept != null && academicClass.getDepartment() != null && !dept.getId().equals(academicClass.getDepartment().getId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lớp không thuộc khoa của người dùng.");
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
        u.setEmail(email);
        u.setPhone(phone);
        u.setDepartment(dept);
        u.setRole(role);
        u.setActive(active);
        User saved = userRepository.save(u);

        if (roleName.equals("STUDENT") && !studentRepository.existsById(saved.getId())) {
            Student s = new Student();
            s.setUser(userRepository.getReferenceById(saved.getId()));
            s.setStudentCode(studentCode);
            if (className != null) s.setClassName(className);
            if (academicClass != null) s.setAcademicClass(academicClass);
            studentRepository.save(s);
        }

        if (roleName.equals("LECTURER") && !lecturerRepository.existsById(saved.getId())) {
            Lecturer l = new Lecturer();
            l.setUser(userRepository.getReferenceById(saved.getId()));
            l.setDepartment(dept);
            if (body.degree() != null) l.setDegree(body.degree());
            if (body.speciality() != null) l.setSpeciality(body.speciality());
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng."));
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng."));
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy vai trò."));
        
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng."));
        user.setActive(active);
        if (!active) {
            user.setTokenVersion(user.getTokenVersion() + 1); // khóa tài khoản → token đang dùng chết ngay
        }
        User saved = userRepository.save(user);
        auditLogService.log(active ? "UNLOCK_USER" : "LOCK_USER", "User", saved.getId().toString(), "Updated active status to " + active);
        return new UserDTO(saved);
    }


}



