package com.doanltmmt.Backend.controller;

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
import com.doanltmmt.Backend.service.SecurityScopeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:5173") // for React local development
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

    public UserController(UserRepository userRepository,
                          RoleRepository roleRepository,
                          StudentRepository studentRepository,
                          LecturerRepository lecturerRepository,
                          DepartmentRepository departmentRepository,
                          AcademicClassRepository classRepository,
                          PasswordEncoder passwordEncoder,
                          com.doanltmmt.Backend.service.AuditLogService auditLogService,
                          SecurityScopeService scope) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.studentRepository = studentRepository;
        this.lecturerRepository = lecturerRepository;
        this.departmentRepository = departmentRepository;
        this.classRepository = classRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
        this.scope = scope;
    }

    @PutMapping("/{id}/profile")
    @PreAuthorize("isAuthenticated()")
    public User updateProfile(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        String currentUsername = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        boolean isAdmin = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        
        if (!user.getUsername().equals(currentUsername) && !isAdmin) {
             throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to update other user's profile");
        }

        if (body.containsKey("fullName")) user.setFullName(body.get("fullName"));
        if (body.containsKey("email")) user.setEmail(body.get("email"));
        
        User saved = userRepository.save(user);
        auditLogService.log("UPDATE_PROFILE", "User", saved.getId().toString(), "Updated profile info");
        return saved;
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("isAuthenticated()")
    public void changePassword(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        String currentUsername = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        if (!user.getUsername().equals(currentUsername)) {
             throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to change other user's password");
        }
        
        String oldPass = body.get("oldPassword");
        String newPass = body.get("newPassword");
        
        if (!passwordEncoder.matches(oldPass, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Incorrect old password");
        }
        
        user.setPassword(passwordEncoder.encode(newPass));
        userRepository.save(user);
        auditLogService.log("CHANGE_PASSWORD", "User", user.getId().toString(), "Password changed");
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public User getUser(@PathVariable Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }



    @GetMapping("/test")
    @PreAuthorize("hasRole('ADMIN')")
    public String test() {
        return "Backend OK";
    }

    // create quick admin demo + role for DB testing
    @PostMapping("/init-admin")
    @PreAuthorize("hasRole('ADMIN')")
    public User createAdminDemo() {
        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> {
                    Role r = new Role("ADMIN");
                    r.setDescription("Quan tri he thong");
                    return roleRepository.save(r);
                });

        User u = new User();
        u.setUsername("admin");
        u.setPassword("123456"); // will be encoded later
        u.setFullName("Admin Demo");
        u.setEmail("admin@example.com");
        u.setRole(adminRole);
        u.setActive(true);

        return userRepository.save(u);
    }

    // create quick lecturer demo if missing
    @PostMapping("/init-lecturer")
    @PreAuthorize("hasRole('ADMIN')")
    public User createLecturerDemo() {
        Role lecturerRole = roleRepository.findByName("LECTURER")
                .orElseGet(() -> roleRepository.save(new Role("LECTURER")));

        return userRepository.findByUsername("lecturer")
                .orElseGet(() -> {
                    User u = new User();
                    u.setUsername("lecturer");
                    u.setPassword(passwordEncoder.encode("123456"));
                    u.setFullName("Giang vien Demo");
                    u.setEmail("lecturer@example.com");
                    u.setRole(lecturerRole);
                    u.setActive(true);
                    return userRepository.save(u);
                });
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> getAll() {
        return userRepository.findAll();
    }

    @PostMapping({ "", "/create" })
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public User create(@RequestBody Map<String, Object> body) {
        String username = String.valueOf(body.getOrDefault("username", "")).trim();
        String password = String.valueOf(body.getOrDefault("password", "")).trim();
        String fullName = String.valueOf(body.getOrDefault("fullName", "")).trim();
        String email = body.get("email") != null ? String.valueOf(body.get("email")).trim() : null;
        String phone = body.get("phone") != null ? String.valueOf(body.get("phone")).trim() : null;
        Long roleId = body.get("roleId") != null ? Long.valueOf(String.valueOf(body.get("roleId"))) : null;
        Long departmentId = body.get("departmentId") != null ? Long.valueOf(String.valueOf(body.get("departmentId"))) : null;
        boolean active = body.get("active") == null || Boolean.parseBoolean(String.valueOf(body.get("active")));

        if (username.isBlank() || password.isBlank() || fullName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username, password, fullName are required");
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
            studentCode = String.valueOf(body.getOrDefault("studentCode", "")).trim();
            className = body.get("className") != null ? String.valueOf(body.get("className")).trim() : null;
            academicClassId = body.get("academicClassId") != null ? Long.valueOf(String.valueOf(body.get("academicClassId"))) : null;

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
        u.setPassword(passwordEncoder.encode(password));
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
            String degree = body.get("degree") != null ? String.valueOf(body.get("degree")).trim() : null;
            String speciality = body.get("speciality") != null ? String.valueOf(body.get("speciality")).trim() : null;
            if (degree != null && !degree.isBlank()) l.setDegree(degree);
            if (speciality != null && !speciality.isBlank()) l.setSpeciality(speciality);
            lecturerRepository.save(l);
        }

        auditLogService.log("CREATE_USER", "User", saved.getId().toString(), "username=" + saved.getUsername());
        return saved;
    }

    @GetMapping("/by-department")
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_ADMIN')")
    public List<User> listByDepartment(@RequestParam(required = false) Long departmentId,
                                       @RequestParam(required = false) String role) {
        if (departmentId == null && !scope.hasRole("ADMIN")) {
            departmentId = scope.requireCurrentUser().getDepartment() != null ? scope.requireCurrentUser().getDepartment().getId() : null;
        }
        scope.requireDepartmentAccess(departmentId);
        if (role != null && !role.isBlank()) {
            return userRepository.findByDepartment_IdAndRole_NameOrderByIdDesc(departmentId, role.trim().toUpperCase());
        }
        return userRepository.findByDepartment_IdOrderByIdDesc(departmentId);
    }

    @GetMapping("/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public User updateRole(@PathVariable Long id, @RequestParam Long roleId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
        
        String oldRole = user.getRole() != null ? user.getRole().getName() : "None";
        user.setRole(role);
        User saved = userRepository.save(user);
        
        auditLogService.log("UPDATE_USER_ROLE", "User", saved.getId().toString(), "Changed role from " + oldRole + " to " + role.getName());
        return saved;
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public User updateStatus(@PathVariable Long id, @RequestParam Boolean active) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setActive(active);
        User saved = userRepository.save(user);
        auditLogService.log(active ? "UNLOCK_USER" : "LOCK_USER", "User", saved.getId().toString(), "Updated active status to " + active);
        return saved;
    }

    // create quick student demo if missing
    @PostMapping("/init-student")
    @PreAuthorize("hasRole('ADMIN')")
    public User createStudentDemo() {
        Role studentRole = roleRepository.findByName("STUDENT")
                .orElseGet(() -> roleRepository.save(new Role("STUDENT")));

        User user = userRepository.findByUsername("student")
                .orElseGet(() -> {
                    User u = new User();
                    u.setUsername("student");
                    u.setPassword(passwordEncoder.encode("123456"));
                    u.setFullName("Sinh vien Demo");
                    u.setEmail("student@example.com");
                    u.setRole(studentRole);
                    u.setActive(true);
                    return userRepository.save(u);
                });

        // ensure Student record exists with 1-1 mapping to User
        if (!studentRepository.existsById(user.getId())) {
            Student s = new Student();
            s.setUser(user);
            s.setStudentCode("SV001");
            s.setClassName("LTM1");
            studentRepository.save(s);
        }

        return user;
    }
}



