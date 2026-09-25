package com.doanltmmt.Backend;

import com.doanltmmt.Backend.entity.AcademicClass;
import com.doanltmmt.Backend.entity.Department;
import com.doanltmmt.Backend.entity.Lecturer;
import com.doanltmmt.Backend.entity.Role;
import com.doanltmmt.Backend.entity.Student;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.entity.Workspace;
import com.doanltmmt.Backend.repository.AcademicClassRepository;
import com.doanltmmt.Backend.repository.DepartmentRepository;
import com.doanltmmt.Backend.repository.LecturerRepository;
import com.doanltmmt.Backend.repository.RoleRepository;
import com.doanltmmt.Backend.repository.StudentRepository;
import com.doanltmmt.Backend.repository.UserRepository;
import com.doanltmmt.Backend.repository.WorkspaceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seed dữ liệu demo cho profile dev (chỉ chạy khi app.seed.enabled=true).
 * Idempotent: chạy lại nhiều lần không nhân bản dữ liệu.
 *
 * Tài khoản demo (mật khẩu 123456): admin, deptadmin, lecturer, student, student2.
 * Email nhận OTP của admin/deptadmin/lecturer lấy từ app.seed.otp-email (biến APP_SEED_OTP_EMAIL),
 * dùng plus-addressing của Gmail: ten+admin@gmail.com, ten+deptadmin@gmail.com, ten+lecturer@gmail.com.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
@SuppressWarnings("null")
public class DataSeeder {

    private static final String DEMO_PASSWORD = "123456";
    private static final String DEPT_CODE = "CNTT";
    private static final String CLASS_CODE = "CNTT-K15";
    private static final String DEMO_WORKSPACE_NAME = "Đồ án CNPM HK1 2026-2027";

    private final RoleRepository roleRepo;
    private final UserRepository userRepo;
    private final StudentRepository studentRepo;
    private final LecturerRepository lecturerRepo;
    private final DepartmentRepository departmentRepo;
    private final AcademicClassRepository classRepo;
    private final WorkspaceRepository workspaceRepo;
    private final PasswordEncoder encoder;
    private final String otpEmail;

    public DataSeeder(RoleRepository roleRepo, UserRepository userRepo, StudentRepository studentRepo,
                      LecturerRepository lecturerRepo, DepartmentRepository departmentRepo,
                      AcademicClassRepository classRepo, WorkspaceRepository workspaceRepo,
                      PasswordEncoder encoder,
                      @Value("${app.seed.otp-email:}") String otpEmail) {
        this.roleRepo = roleRepo;
        this.userRepo = userRepo;
        this.studentRepo = studentRepo;
        this.lecturerRepo = lecturerRepo;
        this.departmentRepo = departmentRepo;
        this.classRepo = classRepo;
        this.workspaceRepo = workspaceRepo;
        this.encoder = encoder;
        this.otpEmail = otpEmail == null ? "" : otpEmail.trim();
    }

