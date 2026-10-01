package com.doanltmmt.Backend.security;

import com.doanltmmt.Backend.entity.Department;
import com.doanltmmt.Backend.entity.OtpPurpose;
import com.doanltmmt.Backend.entity.OtpToken;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.repository.OtpTokenRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P1-09: bỏ đăng ký công khai, kích hoạt tài khoản bằng mã, mật khẩu tạm bắt buộc đổi (token PWD_CHANGE),
 * chính sách mật khẩu.
 */
@SuppressWarnings("null")
class AccountActivationIT extends AbstractSecurityIT {

    @Autowired private OtpTokenRepository otpRepo;

    private Department dept;
    private User admin;
    private int seq = 0;

    @BeforeEach
    void setUp() {
        dept = newDepartment("ACT");
        admin = newUser("adm", "ADMIN", null);
    }

    // ------------------------------------------------------------------ tiện ích

    private Map<String, Object> studentPayload(String username, String password, String email) {
        Map<String, Object> m = new HashMap<>();
        m.put("username", username);
        if (password != null) m.put("password", password);
        if (email != null) m.put("email", email);
        m.put("fullName", "Sinh viên " + username);
        m.put("roleId", roleRepo.findByName("STUDENT").orElseThrow().getId());
        m.put("departmentId", dept.getId());
        m.put("studentCode", "SV" + (++seq) + System.nanoTime() % 100000);
        m.put("className", "L1");
        return m;
    }

