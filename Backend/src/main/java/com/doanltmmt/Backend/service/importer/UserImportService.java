package com.doanltmmt.Backend.service.importer;

import com.doanltmmt.Backend.entity.AcademicClass;
import com.doanltmmt.Backend.entity.Department;
import com.doanltmmt.Backend.entity.Lecturer;
import com.doanltmmt.Backend.entity.OtpPurpose;
import com.doanltmmt.Backend.entity.Role;
import com.doanltmmt.Backend.entity.Student;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.exception.ApiException;
import com.doanltmmt.Backend.exception.ErrorCodes;
import com.doanltmmt.Backend.repository.AcademicClassRepository;
import com.doanltmmt.Backend.repository.DepartmentRepository;
import com.doanltmmt.Backend.repository.LecturerRepository;
import com.doanltmmt.Backend.repository.RoleRepository;
import com.doanltmmt.Backend.repository.StudentRepository;
import com.doanltmmt.Backend.repository.UserRepository;
import com.doanltmmt.Backend.service.AuditLogService;
import com.doanltmmt.Backend.service.OtpService;
import com.doanltmmt.Backend.service.SecurityScopeService;
import com.doanltmmt.Backend.service.importer.ImportKind.ImportColumn;
import com.doanltmmt.Backend.service.importer.ImportWorkbook.RawRow;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * P4-03 — nhập hàng loạt sinh viên / giảng viên từ Excel.
 * <ol>
 *   <li>{@link #preview}: đọc file, kiểm tra từng dòng (bắt buộc, định dạng, trùng trong file, trùng tài khoản đã có,
 *       lớp thuộc khoa) — không ghi gì.</li>
 *   <li>{@link #commit}: kiểm tra lại y hệt rồi tạo các dòng hợp lệ trong MỘT transaction, bỏ qua dòng lỗi. Tài khoản
 *       tạo ra <b>chưa kích hoạt</b>; mã kích hoạt gửi qua email sau khi commit (hàng đợi mail, giãn nhịp).</li>
 * </ol>
 * ADMIN nhập cho khoa bất kỳ; DEPARTMENT_ADMIN chỉ cho khoa của mình.
 */
@Service
public class UserImportService {

    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9._-]+");
    private static final Pattern EMAIL = Pattern.compile("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");

    /** Kết quả một dòng; {@code errors} rỗng = hợp lệ. */
    public record RowResult(int row, String username, String fullName, String email, String classCode, List<String> errors) {
        public boolean valid() {
            return errors.isEmpty();
        }
    }

    /** Báo cáo xem trước ({@code committed=false}) hoặc kết quả nhập ({@code committed=true}, {@code created}). */
    public record ImportReport(String kind, Long departmentId, String departmentName, int total, int valid, int invalid,
                               List<String> newClasses, boolean committed, int created, List<RowResult> rows) {
    }

    private record Analysis(Department department, List<RawRow> raw, List<RowResult> results, Map<String, AcademicClass> classes,
                            List<String> newClasses) {
    }

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final StudentRepository studentRepo;
    private final LecturerRepository lecturerRepo;
    private final DepartmentRepository departmentRepo;
    private final AcademicClassRepository classRepo;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final AuditLogService auditLog;
    private final SecurityScopeService scope;

    public UserImportService(UserRepository userRepo, RoleRepository roleRepo, StudentRepository studentRepo,
                             LecturerRepository lecturerRepo, DepartmentRepository departmentRepo,
                             AcademicClassRepository classRepo, PasswordEncoder passwordEncoder, OtpService otpService,
                             AuditLogService auditLog, SecurityScopeService scope) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.studentRepo = studentRepo;
        this.lecturerRepo = lecturerRepo;
        this.departmentRepo = departmentRepo;
        this.classRepo = classRepo;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.auditLog = auditLog;
        this.scope = scope;
    }

    public byte[] template(ImportKind kind) {
        return ImportWorkbook.template(kind);
    }

    @Transactional(readOnly = true)
    public ImportReport preview(MultipartFile file, ImportKind kind, Long departmentId, boolean createMissingClasses) {
        Analysis a = analyze(file, kind, departmentId, createMissingClasses);
        return report(kind, a, false, 0);
    }

    @Transactional
    public ImportReport commit(MultipartFile file, ImportKind kind, Long departmentId, boolean createMissingClasses) {
        Analysis a = analyze(file, kind, departmentId, createMissingClasses);
        List<RowResult> valid = a.results().stream().filter(RowResult::valid).toList();
        if (valid.isEmpty()) {
            throw ApiException.badRequest(ErrorCodes.IMPORT_FILE_INVALID, "Không có dòng hợp lệ nào để nhập — hãy sửa các lỗi rồi thử lại.");
        }
        Department dept = a.department();
        Map<String, AcademicClass> classes = new HashMap<>(a.classes());
        if (kind == ImportKind.STUDENT) {
            for (String code : a.newClasses()) {
                AcademicClass c = new AcademicClass();
                c.setDepartment(dept);
                c.setCode(code);
                c.setName(code);
                classes.put(key(code), classRepo.save(c));
            }
        }
        Role role = roleRepo.findByName(kind.roleName())
                .orElseThrow(() -> new IllegalStateException("Thiếu vai trò " + kind.roleName()));
        // Tài khoản chưa kích hoạt không đăng nhập được: một hash ngẫu nhiên không ai biết là đủ (BCrypt từng dòng sẽ rất chậm)
        String unusablePassword = passwordEncoder.encode(UUID.randomUUID().toString());
        Map<Integer, RawRow> rawByRow = a.raw().stream().collect(Collectors.toMap(RawRow::rowNumber, r -> r));

        List<User> created = new ArrayList<>();
        for (RowResult row : valid) {
            RawRow raw = rawByRow.get(row.row());
            User u = new User();
            u.setUsername(row.username());
            u.setPassword(unusablePassword);
            u.setActivated(false);
            u.setFullName(row.fullName());
            u.setEmail(row.email());
            u.setPhone(raw.get(ImportColumn.PHONE));
            u.setDepartment(dept);
            u.setRole(role);
            User saved = userRepo.save(u);
            if (kind == ImportKind.STUDENT) {
                Student s = new Student();
                s.setUser(saved);
                s.setStudentCode(row.username());
                AcademicClass c = classes.get(key(row.classCode()));
                s.setAcademicClass(c);
                s.setClassName(c.getCode());
                studentRepo.save(s);
            } else {
                Lecturer l = new Lecturer();
                l.setUser(saved);
                l.setDepartment(dept);
                l.setDegree(raw.get(ImportColumn.DEGREE));
                l.setSpeciality(raw.get(ImportColumn.SPECIALITY));
                lecturerRepo.save(l);
            }
            created.add(saved);
        }
        // Email kích hoạt chỉ được gửi sau khi transaction commit (EmailService → MailDispatcher AFTER_COMMIT)
        created.forEach(u -> otpService.issue(u, OtpPurpose.ACTIVATION));
        auditLog.log("IMPORT_USERS", "Department", String.valueOf(dept.getId()),
                "kind=" + kind + " created=" + created.size() + " skipped=" + (a.results().size() - valid.size())
                        + (a.newClasses().isEmpty() ? "" : " newClasses=" + a.newClasses()));
        return report(kind, a, true, created.size());
    }

    private Analysis analyze(MultipartFile file, ImportKind kind, Long departmentId, boolean createMissingClasses) {
        Department dept = resolveDepartment(departmentId);
        List<RawRow> raw = ImportWorkbook.read(file, kind);

        Set<String> usernames = raw.stream().map(r -> r.get(ImportColumn.USERNAME)).filter(v -> v != null).collect(Collectors.toSet());
        Set<String> emails = raw.stream().map(r -> r.get(ImportColumn.EMAIL)).filter(v -> v != null).collect(Collectors.toSet());
        Set<String> takenUsernames = lower(usernames.isEmpty() ? List.of() : userRepo.findExistingUsernames(usernames));
        Set<String> takenEmails = lower(emails.isEmpty() ? List.of() : userRepo.findExistingEmails(emails));

        Map<String, AcademicClass> classes = new LinkedHashMap<>();
        if (kind == ImportKind.STUDENT) {
            classRepo.findByDepartment_IdOrderByIdDesc(dept.getId()).forEach(c -> classes.put(key(c.getCode()), c));
        }
        Map<String, Long> usernameCount = countBy(raw, ImportColumn.USERNAME);
        Map<String, Long> emailCount = countBy(raw, ImportColumn.EMAIL);
        Map<String, String> newClasses = new LinkedHashMap<>();

        List<RowResult> results = new ArrayList<>();
        for (RawRow r : raw) {
            List<String> errors = new ArrayList<>();
            String username = r.get(ImportColumn.USERNAME);
            String fullName = r.get(ImportColumn.FULL_NAME);
            String email = r.get(ImportColumn.EMAIL);
            String classCode = r.get(ImportColumn.CLASS_CODE);
            String usernameHeader = kind.columns().get(0).header();

            if (username == null) errors.add("Thiếu " + usernameHeader);
            else if (username.length() > 50) errors.add(usernameHeader + " tối đa 50 ký tự");
            else if (!USERNAME.matcher(username).matches()) errors.add(usernameHeader + " chỉ gồm chữ không dấu, số và . _ -");
            else if (takenUsernames.contains(key(username))) errors.add(usernameHeader + " đã có tài khoản");
            else if (usernameCount.getOrDefault(key(username), 0L) > 1) errors.add(usernameHeader + " bị trùng trong file");

            if (fullName == null) errors.add("Thiếu họ tên");
            else if (fullName.length() > 100) errors.add("Họ tên tối đa 100 ký tự");

            if (email == null) errors.add("Thiếu email (cần để gửi mã kích hoạt)");
            else if (email.length() > 100 || !EMAIL.matcher(email).matches()) errors.add("Email không hợp lệ");
            else if (takenEmails.contains(key(email))) errors.add("Email đã được dùng cho tài khoản khác");
            else if (emailCount.getOrDefault(key(email), 0L) > 1) errors.add("Email bị trùng trong file");

            String phone = r.get(ImportColumn.PHONE);
            if (phone != null && phone.length() > 20) errors.add("Số điện thoại tối đa 20 ký tự");
            if (kind == ImportKind.LECTURER) {
                String degree = r.get(ImportColumn.DEGREE);
                String speciality = r.get(ImportColumn.SPECIALITY);
                if (degree != null && degree.length() > 100) errors.add("Học vị tối đa 100 ký tự");
                if (speciality != null && speciality.length() > 255) errors.add("Chuyên ngành tối đa 255 ký tự");
            } else if (classCode == null) {
                errors.add("Thiếu mã lớp");
            } else {
                AcademicClass c = classes.get(key(classCode));
                if (c != null && !c.isActive()) errors.add("Lớp " + c.getCode() + " đã ngừng hoạt động");
                else if (c == null && classCode.length() > 50) errors.add("Mã lớp tối đa 50 ký tự");
                else if (c == null && !createMissingClasses) errors.add("Lớp " + classCode + " không có trong khoa " + dept.getName());
                else if (c == null && errors.isEmpty()) newClasses.putIfAbsent(key(classCode), classCode);
            }
            results.add(new RowResult(r.rowNumber(), username, fullName, email, classCode, List.copyOf(errors)));
        }
        return new Analysis(dept, raw, results, classes, List.copyOf(newClasses.values()));
    }

    private Department resolveDepartment(Long departmentId) {
        if (scope.hasRole("ADMIN")) {
            if (departmentId == null) throw ApiException.badRequest(ErrorCodes.INVALID_INPUT, "Vui lòng chọn khoa cần nhập tài khoản.");
            return departmentRepo.findById(departmentId).orElseThrow(() -> ApiException.notFound("Không tìm thấy khoa."));
        }
        Department own = scope.requireCurrentUser().getDepartment();
        if (own == null) throw ApiException.forbidden("Tài khoản của bạn chưa thuộc khoa nào.");
        if (departmentId != null && !departmentId.equals(own.getId())) {
            throw ApiException.forbidden("Bạn chỉ được nhập tài khoản cho khoa của mình.");
        }
        return own;
    }

    private ImportReport report(ImportKind kind, Analysis a, boolean committed, int created) {
        int valid = (int) a.results().stream().filter(RowResult::valid).count();
        return new ImportReport(kind.name(), a.department().getId(), a.department().getName(), a.results().size(), valid,
                a.results().size() - valid, a.newClasses(), committed, created, a.results());
    }

    private static Map<String, Long> countBy(List<RawRow> rows, String column) {
        return rows.stream().map(r -> r.get(column)).filter(v -> v != null)
                .collect(Collectors.groupingBy(UserImportService::key, Collectors.counting()));
    }

    private static Set<String> lower(List<String> values) {
        Set<String> out = new HashSet<>();
        values.forEach(v -> out.add(key(v)));
        return out;
    }

    /** So khớp không phân biệt hoa thường — giống collation của cột username/email và mã lớp trong MySQL. */
    private static String key(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
