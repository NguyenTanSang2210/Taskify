package com.doanltmmt.Backend.importer;

import com.doanltmmt.Backend.entity.AcademicClass;
import com.doanltmmt.Backend.entity.Department;
import com.doanltmmt.Backend.entity.OtpPurpose;
import com.doanltmmt.Backend.entity.Student;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.repository.AuditLogRepository;
import com.doanltmmt.Backend.repository.OtpTokenRepository;
import com.doanltmmt.Backend.security.AbstractSecurityIT;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** P4-03 — nhập sinh viên / giảng viên từ Excel: file mẫu, xem trước báo lỗi từng dòng, nhập dòng hợp lệ, phân quyền. */
@SuppressWarnings("null")
class UserImportIT extends AbstractSecurityIT {

    private static final List<String> STUDENT_HEADER = List.of("MSSV", "Họ tên", "Email", "Mã lớp", "Số điện thoại");

    @Autowired private OtpTokenRepository otpRepo;
    @Autowired private AuditLogRepository auditRepo;

    private Department dept;
    private AcademicClass cls;
    private User admin;
    private User deptAdmin;

    @BeforeEach
    void setUp() {
        dept = newDepartment("IMP");
        cls = newClass(dept);
        admin = newUser("adm", "ADMIN", null);
        deptAdmin = newUser("dad", "DEPARTMENT_ADMIN", dept);
    }

    // ------------------------------------------------------------------ tiện ích

