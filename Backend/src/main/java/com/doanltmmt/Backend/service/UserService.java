package com.doanltmmt.Backend.service;

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
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/** Quản lý tài khoản: hồ sơ, mật khẩu, tạo tài khoản (kèm hồ sơ sinh viên/giảng viên), vai trò, khóa/mở. */
@Service
@SuppressWarnings("null")
public class UserService {

    // Tên miền tách theo dấu chấm (mỗi phần không chứa '.') để regex không quay lui đa thức (ReDoS)
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)+$");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StudentRepository studentRepository;
    private final LecturerRepository lecturerRepository;
    private final DepartmentRepository departmentRepository;
    private final AcademicClassRepository classRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final SecurityScopeService scope;
    private final JwtTokenProvider tokenProvider;
    private final OtpService otpService;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, StudentRepository studentRepository,
                       LecturerRepository lecturerRepository, DepartmentRepository departmentRepository,
                       AcademicClassRepository classRepository, PasswordEncoder passwordEncoder,
                       AuditLogService auditLogService, SecurityScopeService scope, JwtTokenProvider tokenProvider,
                       OtpService otpService) {
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
        this.otpService = otpService;
    }

    public static boolean isValidEmail(String email) {
        return email != null && email.length() <= 254 && EMAIL.matcher(email).matches();
    }

    @Transactional
    public UserDTO updateProfile(Long id, ProfileUpdateRequest body) {
        User user = load(id);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!user.getUsername().equals(auth.getName()) && !isAdmin) {
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
        auditLogService.log("UPDATE_PROFILE", "User", saved.getId().toString(), "Cập nhật hồ sơ");
        return new UserDTO(saved);
    }

    /** Đổi mật khẩu; trả token mới cho phiên hiện tại (mọi token cũ, kể cả của phiên này, bị vô hiệu). */
    @Transactional
    public Map<String, String> changePassword(Long id, ChangePasswordRequest body) {
        User user = load(id);
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!user.getUsername().equals(currentUsername)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không thể đổi mật khẩu của người khác.");
        }
        String newPass = body.newPassword();
        if (!passwordEncoder.matches(body.oldPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu hiện tại không đúng");
        }
        PasswordPolicy.requireValid(newPass, user.getUsername());
        if (passwordEncoder.matches(newPass, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu mới không được trùng mật khẩu hiện tại");
        }

        user.setPassword(passwordEncoder.encode(newPass));
        user.setTokenVersion(user.getTokenVersion() + 1);
        User saved = userRepository.save(user);
        auditLogService.log("CHANGE_PASSWORD", "User", saved.getId().toString(), "Đổi mật khẩu");
        String roleName = saved.getRole() != null ? saved.getRole().getName() : "STUDENT";
        return Map.of("token", tokenProvider.generateAccessToken(saved.getUsername(), saved.getId(), roleName, saved.getTokenVersion()));
    }

    /**
     * Hồ sơ ĐẦY ĐỦ (email, SĐT, vai trò, khoa) chỉ cho: chính chủ, quản trị hệ thống, quản trị khoa CÙNG khoa.
     * Người khác chỉ thấy id + họ tên.
     */
    @Transactional(readOnly = true)
    public UserDTO get(Long id) {
        User user = load(id);
        User me = scope.requireCurrentUser();
        boolean self = me.getId().equals(user.getId());
        boolean admin = scope.hasRole("ADMIN");
        boolean sameDepartmentAdmin = scope.hasRole("DEPARTMENT_ADMIN")
                && me.getDepartment() != null && user.getDepartment() != null
                && me.getDepartment().getId().equals(user.getDepartment().getId());
        return self || admin || sameDepartmentAdmin ? new UserDTO(user) : UserDTO.limited(user);
    }

    @Transactional(readOnly = true)
    public List<UserDTO> listAll() {
        return userRepository.findAll().stream().map(UserDTO::new).toList();
    }

    @Transactional
    public UserDTO create(UserCreateRequest body) {
        String username = body.username();
        String password = body.password();
        String email = body.email();
        Long roleId = body.roleId();

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
        String roleName = role.getName() != null ? role.getName().trim().toUpperCase(Locale.ROOT) : "";

        Department dept = null;
        if (body.departmentId() != null) {
            dept = departmentRepository.findById(body.departmentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khoa."));
        }
        if (dept == null && !roleName.equals("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tài khoản không phải quản trị hệ thống phải thuộc một khoa (departmentId).");
        }

        AcademicClass academicClass = null;
        if (roleName.equals("STUDENT")) {
            if (body.studentCode() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sinh viên phải có mã sinh viên.");
            }
            if (body.className() == null && body.academicClassId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sinh viên phải thuộc một lớp.");
            }
            if (body.academicClassId() != null) {
                academicClass = classRepository.findById(body.academicClassId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy lớp."));
                if (dept != null && academicClass.getDepartment() != null && !dept.getId().equals(academicClass.getDepartment().getId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lớp không thuộc khoa của người dùng.");
                }
            }
        }

        User u = new User();
        u.setUsername(username);
        // Chưa kích hoạt: mật khẩu ngẫu nhiên không ai biết (đăng nhập bị chặn cho tới khi kích hoạt)
        u.setPassword(passwordEncoder.encode(sendActivation ? UUID.randomUUID().toString() : password));
        u.setActivated(!sendActivation);
        u.setMustChangePassword(!sendActivation);
        u.setFullName(body.fullName());
        u.setEmail(email);
        u.setPhone(body.phone());
        u.setDepartment(dept);
        u.setRole(role);
        u.setActive(body.active() == null || body.active());
        User saved = userRepository.save(u);

        if (roleName.equals("STUDENT") && !studentRepository.existsById(saved.getId())) {
            Student s = new Student();
            s.setUser(userRepository.getReferenceById(saved.getId()));
            s.setStudentCode(body.studentCode());
            if (body.className() != null) s.setClassName(body.className());
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
    @Transactional
    public Map<String, Object> resendActivation(Long id) {
        User user = load(id);
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

    @Transactional(readOnly = true)
    public List<UserDTO> listByDepartment(Long departmentId, String role) {
        if (departmentId == null && !scope.hasRole("ADMIN")) {
            User me = scope.requireCurrentUser();
            departmentId = me.getDepartment() != null ? me.getDepartment().getId() : null;
        }
        scope.requireDepartmentAccess(departmentId);
        List<User> users = role != null && !role.isBlank()
                ? userRepository.findByDepartment_IdAndRole_NameOrderByIdDesc(departmentId, role.trim().toUpperCase(Locale.ROOT))
                : userRepository.findByDepartment_IdOrderByIdDesc(departmentId);
        return users.stream().map(UserDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public List<Role> listRoles() {
        return roleRepository.findAll();
    }

    @Transactional
    public UserDTO updateRole(Long id, Long roleId) {
        User user = load(id);
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy vai trò."));
        String oldRole = user.getRole() != null ? user.getRole().getName() : "None";
        user.setRole(role);
        user.setTokenVersion(user.getTokenVersion() + 1); // quyền đổi → token cũ (mang role cũ) không còn hiệu lực
        User saved = userRepository.save(user);
        auditLogService.log("UPDATE_USER_ROLE", "User", saved.getId().toString(), "Đổi vai trò " + oldRole + " → " + role.getName());
        return new UserDTO(saved);
    }

    @Transactional
    public UserDTO updateStatus(Long id, boolean active) {
        User user = load(id);
        user.setActive(active);
        if (!active) {
            user.setTokenVersion(user.getTokenVersion() + 1); // khóa tài khoản → token đang dùng chết ngay
        }
        User saved = userRepository.save(user);
        auditLogService.log(active ? "UNLOCK_USER" : "LOCK_USER", "User", saved.getId().toString(), "active=" + active);
        return new UserDTO(saved);
    }

    private User load(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng."));
    }
}
