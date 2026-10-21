package com.doanltmmt.Backend.security;

import com.doanltmmt.Backend.entity.AuditLog;
import com.doanltmmt.Backend.entity.Department;
import com.doanltmmt.Backend.entity.OtpPurpose;
import com.doanltmmt.Backend.entity.OtpToken;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.repository.AuditLogRepository;
import com.doanltmmt.Backend.repository.OtpTokenRepository;
import com.doanltmmt.Backend.security.JwtTokenProvider.Scope;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P1-01 — luồng đăng nhập nhiều bước: KHÔNG cấp token đầy đủ quyền trước khi OTP đúng.
 * Bao phủ test bắt buộc #1 (token OTP_PENDING không gọi được API) và #2 (OTP sai nhiều lần bị vô hiệu).
 */
@SuppressWarnings("null")
class AuthFlowIT extends AbstractSecurityIT {

    @Autowired private OtpTokenRepository otpRepo;
    @Autowired private AuditLogRepository auditRepo;

    private Department dept;
    private User admin;
    private User student;

    @BeforeEach
    void setUp() {
        dept = newDepartment("OTP");
        admin = newUser("adm", "ADMIN", null);       // vai trò cần OTP
        student = newUser("stu", "STUDENT", dept);   // không cần OTP
    }

    // ------------------------------------------------------------------ tiện ích