    private static byte[] xlsx(List<String> header, List<List<String>> rows) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Danh sách");
            List<List<String>> all = new ArrayList<>();
            all.add(header);
            all.addAll(rows);
            for (int r = 0; r < all.size(); r++) {
                Row row = sheet.createRow(r);
                for (int c = 0; c < all.get(r).size(); c++) row.createCell(c).setCellValue(all.get(r).get(c));
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    private ResultActions send(String path, User as, byte[] content, String fileName, Map<String, String> params) throws Exception {
        MockMultipartHttpServletRequestBuilder req = multipart(path)
                .file(new MockMultipartFile("file", fileName, MediaType.APPLICATION_OCTET_STREAM_VALUE, content));
        params.forEach(req::param);
        return mvc.perform(req.header(AUTH, bearer(as)));
    }

    private ResultActions preview(User as, byte[] content, Map<String, String> params) throws Exception {
        return send("/api/users/import/preview", as, content, "ds.xlsx", params);
    }

    private ResultActions commit(User as, byte[] content, Map<String, String> params) throws Exception {
        return send("/api/users/import", as, content, "ds.xlsx", params);
    }

    private Map<String, String> studentParams() {
        return Map.of("kind", "STUDENT", "departmentId", dept.getId().toString());
    }

    private JsonNode read(ResultActions r) throws Exception {
        return json.readTree(r.andReturn().getResponse().getContentAsString());
    }

    private static List<String> errorsOf(JsonNode report, int row) {
        for (JsonNode r : report.get("rows")) {
            if (r.get("row").asInt() == row) {
                List<String> out = new ArrayList<>();
                r.get("errors").forEach(e -> out.add(e.asText()));
                return out;
            }
        }
        throw new AssertionError("không có dòng " + row);
    }

    // ------------------------------------------------------------------ file mẫu

    @Test
    void template_isAReadableWorkbookWithTheExpectedHeader() throws Exception {
        byte[] body = mvc.perform(get("/api/users/import/template").param("kind", "STUDENT").header(AUTH, bearer(deptAdmin)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(body))) {
            Row header = wb.getSheetAt(0).getRow(0);
            for (int i = 0; i < STUDENT_HEADER.size(); i++) assertEquals(STUDENT_HEADER.get(i), header.getCell(i).getStringCellValue());
            assertEquals("Hướng dẫn", wb.getSheetName(1));
        }
        mvc.perform(get("/api/users/import/template").param("kind", "LECTURER").header(AUTH, bearer(admin)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/users/import/template").param("kind", "ADMIN").header(AUTH, bearer(admin)))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ xem trước

    @Test
    void preview_reportsEveryRowError_andWritesNothing() throws Exception {
        User existing = newUser("cu", "STUDENT", dept);
        String a = unique("sv"), b = unique("sv");
        byte[] file = xlsx(STUDENT_HEADER, List.of(
                List.of(a, "Nguyễn Văn A", a + "@t.local", cls.getCode(), ""),           // dòng 2: hợp lệ
                List.of(b, "Trần Thị B", "khong-phai-email", cls.getCode(), ""),         // dòng 3: email sai
                List.of(existing.getUsername(), "Đã có", unique("x") + "@t.local", cls.getCode(), ""), // dòng 4
                List.of(b, "Trùng MSSV", unique("y") + "@t.local", cls.getCode(), ""),   // dòng 5: trùng dòng 3 (cả hai bị đánh dấu)
                List.of(unique("sv"), "", existing.getEmail(), "KHONG-CO", ""),          // dòng 6: nhiều lỗi
                List.of("", "", "", "", "")));                                           // dòng trống: bỏ qua
        long usersBefore = userRepo.count();

        JsonNode report = read(preview(deptAdmin, file, Map.of("kind", "STUDENT")).andExpect(status().isOk())
                .andExpect(jsonPath("$.committed").value(false))
                .andExpect(jsonPath("$.departmentId").value(dept.getId()))
                .andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.valid").value(1))
                .andExpect(jsonPath("$.invalid").value(4)));

        assertTrue(errorsOf(report, 2).isEmpty());
        assertEquals(List.of("MSSV bị trùng trong file", "Email không hợp lệ"), errorsOf(report, 3));
        assertEquals(List.of("MSSV đã có tài khoản"), errorsOf(report, 4));
        assertTrue(errorsOf(report, 5).contains("MSSV bị trùng trong file"));
        List<String> many = errorsOf(report, 6);
        assertTrue(many.contains("Thiếu họ tên"), many.toString());
        assertTrue(many.contains("Email đã được dùng cho tài khoản khác"), many.toString());
        assertTrue(many.stream().anyMatch(e -> e.startsWith("Lớp KHONG-CO không có trong khoa")), many.toString());
        assertEquals(usersBefore, userRepo.count(), "xem trước không được ghi gì");
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void headersAreMatchedWithoutAccentsAndInAnyOrder() throws Exception {
        String a = unique("sv");
        byte[] file = xlsx(List.of("ho va ten", "email", "LOP", "Ma sinh vien"),
                List.of(List.of("Lê Văn C", a + "@t.local", cls.getCode().toLowerCase(), a)));
        preview(admin, file, studentParams()).andExpect(status().isOk()).andExpect(jsonPath("$.valid").value(1))
                .andExpect(jsonPath("$.rows[0].username").value(a));
    }

    @Test
    void unreadableOrIncompleteFiles_areRejectedWithAClearMessage() throws Exception {
        send("/api/users/import/preview", admin, "a,b".getBytes(), "ds.csv", studentParams())
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("IMPORT_FILE_INVALID"));
        send("/api/users/import/preview", admin, "khong phai excel".getBytes(), "ds.xlsx", studentParams())
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("IMPORT_FILE_INVALID"));
        preview(admin, xlsx(List.of("MSSV", "Họ tên"), List.of(List.of("x", "y"))), studentParams())
                .andExpect(status().isBadRequest()).andExpect(errorMessageContains("thiếu cột bắt buộc: Email, Mã lớp"));
        preview(admin, xlsx(STUDENT_HEADER, List.of()), studentParams())
                .andExpect(status().isBadRequest()).andExpect(errorMessageContains("không có dòng dữ liệu"));
        // ADMIN phải chọn khoa
        preview(admin, xlsx(STUDENT_HEADER, List.of(List.of("x", "y", "x@t.local", cls.getCode(), ""))), Map.of("kind", "STUDENT"))
                .andExpect(status().isBadRequest()).andExpect(errorMessageContains("chọn khoa"));
    }

    // ------------------------------------------------------------------ nhập

    @Test
    void commit_createsOnlyValidRows_asInactiveAccounts_andSendsActivationCodes() throws Exception {
        String a = unique("sv"), b = unique("sv");
        byte[] file = xlsx(STUDENT_HEADER, List.of(
                List.of(a, "Nguyễn Văn A", a + "@t.local", cls.getCode(), "0901234567"),
                List.of(b, "Trần Thị B", b + "@t.local", cls.getCode(), ""),
                List.of(unique("sv"), "Lỗi", "sai-email", cls.getCode(), "")));

        commit(deptAdmin, file, Map.of("kind", "STUDENT")).andExpect(status().isOk())
                .andExpect(jsonPath("$.committed").value(true))
                .andExpect(jsonPath("$.created").value(2))
                .andExpect(jsonPath("$.invalid").value(1));

        User ua = userRepo.findByUsername(a).orElseThrow();
        assertFalse(ua.isActivated(), "chưa kích hoạt — người dùng tự đặt mật khẩu bằng mã qua email");
        assertEquals("STUDENT", ua.getRole().getName());
        assertEquals(dept.getId(), ua.getDepartment().getId());
        assertEquals("0901234567", ua.getPhone());
        Student sa = studentRepo.findById(ua.getId()).orElseThrow();
        assertEquals(a, sa.getStudentCode());
        assertEquals(cls.getId(), sa.getAcademicClass().getId());
        assertTrue(otpRepo.findTopByUserAndPurposeOrderByIdDesc(ua, OtpPurpose.ACTIVATION).isPresent());
        verify(emailService).sendEmail(eq(a + "@t.local"), anyString(), anyString());
        verify(emailService).sendEmail(eq(b + "@t.local"), anyString(), anyString());
        verify(emailService, times(2)).sendEmail(anyString(), anyString(), anyString());
        assertTrue(auditRepo.findAll().stream().anyMatch(l -> "IMPORT_USERS".equals(l.getAction())
                && l.getDetails().contains("created=2") && l.getDetails().contains("skipped=1")));

        // tài khoản nhập vào kích hoạt được như tài khoản do Admin tạo
        String code = otpRepo.findTopByUserAndPurposeOrderByIdDesc(ua, OtpPurpose.ACTIVATION).orElseThrow().getCode();
        mvc.perform(post("/api/auth/activate").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("identifier", a, "code", code, "newPassword", "MatKhauMoi2026"))))
                .andExpect(status().isOk());

        // nhập lại cùng file: mọi dòng đã có tài khoản → không có gì để nhập
        commit(deptAdmin, file, Map.of("kind", "STUDENT")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("IMPORT_FILE_INVALID"));
    }

    @Test
    void missingClasses_areCreatedOnlyWhenAsked() throws Exception {
        String a = unique("sv");
        String newCode = unique("K").toUpperCase();
        byte[] file = xlsx(STUDENT_HEADER, List.of(List.of(a, "Phạm D", a + "@t.local", newCode, "")));

        preview(admin, file, studentParams()).andExpect(jsonPath("$.valid").value(0));
        preview(admin, file, Map.of("kind", "STUDENT", "departmentId", dept.getId().toString(), "createMissingClasses", "true"))
                .andExpect(jsonPath("$.valid").value(1)).andExpect(jsonPath("$.newClasses[0]").value(newCode));
        assertTrue(classRepo.findByDepartment_IdAndCode(dept.getId(), newCode).isEmpty());

        commit(admin, file, Map.of("kind", "STUDENT", "departmentId", dept.getId().toString(), "createMissingClasses", "true"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.created").value(1));
        AcademicClass created = classRepo.findByDepartment_IdAndCode(dept.getId(), newCode).orElseThrow();
        assertEquals(created.getId(), studentRepo.findById(userRepo.findByUsername(a).orElseThrow().getId()).orElseThrow()
                .getAcademicClass().getId());
    }

    @Test
    void lecturers_areImportedWithDegreeAndSpeciality() throws Exception {
        String g = unique("gv");
        byte[] file = xlsx(List.of("Mã giảng viên", "Họ tên", "Email", "Học vị", "Chuyên ngành"),
                List.of(List.of(g, "Trần Thị Bình", g + "@t.local", "Tiến sĩ", "Trí tuệ nhân tạo")));
        commit(admin, file, Map.of("kind", "LECTURER", "departmentId", dept.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.created").value(1));
        User u = userRepo.findByUsername(g).orElseThrow();
        assertEquals("LECTURER", u.getRole().getName());
        var l = lecturerRepo.findById(u.getId()).orElseThrow();
        assertEquals("Tiến sĩ", l.getDegree());
        assertEquals("Trí tuệ nhân tạo", l.getSpeciality());
        assertEquals(dept.getId(), l.getDepartment().getId());
    }

    // ------------------------------------------------------------------ phân quyền

    @Test
    void departmentAdmin_canOnlyImportIntoOwnDepartment_andOthersCannotImportAtAll() throws Exception {
        Department other = newDepartment("IMP2");
        String a = unique("sv");
        byte[] file = xlsx(STUDENT_HEADER, List.of(List.of(a, "Nguyễn Văn A", a + "@t.local", cls.getCode(), "")));

        preview(deptAdmin, file, Map.of("kind", "STUDENT", "departmentId", other.getId().toString()))
                .andExpect(status().isForbidden());
        for (String role : List.of("LECTURER", "STUDENT")) {
            User u = newUser("x", role, dept);
            preview(u, file, Map.of("kind", "STUDENT")).andExpect(status().isForbidden());
            commit(u, file, Map.of("kind", "STUDENT")).andExpect(status().isForbidden());
            mvc.perform(get("/api/users/import/template").header(AUTH, bearer(u))).andExpect(status().isForbidden());
        }
        assertTrue(userRepo.findByUsername(a).isEmpty());
    }
}
