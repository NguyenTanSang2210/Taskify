package com.doanltmmt.Backend.security;

import com.doanltmmt.Backend.entity.enums.TopicStatus;
import com.doanltmmt.Backend.entity.enums.WorkspaceStatus;
import com.doanltmmt.Backend.entity.AcademicClass;
import com.doanltmmt.Backend.entity.Department;
import com.doanltmmt.Backend.entity.Lecturer;
import com.doanltmmt.Backend.entity.Student;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.TopicRegistration;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.entity.Workspace;
import com.doanltmmt.Backend.repository.AcademicClassRepository;
import com.doanltmmt.Backend.repository.DepartmentRepository;
import com.doanltmmt.Backend.repository.LecturerRepository;
import com.doanltmmt.Backend.repository.RoleRepository;
import com.doanltmmt.Backend.repository.StudentRepository;
import com.doanltmmt.Backend.repository.TopicRegistrationRepository;
import com.doanltmmt.Backend.repository.TopicRepository;
import com.doanltmmt.Backend.repository.UserRepository;
import com.doanltmmt.Backend.repository.WorkspaceRepository;
import com.doanltmmt.Backend.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import com.jayway.jsonpath.JsonPath;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Nền cho các test tích hợp bảo mật: nạp toàn bộ ứng dụng (kể cả bộ lọc JWT thật), gọi API bằng MockMvc.
 *
 * <p>Mỗi test chạy trong MỘT transaction và bị rollback ở cuối ⇒ dữ liệu mẫu tự tạo ở đây không để lại rác.
 * {@link EmailService} được mock ⇒ không bao giờ gửi email thật.
 *
 * <p>Cần MySQL (xem {@code scripts/run-tests.ps1} — dùng DB test riêng {@code doan_ltmmt_test}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@SuppressWarnings("null")
public abstract class AbstractSecurityIT {

    /** Thư mục upload tạm: test không ghi rác vào Backend/uploads. */
    private static final java.nio.file.Path UPLOAD_DIR;

    static {
        try {
            UPLOAD_DIR = java.nio.file.Files.createTempDirectory("taskify-it-uploads");
            UPLOAD_DIR.toFile().deleteOnExit();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void uploadProperties(DynamicPropertyRegistry registry) {
        registry.add("app.upload.dir", UPLOAD_DIR::toString);
    }

    /** Mật khẩu của mọi user tạo bằng {@link #newUser}. */
    protected static final String PASSWORD = "Passw0rd-Test";
    protected static final String AUTH = "Authorization";

    // cost thấp cho nhanh — BCrypt tự đọc cost trong chuỗi băm khi xác minh
    private static final String PASSWORD_HASH = new BCryptPasswordEncoder(4).encode(PASSWORD);

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @Autowired protected UserRepository userRepo;
    @Autowired protected RoleRepository roleRepo;
    @Autowired protected DepartmentRepository deptRepo;
    @Autowired protected AcademicClassRepository classRepo;
    @Autowired protected StudentRepository studentRepo;
    @Autowired protected LecturerRepository lecturerRepo;
    @Autowired protected WorkspaceRepository workspaceRepo;
    @Autowired protected TopicRepository topicRepo;
    @Autowired protected TopicRegistrationRepository regRepo;
    @Autowired protected PasswordEncoder encoder;
    @Autowired protected JwtTokenProvider tokens;
    @Autowired protected RateLimiter rateLimiter;

    @MockitoBean protected EmailService emailService;

    private final String run = Long.toString(System.nanoTime(), 36);
    private int seq = 0;

    /** Bộ giới hạn tần suất nằm trong RAM và dùng chung giữa các test: xóa trước mỗi test để không ảnh hưởng nhau. */
    @BeforeEach
    void resetRateLimiter() {
        rateLimiter.reset();
    }

    // ------------------------------------------------------------------ định danh duy nhất

    protected String unique(String prefix) {
        String value = prefix + "_" + run + "_" + (seq++);
        return value.length() > 45 ? value.substring(value.length() - 45) : value;
    }

    // ------------------------------------------------------------------ tạo dữ liệu mẫu

    protected Department newDepartment(String label) {
        Department d = new Department();
        d.setCode(unique(label));
        d.setName("Khoa " + label);
        d.setActive(true);
        return deptRepo.save(d);
    }

    protected AcademicClass newClass(Department dept) {
        AcademicClass c = new AcademicClass();
        c.setDepartment(dept);
        c.setCode(unique("CLS"));
        c.setName("Lớp thử nghiệm");
        c.setActive(true);
        return classRepo.save(c);
    }

    /** Tạo user với vai trò {@code roleName} (ADMIN, DEPARTMENT_ADMIN, LECTURER, STUDENT), mật khẩu {@link #PASSWORD}. */
    protected User newUser(String prefix, String roleName, Department dept) {
        User u = new User();
        u.setUsername(unique(prefix));
        u.setPassword(PASSWORD_HASH);
        u.setFullName("Người dùng " + prefix);
        u.setEmail(u.getUsername() + "@test.local");
        u.setRole(roleRepo.findByName(roleName).orElseThrow(() -> new IllegalStateException("Thiếu role " + roleName)));
        u.setDepartment(dept);
        u.setActive(true);
        return userRepo.save(u);
    }

    protected Student newStudent(String prefix, Department dept, AcademicClass cls) {
        User u = newUser(prefix, "STUDENT", dept);
        Student s = new Student();
        s.setUser(u);
        s.setStudentCode(unique("SV"));
        s.setClassName(cls != null ? cls.getCode() : "X");
        s.setAcademicClass(cls);
        return studentRepo.save(s);
    }

    protected Lecturer newLecturer(String prefix, Department dept) {
        User u = newUser(prefix, "LECTURER", dept);
        Lecturer l = new Lecturer();
        l.setUser(u);
        l.setDepartment(dept);
        l.setDegree("ThS");
        return lecturerRepo.save(l);
    }

    protected Workspace newWorkspace(Department dept, String status) {
        Workspace w = new Workspace();
        w.setDepartment(dept);
        w.setName(unique("WS"));
        w.setStatus(WorkspaceStatus.valueOf(status));
        w.setActive(true);
        return workspaceRepo.save(w);
    }

    protected Topic newTopic(Lecturer lecturer, Workspace ws, int capacity) {
        Topic t = new Topic();
        t.setTitle("Đề tài " + unique("T"));
        t.setLecturer(lecturer);
        t.setWorkspace(ws);
        t.setCapacity(capacity);
        t.setStatus(TopicStatus.OPEN);
        return topicRepo.save(t);
    }

    /** {@code approved}: true = đã duyệt, false = từ chối, null = chờ duyệt. */
    protected TopicRegistration newRegistration(Student s, Topic t, Boolean approved) {
        TopicRegistration r = new TopicRegistration();
        r.setStudent(s);
        r.setTopic(t);
        r.setApproved(approved);
        r.setRegisteredAt(LocalDateTime.now());
        return regRepo.save(r);
    }

    // ------------------------------------------------------------------ so khớp thông báo lỗi

    /**
     * Thông báo lỗi của response. Với ResponseStatusException, MockMvc KHÔNG forward sang /error nên thông báo nằm ở
     * getErrorMessage(); với lỗi do bộ xử lý bảo mật ghi JSON trực tiếp thì nằm ở body ($.message).
     * (Server thật luôn trả JSON có $.message.)
     */
    protected static String errorMessageOf(org.springframework.test.web.servlet.MvcResult result) throws Exception {
        String message = result.getResponse().getErrorMessage();
        if (message == null || message.isEmpty()) {
            try {
                message = JsonPath.read(result.getResponse().getContentAsString(), "$.message");
            } catch (Exception ignored) {
                message = "";
            }
        }
        return message == null ? "" : message;
    }

    protected static ResultMatcher errorMessageContains(String fragment) {
        return result -> {
            String message = errorMessageOf(result);
            if (!message.contains(fragment)) {
                throw new AssertionError("Thông báo lỗi \"" + message + "\" không chứa \"" + fragment + "\"");
            }
        };
    }

    protected static ResultMatcher errorMessageIs(String expected) {
        return result -> {
            String message = errorMessageOf(result);
            if (!message.equals(expected)) {
                throw new AssertionError("Thông báo lỗi \"" + message + "\" khác mong đợi \"" + expected + "\"");
            }
        };
    }

    // ------------------------------------------------------------------ token

    protected String tokenOf(User u) {
        return tokens.generateAccessToken(u.getUsername(), u.getId(), u.getRole().getName(), u.getTokenVersion());
    }

    /** Giá trị header Authorization cho user. */
    protected String bearer(User u) {
        return "Bearer " + tokenOf(u);
    }
}