    private ResultActions doLogin(String username, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("username", username, "password", password))));
    }

    private JsonNode loginOk(User user) throws Exception {
        MvcResult r = doLogin(user.getUsername(), PASSWORD).andExpect(status().isOk()).andReturn();
        return json.readTree(r.getResponse().getContentAsString());
    }

    private ResultActions doVerify(String preAuthToken, String code) throws Exception {
        return mvc.perform(post("/api/auth/otp/verify").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("preAuthToken", preAuthToken, "code", code))));
    }

    private ResultActions doResend(String preAuthToken) throws Exception {
        return mvc.perform(post("/api/auth/otp/resend").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("preAuthToken", preAuthToken))));
    }

    private OtpToken latestOtp(User user) {
        return otpRepo.findTopByUserAndPurposeOrderByIdDesc(user, OtpPurpose.LOGIN).orElseThrow();
    }

    private String otpCodeOf(User user) {
        return latestOtp(user).getCode();
    }

    private String wrongCodeFor(String realCode) {
        return realCode.equals("000000") ? "111111" : "000000";
    }

    // ------------------------------------------------------------------ bước 1: đăng nhập

    @Test
    void overlongUsername_isRejectedWith400_notServerError() throws Exception {
        // Trước đây: ghi audit log thất bại (cột 255 ký tự) -> HTTP 500 kèm câu lệnh SQL trong phản hồi
        doLogin("x".repeat(10_000), "sai")
                .andExpect(status().isBadRequest());
    }

    @Test
    void student_getsAccessTokenDirectly_noOtpStep() throws Exception {
        JsonNode body = loginOk(student);

        assertEquals("NONE", body.get("nextStep").asText());
        assertFalse(body.get("otpRequired").asBoolean());
        assertEquals("STUDENT", body.get("role").asText());
        assertEquals(student.getId(), body.get("userId").asLong());
        assertTrue(body.get("preAuthToken").isNull());

        mvc.perform(get("/api/users/{id}", student.getId()).header(AUTH, "Bearer " + body.get("token").asText()))
                .andExpect(status().isOk());
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void admin_getsOnlyAPreAuthToken_neverAnAccessTokenBeforeOtp() throws Exception {
        JsonNode body = loginOk(admin);

        assertEquals("OTP", body.get("nextStep").asText());
        assertTrue(body.get("otpRequired").asBoolean());
        assertTrue(body.get("token").isNull(), "không được cấp access token trước khi OTP đúng");
        assertFalse(body.get("preAuthToken").asText().isBlank());
        // chưa xác thực OTP thì không lộ thông tin nhạy cảm
        assertTrue(body.get("userId").isNull());
        assertTrue(body.get("fullName").isNull());
        assertTrue(body.get("role").isNull());
        assertTrue(body.get("maskedEmail").asText().contains("***@"), body.get("maskedEmail").asText());
        assertEquals(60, body.get("resendCooldownSeconds").asInt());
    }

    @Test
    void admin_login_sendsOtpEmail_containingTheStoredCode() throws Exception {
        loginOk(admin);

        String code = otpCodeOf(admin);
        assertEquals(6, code.length());
        verify(emailService, atLeastOnce()).sendEmail(eq(admin.getEmail()), eq("Taskify - Mã xác thực đăng nhập"),
                argThat(text -> text != null && text.contains(code)));
    }

    // ------------------------------------------------------------------ test bắt buộc #1

    @Test
    void preAuthToken_cannotCallAnyApi() throws Exception {
        String preAuth = loginOk(admin).get("preAuthToken").asText();

        mvc.perform(get("/api/users").header(AUTH, "Bearer " + preAuth)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users/{id}", admin.getId()).header(AUTH, "Bearer " + preAuth)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/notifications/count").param("userId", String.valueOf(admin.getId()))
                .header(AUTH, "Bearer " + preAuth)).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ bước 2: xác minh OTP

    @Test
    void correctOtp_issuesAccessToken_thatReallyWorks() throws Exception {
        String preAuth = loginOk(admin).get("preAuthToken").asText();

        MvcResult r = doVerify(preAuth, otpCodeOf(admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.nextStep").value("NONE"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.userId").value(admin.getId()))
                .andReturn();
        String access = json.readTree(r.getResponse().getContentAsString()).get("token").asText();

        assertNotEquals(preAuth, access);
        mvc.perform(get("/api/users").header(AUTH, "Bearer " + access)).andExpect(status().isOk());
    }

    @Test
    void wrongOtp_isRejected_andReportsRemainingAttempts() throws Exception {
        String preAuth = loginOk(admin).get("preAuthToken").asText();
        String wrong = wrongCodeFor(otpCodeOf(admin));

        doVerify(preAuth, wrong).andExpect(status().isUnauthorized())
                .andExpect(errorMessageContains("còn 4 lần"));
        doVerify(preAuth, wrong).andExpect(status().isUnauthorized())
                .andExpect(errorMessageContains("còn 3 lần"));
    }

    // ------------------------------------------------------------------ test bắt buộc #2

    @Test
    void otpWrongTooManyTimes_invalidatesTheCode_evenIfTheCorrectCodeComesLater() throws Exception {
        String preAuth = loginOk(admin).get("preAuthToken").asText();
        String correct = otpCodeOf(admin);
        String wrong = wrongCodeFor(correct);

        for (int i = 1; i <= 4; i++) {
            doVerify(preAuth, wrong).andExpect(status().isUnauthorized());
        }
        // lần sai thứ 5 → mã bị hủy
        doVerify(preAuth, wrong).andExpect(status().isUnauthorized())
                .andExpect(errorMessageContains("quá nhiều lần"));
        assertTrue(latestOtp(admin).isUsed(), "mã phải bị đánh dấu hủy sau khi sai quá số lần");

        // lần thứ 6, dù gửi ĐÚNG mã vẫn phải thất bại
        doVerify(preAuth, correct).andExpect(status().isUnauthorized());
    }

    @Test
    void otp_cannotBeUsedTwice() throws Exception {
        String preAuth = loginOk(admin).get("preAuthToken").asText();
        String code = otpCodeOf(admin);

        doVerify(preAuth, code).andExpect(status().isOk());
        doVerify(preAuth, code).andExpect(status().isUnauthorized());
    }

    @Test
    void expiredOtp_isRejected() throws Exception {
        String preAuth = loginOk(admin).get("preAuthToken").asText();
        OtpToken token = latestOtp(admin);
        token.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        otpRepo.saveAndFlush(token);

        doVerify(preAuth, token.getCode()).andExpect(status().isUnauthorized())
                .andExpect(errorMessageContains("hết hạn"));
    }

    @Test
    void otpVerify_takesTheUserFromTheToken_notFromAnythingTheClientSends() throws Exception {
        User otherAdmin = newUser("adm2", "ADMIN", null);
        String preAuthA = loginOk(admin).get("preAuthToken").asText();
        loginOk(otherAdmin);

        // dùng phiên của A nhưng thử mã OTP của B (kèm cả username của B trong body) → phải thất bại
        mvc.perform(post("/api/auth/otp/verify").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("preAuthToken", preAuthA,
                                "code", otpCodeOf(otherAdmin), "username", otherAdmin.getUsername()))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void otpEndpoints_rejectTokensOfTheWrongScope() throws Exception {
        String access = tokenOf(student);
        String pwdChange = tokens.generatePreAuthToken(admin.getUsername(), admin.getId(), admin.getTokenVersion(), Scope.PWD_CHANGE);
        loginOk(admin);

        doVerify(access, "123456").andExpect(status().isUnauthorized());
        doVerify(pwdChange, otpCodeOf(admin)).andExpect(status().isUnauthorized());
        doVerify("khong-phai-jwt", "123456").andExpect(status().isUnauthorized());
        doResend(access).andExpect(status().isUnauthorized());
    }

    @Test
    void preAuthToken_diesWhenTheAccountIsLockedMeanwhile() throws Exception {
        String preAuth = loginOk(admin).get("preAuthToken").asText();
        String code = otpCodeOf(admin);

        admin.setActive(false);
        admin.setTokenVersion(admin.getTokenVersion() + 1);
        userRepo.saveAndFlush(admin);

        doVerify(preAuth, code).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ gửi lại OTP

    @Test
    void resend_withinCooldown_returns429_withRetryAfter() throws Exception {
        String preAuth = loginOk(admin).get("preAuthToken").asText();

        doResend(preAuth).andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(errorMessageContains("chờ"));
    }

    @Test
    void resend_afterCooldown_cancelsOldCode_andIssuesANewOne() throws Exception {
        String preAuth = loginOk(admin).get("preAuthToken").asText();
        OtpToken first = latestOtp(admin);
        String oldCode = first.getCode();
        first.setCreatedAt(LocalDateTime.now().minusMinutes(2)); // giả lập đã quá 60 giây
        otpRepo.saveAndFlush(first);

        doResend(preAuth).andExpect(status().isOk())
                .andExpect(jsonPath("$.sent").value(true))
                .andExpect(jsonPath("$.cooldownSeconds").value(60));

        OtpToken second = latestOtp(admin);
        assertNotEquals(first.getId(), second.getId());
        assertTrue(otpRepo.findById(first.getId()).orElseThrow().isUsed(), "mã cũ phải bị hủy");
        if (!oldCode.equals(second.getCode())) {
            doVerify(preAuth, oldCode).andExpect(status().isUnauthorized());
        }
        doVerify(preAuth, second.getCode()).andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ thông báo lỗi & nhật ký

    @Test
    void wrongPassword_unknownUser_lockedAccount_allGiveTheSameGenericMessage() throws Exception {
        User locked = newUser("lck", "STUDENT", dept);
        locked.setActive(false);
        userRepo.saveAndFlush(locked);

        String message = "Sai tên đăng nhập hoặc mật khẩu";
        doLogin(student.getUsername(), "sai-mat-khau").andExpect(status().isUnauthorized())
                .andExpect(errorMessageIs(message));
        doLogin("khong-ton-tai-" + unique("x"), PASSWORD).andExpect(status().isUnauthorized())
                .andExpect(errorMessageIs(message));
        doLogin(locked.getUsername(), PASSWORD).andExpect(status().isUnauthorized())
                .andExpect(errorMessageIs(message));
    }

    @Test
    void login_requiresUsernameAndPassword() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"\",\"password\":\"x\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/otp/verify").contentType(MediaType.APPLICATION_JSON).content("{\"preAuthToken\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginAttempts_areWrittenToTheAuditLog_withTheAttemptedUsernameAsActor() throws Exception {
        doLogin(student.getUsername(), "sai-mat-khau").andExpect(status().isUnauthorized());
        loginOk(student);

        assertTrue(hasAudit("LOGIN_FAILED", student.getUsername()), "thiếu LOGIN_FAILED");
        assertTrue(hasAudit("LOGIN_SUCCESS", student.getUsername()), "thiếu LOGIN_SUCCESS");
    }

    @Test
    void auditLog_neverContainsThePasswordOrTheOtpCode() throws Exception {
        doLogin(student.getUsername(), "mat-khau-bi-mat-123").andExpect(status().isUnauthorized());
        String preAuth = loginOk(admin).get("preAuthToken").asText();
        String code = otpCodeOf(admin);
        doVerify(preAuth, wrongCodeFor(code));

        for (AuditLog log : auditRepo.findAll()) {
            String details = String.valueOf(log.getDetails());
            assertFalse(details.contains("mat-khau-bi-mat-123"), "mật khẩu lọt vào audit log");
            if (log.getActorUsername() != null && log.getActorUsername().equals(admin.getUsername())) {
                assertFalse(details.contains(code), "mã OTP lọt vào audit log");
            }
        }
    }

    private boolean hasAudit(String action, String actor) {
        return auditRepo.findAll().stream()
                .anyMatch(l -> action.equals(l.getAction()) && actor.equals(l.getActorUsername()));
    }
}
