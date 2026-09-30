package com.doanltmmt.Backend.security;

import com.doanltmmt.Backend.entity.Department;
import com.doanltmmt.Backend.entity.Role;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.security.JwtTokenProvider.Scope;
import com.jayway.jsonpath.JsonPath;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Base64;
import java.util.Date;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P1-02 — thu hồi token & chặn tài khoản bị khóa; phản hồi 401 (chưa xác thực) khác 403 (không đủ quyền).
 * Bao phủ test bắt buộc #3 (user bị khóa dùng token cũ) và #4 (token sau khi đổi mật khẩu).
 */
@SuppressWarnings("null")
class TokenRevocationIT extends AbstractSecurityIT {

    @Value("${jwt.secret:}")
    private String jwtSecret;

    private Department dept;
    private User admin;
    private User student;

    @BeforeEach
    void setUp() {
        dept = newDepartment("REV");
        admin = newUser("adm", "ADMIN", null);
        student = newUser("stu", "STUDENT", dept);
    }

    private void expectOk(String token) throws Exception {
        mvc.perform(get("/api/users/{id}", student.getId()).header(AUTH, token)).andExpect(status().isOk());
    }

    private void expectUnauthorized(String token) throws Exception {
        mvc.perform(get("/api/users/{id}", student.getId()).header(AUTH, token)).andExpect(status().isUnauthorized());
    }

    @Test
    void validToken_isAccepted() throws Exception {
        expectOk(bearer(student));
    }

    // ------------------------------------------------------------------ test bắt buộc #3

    @Test
    void lockedUser_oldTokenIsRejectedImmediately() throws Exception {
        String studentToken = bearer(student);
        expectOk(studentToken);

        mvc.perform(put("/api/users/{id}/status", student.getId()).param("active", "false").header(AUTH, bearer(admin)))
                .andExpect(status().isOk());

        expectUnauthorized(studentToken);
    }

    @Test
    void lockedThenUnlockedUser_oldTokenStaysRevoked_newTokenWorks() throws Exception {
        String oldToken = bearer(student);
        mvc.perform(put("/api/users/{id}/status", student.getId()).param("active", "false").header(AUTH, bearer(admin)))
                .andExpect(status().isOk());
        mvc.perform(put("/api/users/{id}/status", student.getId()).param("active", "true").header(AUTH, bearer(admin)))
                .andExpect(status().isOk());

        expectUnauthorized(oldToken);            // mở khóa KHÔNG làm token cũ sống lại
        expectOk(bearer(userRepo.findById(student.getId()).orElseThrow())); // token mới (tokenVersion mới) dùng được
    }

    @Test
    void unactivatedUser_tokenIsRejected() throws Exception {
        String token = bearer(student);
        student.setActivated(false);
        userRepo.saveAndFlush(student);

        expectUnauthorized(token);
    }

    // ------------------------------------------------------------------ test bắt buộc #4

    @Test
    void passwordChange_invalidatesOldToken_andReturnsWorkingNewToken() throws Exception {
        String oldToken = bearer(student);
        String body = "{\"oldPassword\":\"" + PASSWORD + "\",\"newPassword\":\"Another-Passw0rd\"}";

        MvcResult result = mvc.perform(put("/api/users/{id}/password", student.getId())
                        .header(AUTH, oldToken).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();
        String newToken = "Bearer " + JsonPath.read(result.getResponse().getContentAsString(), "$.token");

        expectUnauthorized(oldToken);
        expectOk(newToken);
    }

    @Test
    void roleChange_invalidatesTokenCarryingTheOldRole() throws Exception {
        String oldToken = bearer(student);
        Role lecturerRole = roleRepo.findByName("LECTURER").orElseThrow();

        mvc.perform(put("/api/users/{id}/role", student.getId()).param("roleId", String.valueOf(lecturerRole.getId()))
                        .header(AUTH, bearer(admin)))
                .andExpect(status().isOk());

        expectUnauthorized(oldToken);
    }

    @Test
    void logoutAll_invalidatesEveryTokenOfTheUser() throws Exception {
        String tokenA = bearer(student);
        String tokenB = bearer(student); // hai thiết bị cùng phiên bản token

        mvc.perform(post("/api/auth/logout-all").header(AUTH, tokenA)).andExpect(status().isOk());

        expectUnauthorized(tokenA);
        expectUnauthorized(tokenB);
    }

    @Test
    void logoutAll_requiresAuthentication() throws Exception {
        mvc.perform(post("/api/auth/logout-all")).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenOfDeletedUser_gets401_notServerError() throws Exception {
        String token = bearer(student);
        userRepo.delete(student);
        userRepo.flush();

        mvc.perform(get("/api/users/{id}", student.getId()).header(AUTH, token)).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ 401 khác 403

    @Test
    void noToken_gets401WithJsonBody_notTheOldEmpty403() throws Exception {
        mvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value(containsString("đăng nhập")));
    }

    @Test
    void garbageToken_gets401() throws Exception {
        mvc.perform(get("/api/users").header(AUTH, "Bearer abc.def.ghi")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users").header(AUTH, "Bearer ")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users").header(AUTH, "Basic dXNlcjpwYXNz")).andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedButNotAllowed_gets403WithVietnameseMessage() throws Exception {
        mvc.perform(get("/api/users").header(AUTH, bearer(student)))   // /api/users chỉ dành cho ADMIN
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value(containsString("không có quyền")));
    }

    // ------------------------------------------------------------------ token sai loại / token cũ

    @Test
    void preAuthTokens_areRejectedByTheApiFilter() throws Exception {
        String otp = "Bearer " + tokens.generatePreAuthToken(student.getUsername(), student.getId(), student.getTokenVersion(), Scope.OTP_PENDING);
        String pwd = "Bearer " + tokens.generatePreAuthToken(student.getUsername(), student.getId(), student.getTokenVersion(), Scope.PWD_CHANGE);

        expectUnauthorized(otp);
        expectUnauthorized(pwd);
    }

    @Test
    void legacyTokenWithoutScopeAndVersion_isRejected() throws Exception {
        String legacy = Jwts.builder()
                .subject(student.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .claim("role", "STUDENT").claim("userId", student.getId())
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret)), Jwts.SIG.HS256)
                .compact();

        expectUnauthorized("Bearer " + legacy);
    }

    @Test
    void tokenVersionStartsAtZeroAndOnlyGrowsOnRevocation() {
        assertEquals(0, student.getTokenVersion());
        student.setTokenVersion(student.getTokenVersion() + 1);
        assertEquals(1, userRepo.saveAndFlush(student).getTokenVersion());
    }
}