    @Transactional
    public void seed() {
        // 1. Roles
        Role adminRole = ensureRole("ADMIN");
        Role deptAdminRole = ensureRole("DEPARTMENT_ADMIN");
        Role lecturerRole = ensureRole("LECTURER");
        Role studentRole = ensureRole("STUDENT");

        // 2. Khoa + lớp học thuật
        Department cntt = departmentRepo.findByCode(DEPT_CODE).orElseGet(() -> {
            Department d = new Department();
            d.setCode(DEPT_CODE);
            d.setName("Công nghệ thông tin");
            d.setActive(true);
            return departmentRepo.save(d);
        });

        AcademicClass k15 = classRepo.findByDepartment_IdAndCode(cntt.getId(), CLASS_CODE).orElseGet(() -> {
            AcademicClass c = new AcademicClass();
            c.setDepartment(cntt);
            c.setCode(CLASS_CODE);
            c.setName("Công nghệ thông tin K15");
            c.setActive(true);
            return classRepo.save(c);
        });

        // 3. Admin (chuẩn hoá mật khẩu cũ chưa mã hoá nếu có)
        userRepo.findByUsername("admin").ifPresentOrElse(admin -> {
            if (admin.getPassword() != null && !admin.getPassword().startsWith("$2")) {
                admin.setPassword(encoder.encode(admin.getPassword()));
                userRepo.save(admin);
            }
        }, () -> createUser("admin", "Quản trị hệ thống", otpEmailFor("admin"), adminRole, null));

        // 4. Quản trị khoa
        if (userRepo.findByUsername("deptadmin").isEmpty()) {
            createUser("deptadmin", "Quản trị khoa CNTT", otpEmailFor("deptadmin"), deptAdminRole, cntt);
        }

        // 5. Giảng viên
        User lecturerUser = userRepo.findByUsername("lecturer")
                .orElseGet(() -> createUser("lecturer", "ThS. Giảng viên Demo", otpEmailFor("lecturer"), lecturerRole, cntt));
        if (!lecturerRepo.existsById(lecturerUser.getId())) {
            Lecturer l = new Lecturer();
            l.setUser(userRepo.getReferenceById(lecturerUser.getId()));
            l.setDepartment(cntt);
            l.setDegree("ThS");
            l.setSpeciality("Công nghệ phần mềm");
            lecturerRepo.save(l);
        }

        // 6. Sinh viên (gán khoa + lớp học thuật để đăng ký được đề tài)
        ensureStudent("student", "Sinh viên Demo", "student@example.com", "SV001", studentRole, cntt, k15);
        ensureStudent("student2", "Sinh viên Demo 2", "student2@example.com", "SV002", studentRole, cntt, k15);

        // 7. Workspace mẫu ở trạng thái DRAFT
        boolean hasDemoWorkspace = workspaceRepo.findByDepartment_IdOrderByIdDesc(cntt.getId()).stream()
                .anyMatch(w -> DEMO_WORKSPACE_NAME.equals(w.getName()));
        if (!hasDemoWorkspace) {
            Workspace w = new Workspace();
            w.setDepartment(cntt);
            w.setName(DEMO_WORKSPACE_NAME);
            w.setType("DO_AN");
            w.setSemester("HK1 2026-2027");
            w.setStatus("DRAFT");
            w.setActive(true);
            workspaceRepo.save(w);
        }
    }

    private Role ensureRole(String name) {
        return roleRepo.findByName(name).orElseGet(() -> roleRepo.save(new Role(name)));
    }

    private User createUser(String username, String fullName, String email, Role role, Department dept) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(encoder.encode(DEMO_PASSWORD));
        u.setFullName(fullName);
        u.setEmail(email);
        u.setRole(role);
        u.setDepartment(dept);
        u.setActive(true);
        return userRepo.save(u);
    }

    private void ensureStudent(String username, String fullName, String email, String code,
                               Role studentRole, Department dept, AcademicClass academicClass) {
        User user = userRepo.findByUsername(username)
                .orElseGet(() -> createUser(username, fullName, email, studentRole, dept));
        if (user.getDepartment() == null) {
            user.setDepartment(dept);
            userRepo.save(user);
        }

        Student s = studentRepo.findById(user.getId()).orElseGet(() -> {
            Student created = new Student();
            created.setUser(userRepo.getReferenceById(user.getId()));
            created.setStudentCode(code);
            return created;
        });
        if (s.getAcademicClass() == null) {
            s.setAcademicClass(academicClass);
            s.setClassName(academicClass.getCode());
        }
        studentRepo.save(s);
    }

    /** ten@gmail.com + "admin" → ten+admin@gmail.com; không cấu hình → admin@example.com */
    private String otpEmailFor(String username) {
        int at = otpEmail.indexOf('@');
        if (at <= 0) {
            return username + "@example.com";
        }
        return otpEmail.substring(0, at) + "+" + username + otpEmail.substring(at);
    }
}