    private ResultActions createUser(Map<String, Object> payload) throws Exception {
        return mvc.perform(post("/api/users/create").header(AUTH, bearer(admin))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload)));
    }

    private ResultActions login(String username, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("username", username, "password", password))));
    }

    private ResultActions postJson(String url, Object body) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private JsonNode read(ResultActions r) throws Exception {
        return json.readTree(r.andReturn().getResponse().getContentAsString());
    }

    private String activationCodeOf(String username) {
        User u = userRepo.findByUsername(username).orElseThrow();
        return otpRepo.findTopByUserAndPurposeOrderByIdDesc(u, OtpPurpose.ACTIVATION).orElseThrow().getCode();
    }

    private String wrong(String code) {
        return code.equals("000000") ? "111111" : "000000";
    }

    // ------------------------------------------------------------------ đăng ký công khai đã bị gỡ

    @Test
    void publicRegistration_isGone() throws Exception {
        long before = userRepo.count();
        Map<String, String> body = Map.of("username", "tuDangKy", "password", "1", "fullName", "X", "studentCode", "X1", "className", "X");
        // chưa đăng nhập: không còn nằm trong danh sách công khai → 401
        postJson("/api/auth/register", body).andExpect(status().isUnauthorized());
        // đã đăng nhập: endpoint không tồn tại
        mvc.perform(post("/api/auth/register").header(AUTH, bearer(admin)).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body))).andExpect(status().isNotFound());
        assertEquals(before, userRepo.count());
    }

    // ------------------------------------------------------------------ chính sách mật khẩu khi tạo user

    @Test
    void create_withWeakPassword_isRejectedWithClearMessage() throws Exception {
        String name = unique("weak");
        createUser(studentPayload(name, "123", name + "@t.local")).andExpect(status().isBadRequest())
                .andExpect(errorMessageContains("ít nhất 8 ký tự"));
        createUser(studentPayload(name, "chicoChu", name + "@t.local")).andExpect(status().isBadRequest())
                .andExpect(errorMessageContains("chữ số"));
        createUser(studentPayload(name, "12345678", name + "@t.local")).andExpect(status().isBadRequest())
                .andExpect(errorMessageContains("chữ cái"));
        createUser(studentPayload(name, "xx" + name + "9", name + "@t.local")).andExpect(status().isBadRequest())
                .andExpect(errorMessageContains("tên đăng nhập"));
        assertTrue(userRepo.findByUsername(name).isEmpty());
    }

    @Test
    void create_withoutPasswordAndEmail_isRejected() throws Exception {
        createUser(studentPayload(unique("noemail"), null, null)).andExpect(status().isBadRequest())
                .andExpect(errorMessageContains("email"));
    }

    // ------------------------------------------------------------------ mật khẩu tạm → bắt buộc đổi

    @Test
    void temporaryPassword_forcesChangeThroughScopedToken() throws Exception {
        String name = unique("tmp");
        createUser(studentPayload(name, "Tam" + "Pass2026", name + "@t.local")).andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(true))
                .andExpect(jsonPath("$.activated").value(true));

        JsonNode step = read(login(name, "TamPass2026").andExpect(status().isOk()));
        assertEquals("CHANGE_PASSWORD", step.get("nextStep").asText());
        assertTrue(step.get("token").isNull(), "không được cấp access token khi chưa đổi mật khẩu tạm");
        String pre = step.get("preAuthToken").asText();

        // token PWD_CHANGE không dùng được cho API nghiệp vụ
        mvc.perform(get("/api/users/roles").header(AUTH, "Bearer " + pre)).andExpect(status().isUnauthorized());
        // token OTP_PENDING cũng không dùng được cho endpoint đổi mật khẩu
        User u = userRepo.findByUsername(name).orElseThrow();
        String otpScoped = tokens.generatePreAuthToken(name, u.getId(), u.getTokenVersion(), JwtTokenProvider.Scope.OTP_PENDING);
        postJson("/api/auth/password/initial", Map.of("preAuthToken", otpScoped, "newPassword", "MatKhauMoi2026")).andExpect(status().isUnauthorized());

        // mật khẩu mới yếu / trùng cũ → 400
        postJson("/api/auth/password/initial", Map.of("preAuthToken", pre, "newPassword", "yeu")).andExpect(status().isBadRequest());
        postJson("/api/auth/password/initial", Map.of("preAuthToken", pre, "newPassword", "TamPass2026")).andExpect(status().isBadRequest())
                .andExpect(errorMessageContains("trùng"));

        // đổi thành công → sinh viên nhận access token ngay
        JsonNode done = read(postJson("/api/auth/password/initial", Map.of("preAuthToken", pre, "newPassword", "MatKhauMoi2026")).andExpect(status().isOk()));
        assertEquals("NONE", done.get("nextStep").asText());
        assertFalse(done.get("token").asText().isBlank());

        // token PWD_CHANGE đã dùng thì hết hiệu lực; mật khẩu tạm không còn dùng được; mật khẩu mới đăng nhập bình thường
        postJson("/api/auth/password/initial", Map.of("preAuthToken", pre, "newPassword", "KhacNua2026")).andExpect(status().isUnauthorized());
        login(name, "TamPass2026").andExpect(status().isUnauthorized());
        assertEquals("NONE", read(login(name, "MatKhauMoi2026").andExpect(status().isOk())).get("nextStep").asText());
    }

    // ------------------------------------------------------------------ kích hoạt bằng mã

    @Test
    void createWithoutPassword_sendsActivationCode_andCannotLoginUntilActivated() throws Exception {
        String name = unique("act");
        String email = name + "@t.local";
        createUser(studentPayload(name, null, email)).andExpect(status().isOk())
                .andExpect(jsonPath("$.activated").value(false));
        verify(emailService, atLeastOnce()).sendEmail(eq(email), anyString(), anyString());

        login(name, "BatKyMatKhau1").andExpect(status().isUnauthorized());

        String code = activationCodeOf(name);
        // mật khẩu yếu: 400 và KHÔNG làm mất mã
        postJson("/api/auth/activate", Map.of("identifier", name, "code", code, "newPassword", "yeu")).andExpect(status().isBadRequest());
        // sai mã
        postJson("/api/auth/activate", Map.of("identifier", name, "code", wrong(code), "newPassword", "MatKhauMoi2026")).andExpect(status().isUnauthorized());
        // đúng mã (dùng email làm định danh)
        postJson("/api/auth/activate", Map.of("identifier", email, "code", code, "newPassword", "MatKhauMoi2026")).andExpect(status().isOk())
                .andExpect(jsonPath("$.activated").value(true));

        assertEquals("NONE", read(login(name, "MatKhauMoi2026").andExpect(status().isOk())).get("nextStep").asText());
        // mã chỉ dùng một lần
        postJson("/api/auth/activate", Map.of("identifier", name, "code", code, "newPassword", "KhacNua2026")).andExpect(status().isUnauthorized());
    }

    @Test
    void activation_wrongCodeFiveTimes_invalidatesTheCode() throws Exception {
        String name = unique("brute");
        createUser(studentPayload(name, null, name + "@t.local")).andExpect(status().isOk());
        String code = activationCodeOf(name);
        for (int i = 0; i < OtpService_MAX(); i++) {
            postJson("/api/auth/activate", Map.of("identifier", name, "code", wrong(code), "newPassword", "MatKhauMoi2026")).andExpect(status().isUnauthorized());
        }
        // mã đúng cũng không còn tác dụng
        postJson("/api/auth/activate", Map.of("identifier", name, "code", code, "newPassword", "MatKhauMoi2026")).andExpect(status().isUnauthorized());
        login(name, "MatKhauMoi2026").andExpect(status().isUnauthorized());
    }

    private static int OtpService_MAX() {
        return com.doanltmmt.Backend.service.OtpService.MAX_ATTEMPTS;
    }

    @Test
    void activation_errorsAreIdenticalForUnknownAndKnownAccounts() throws Exception {
        String name = unique("same");
        createUser(studentPayload(name, null, name + "@t.local")).andExpect(status().isOk());
        String body1 = postJson("/api/auth/activate", Map.of("identifier", "khong-ton-tai-" + name, "code", "123456", "newPassword", "MatKhauMoi2026"))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        String code = activationCodeOf(name);
        String body2 = postJson("/api/auth/activate", Map.of("identifier", name, "code", wrong(code), "newPassword", "MatKhauMoi2026"))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        assertEquals(JsonMessage.of(json, body1), JsonMessage.of(json, body2));
    }

    @Test
    void activation_alreadyActivatedAccount_cannotBeReactivated() throws Exception {
        User active = newUser("stu", "STUDENT", dept);
        OtpToken forged = new OtpToken();
        forged.setUser(active);
        forged.setPurpose(OtpPurpose.ACTIVATION);
        forged.setCode("123456");
        forged.setExpiresAt(java.time.LocalDateTime.now().plusHours(1));
        otpRepo.saveAndFlush(forged);
        postJson("/api/auth/activate", Map.of("identifier", active.getUsername(), "code", "123456", "newPassword", "KeChiemQuyen2026"))
                .andExpect(status().isUnauthorized());
        login(active.getUsername(), PASSWORD).andExpect(status().isOk());
    }

    @Test
    void resendActivation_adminOnly_withCooldown() throws Exception {
        String name = unique("resend");
        JsonNode created = read(createUser(studentPayload(name, null, name + "@t.local")).andExpect(status().isOk()));
        long id = created.get("id").asLong();

        // vừa tạo (mã vừa gửi) → chờ cooldown
        mvc.perform(post("/api/users/{id}/resend-activation", id).header(AUTH, bearer(admin))).andExpect(status().isTooManyRequests());
        // người không phải ADMIN: 403
        User stu = newUser("stu", "STUDENT", dept);
        mvc.perform(post("/api/users/{id}/resend-activation", id).header(AUTH, bearer(stu))).andExpect(status().isForbidden());
        // tài khoản đã kích hoạt: 400
        mvc.perform(post("/api/users/{id}/resend-activation", stu.getId()).header(AUTH, bearer(admin))).andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ đổi mật khẩu thường

    @Test
    void changePassword_appliesPolicyAndRejectsReuse() throws Exception {
        User stu = newUser("stu", "STUDENT", dept);
        String url = "/api/users/" + stu.getId() + "/password";
        java.util.function.BiFunction<String, String, ResultActions> change = (oldP, newP) -> {
            try {
                return mvc.perform(put(url).header(AUTH, bearer(stu)).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("oldPassword", oldP, "newPassword", newP))));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        };
        change.apply("SaiMatKhauCu1", "MatKhauMoi2026").andExpect(status().isBadRequest());
        change.apply(PASSWORD, "123").andExpect(status().isBadRequest());
        change.apply(PASSWORD, PASSWORD).andExpect(status().isBadRequest()).andExpect(errorMessageContains("trùng"));
        change.apply(PASSWORD, "MatKhauMoi2026").andExpect(status().isOk()).andExpect(jsonPath("$.token").exists());
    }

    /** Lấy trường message của phản hồi lỗi JSON để so sánh nội dung. */
    private static final class JsonMessage {
        static String of(com.fasterxml.jackson.databind.ObjectMapper mapper, String body) throws Exception {
            JsonNode n = mapper.readTree(body);
            return n.has("message") ? n.get("message").asText() : body;
        }
    }
}
