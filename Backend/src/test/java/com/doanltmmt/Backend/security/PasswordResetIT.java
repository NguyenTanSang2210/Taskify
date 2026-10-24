package com.doanltmmt.Backend.security;

import com.doanltmmt.Backend.entity.OtpPurpose;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.repository.AuditLogRepository;
import com.doanltmmt.Backend.repository.OtpTokenRepository;
import com.doanltmmt.Backend.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** P2-05 — quên mật khẩu: mã qua email, không dò được tài khoản, đặt lại xong token cũ vô hiệu. */
@SuppressWarnings("null")
class PasswordResetIT extends AbstractSecurityIT {

    private static final String NEW_PASSWORD = "MatKhauMoi2026";

    @Autowired private OtpTokenRepository otpRepo;
    @Autowired private AuditLogRepository auditRepo;

    private User user;

    @BeforeEach
    void setUp() {
        user = newUser("rst", "STUDENT", newDepartment("RST"));
    }

    private ResultActions forgot(String identifier) throws Exception {
        return mvc.perform(post("/api/auth/password/forgot").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("identifier", identifier))));
    }

    private ResultActions reset(String identifier, String code, String password) throws Exception {
        return mvc.perform(post("/api/auth/password/reset").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("identifier", identifier, "code", code, "newPassword", password))));
    }

    private String codeOf(User u) {
        return otpRepo.findTopByUserAndPurposeOrderByIdDesc(u, OtpPurpose.RESET_PASSWORD).orElseThrow().getCode();
    }

    @Test
    void forgot_sendsCodeByEmail_andAnswersTheSameForUnknownAccounts() throws Exception {
        forgot(user.getUsername()).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(AuthService.RESET_REQUESTED));
        verify(emailService).sendEmail(eq(user.getEmail()), contains("đặt lại mật khẩu"), contains(codeOf(user)));

        // tài khoản không tồn tại: cùng mã trạng thái, cùng thông báo, không gửi email
        forgot("khong_ton_tai_" + System.nanoTime()).andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(AuthService.RESET_REQUESTED));
        verify(emailService, times(1)).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void forgot_byEmail_works() throws Exception {
        forgot(user.getEmail()).andExpect(status().isOk());
        assertTrue(otpRepo.findTopByUserAndPurposeOrderByIdDesc(user, OtpPurpose.RESET_PASSWORD).isPresent());
    }

    @Test
    void reset_withCorrectCode_changesPassword_andRevokesOldTokens() throws Exception {
        String oldToken = bearer(user);
        int versionBefore = user.getTokenVersion();
        forgot(user.getUsername()).andExpect(status().isOk());

        reset(user.getUsername(), codeOf(user), NEW_PASSWORD).andExpect(status().isOk()).andExpect(jsonPath("$.reset").value(true));

        User after = userRepo.findByUsername(user.getUsername()).orElseThrow();
        assertTrue(encoder.matches(NEW_PASSWORD, after.getPassword()));
        assertEquals(versionBefore + 1, after.getTokenVersion());
        mvc.perform(get("/api/notifications").header(AUTH, oldToken)).andExpect(status().isUnauthorized());
        assertTrue(auditRepo.findAll().stream().anyMatch(a -> "PASSWORD_RESET_DONE".equals(a.getAction())));

        // mã chỉ dùng được một lần
        reset(user.getUsername(), codeOf(user), "KhacNua2026x").andExpect(status().isBadRequest());
    }

    @Test
    void reset_withWrongCode_isRejected_withoutTouchingPassword() throws Exception {
        forgot(user.getUsername()).andExpect(status().isOk());
        String wrong = codeOf(user).equals("000000") ? "111111" : "000000";
        reset(user.getUsername(), wrong, NEW_PASSWORD).andExpect(status().isBadRequest())
                .andExpect(errorMessageContains("không đúng hoặc đã hết hạn"));
        assertTrue(encoder.matches(PASSWORD, userRepo.findByUsername(user.getUsername()).orElseThrow().getPassword()));
    }

    @Test
    void reset_withWeakPassword_keepsTheCodeUsable() throws Exception {
        forgot(user.getUsername()).andExpect(status().isOk());
        String code = codeOf(user);
        reset(user.getUsername(), code, "123").andExpect(status().isBadRequest()).andExpect(errorMessageContains("ít nhất 8 ký tự"));
        reset(user.getUsername(), code, NEW_PASSWORD).andExpect(status().isOk());
    }

    @Test
    void reset_forUnknownAccount_givesTheSameError() throws Exception {
        reset("khong_ton_tai_" + System.nanoTime(), "123456", NEW_PASSWORD).andExpect(status().isBadRequest())
                .andExpect(errorMessageContains("không đúng hoặc đã hết hạn"));
    }

    @Test
    void forgot_isRateLimitedPerAccount() throws Exception {
        for (int i = 0; i < 3; i++) forgot(user.getUsername()).andExpect(status().isOk());
        forgot(user.getUsername()).andExpect(status().isTooManyRequests());
    }
}
