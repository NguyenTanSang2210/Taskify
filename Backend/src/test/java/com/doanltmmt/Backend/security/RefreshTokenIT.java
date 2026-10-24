package com.doanltmmt.Backend.security;

import com.doanltmmt.Backend.entity.Department;
import com.doanltmmt.Backend.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.jayway.jsonpath.JsonPath;

import java.util.Base64;
import java.util.Map;

/** P3-14 — access token 15 phút + refresh token xoay vòng trong cookie HttpOnly, phát hiện dùng lại, đăng xuất. */
class RefreshTokenIT extends AbstractSecurityIT {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager em;

    private User student;

    @BeforeEach
    void setUp() {
        Department dept = newDepartment("RT");
        student = newStudent("srt", dept, newClass(dept)).getUser();
    }

    private MvcResult login() throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", student.getUsername(), "password", PASSWORD))))
                .andExpect(status().isOk()).andReturn();
    }

    private static Cookie cookieOf(MvcResult r) {
        Cookie c = r.getResponse().getCookie(RefreshCookie.NAME);
        assertNotNull(c, "phải có cookie " + RefreshCookie.NAME);
        return c;
    }

    private ResultActions refresh(Cookie c) throws Exception {
        return mvc.perform(post("/api/auth/refresh").cookie(c));
    }

    @Test
    void login_setsHttpOnlyStrictCookie_andShortLivedAccessToken() throws Exception {
        MvcResult r = login();
        String setCookie = r.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        org.hamcrest.MatcherAssert.assertThat(setCookie, allOf(containsString("HttpOnly"), containsString("SameSite=Strict"),
                containsString("Path=/api/auth")));
        String token = JsonPath.read(r.getResponse().getContentAsString(), "$.token");
        Map<?, ?> claims = json.readValue(Base64.getUrlDecoder().decode(token.split("\\.")[1]), Map.class);
        assertEquals(900, ((Number) claims.get("exp")).longValue() - ((Number) claims.get("iat")).longValue(), "access token sống 15 phút");
    }

    @Test
    void refresh_rotatesTheCookie_andReturnsAWorkingAccessToken() throws Exception {
        Cookie first = cookieOf(login());
        MvcResult r = refresh(first).andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(student.getUsername()))
                .andExpect(jsonPath("$.role").value("STUDENT")).andReturn();
        Cookie second = cookieOf(r);
        assertNotEquals(first.getValue(), second.getValue());
        String token = JsonPath.read(r.getResponse().getContentAsString(), "$.token");
        mvc.perform(get("/api/users/{id}", student.getId()).header(AUTH, "Bearer " + token)).andExpect(status().isOk());
        refresh(second).andExpect(status().isOk());
    }

    @Test
    void reusingARotatedToken_revokesEverySession() throws Exception {
        Cookie stolen = cookieOf(login());
        Cookie current = cookieOf(refresh(stolen).andExpect(status().isOk()).andReturn());
        // quá thời gian ân hạn 30 giây (hai tab làm mới cùng lúc)
        em.flush();
        jdbc.update("update refresh_tokens set revoked_at = date_sub(revoked_at, interval 5 minute) where user_id = ?", student.getId());
        em.flush();
        em.clear();
        String accessBefore = bearer(userRepo.findById(student.getId()).orElseThrow());

        refresh(stolen).andExpect(status().isUnauthorized());
        em.flush();
        em.clear();
        refresh(current).andExpect(status().isUnauthorized()); // phiên của chủ thật cũng bị thu hồi
        mvc.perform(get("/api/users/{id}", student.getId()).header(AUTH, accessBefore)).andExpect(status().isUnauthorized());
        assertEquals(0L, jdbc.queryForObject("select count(*) from refresh_tokens where user_id = ? and revoked_at is null",
                Long.class, student.getId()));
    }

    @Test
    void twoTabsRefreshingAtOnce_bothSucceed() throws Exception {
        Cookie shared = cookieOf(login());
        refresh(shared).andExpect(status().isOk());
        refresh(shared).andExpect(status().isOk()); // trong 30 giây ân hạn: không báo động dùng lại
    }

    @Test
    void logout_revokesTheSession_andClearsTheCookie() throws Exception {
        Cookie c = cookieOf(login());
        MvcResult out = mvc.perform(post("/api/auth/logout").cookie(c)).andExpect(status().isOk()).andReturn();
        assertEquals(0, cookieOf(out).getMaxAge());
        refresh(c).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void crossSiteRequestIsRejected() throws Exception {
        Cookie c = cookieOf(login());
        mvc.perform(post("/api/auth/refresh").cookie(c).header(HttpHeaders.ORIGIN, "https://evil.example"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/refresh").cookie(c).header(HttpHeaders.ORIGIN, "http://localhost:5175"))
                .andExpect(status().isOk());
    }

    @Test
    void changingPassword_endsOtherSessions_butKeepsThisDevice() throws Exception {
        Cookie otherDevice = cookieOf(login());
        MvcResult changed = mvc.perform(put("/api/users/{id}/password", student.getId()).header(AUTH, bearer(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("oldPassword", PASSWORD, "newPassword", "MatKhauMoi2026"))))
                .andExpect(status().isOk()).andExpect(header().exists(HttpHeaders.SET_COOKIE)).andReturn();
        em.flush();
        em.clear();
        refresh(otherDevice).andExpect(status().isUnauthorized());
        refresh(cookieOf(changed)).andExpect(status().isOk());
    }

    @Test
    void lockedAccount_cannotRefresh() throws Exception {
        Cookie c = cookieOf(login());
        em.flush();
        jdbc.update("update users set active = 0 where id = ?", student.getId());
        em.flush();
        em.clear();
        refresh(c).andExpect(status().isUnauthorized());
    }
}
