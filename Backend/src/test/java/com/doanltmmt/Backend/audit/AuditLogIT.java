package com.doanltmmt.Backend.audit;

import com.doanltmmt.Backend.entity.AuditLog;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.repository.AuditLogRepository;
import com.doanltmmt.Backend.security.AbstractSecurityIT;
import com.doanltmmt.Backend.service.AuditLogQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** P4-02 — nhật ký hệ thống: lọc theo nhóm/đối tượng, ghi IP + user-agent, xuất CSV, chính sách lưu trữ, chỉ ADMIN. */
@SuppressWarnings("null")
class AuditLogIT extends AbstractSecurityIT {

    @Autowired private AuditLogRepository auditRepo;
    @Autowired private AuditLogQueryService queryService;

    private User admin;
    private String tag;

    @BeforeEach
    void setUp() {
        admin = newUser("adm", "ADMIN", null);
        tag = unique("al");
    }

    private AuditLog entry(String action, String entity, String entityId, String details, LocalDateTime at) {
        AuditLog a = new AuditLog(action, entity, entityId, tag, details);
        a.setTimestamp(at);
        return auditRepo.save(a);
    }

    @Test
    void loggedInRequests_recordIpAndUserAgent() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.USER_AGENT, "Mozilla/5.0 KiemThu")
                        .with(r -> { r.setRemoteAddr("203.0.113.7"); return r; })
                        .content(json.writeValueAsString(Map.of("username", admin.getUsername(), "password", "sai-mat-khau"))))
                .andExpect(status().isUnauthorized());
        AuditLog failed = auditRepo.findAll().stream()
                .filter(a -> "LOGIN_FAILED".equals(a.getAction()) && admin.getUsername().equals(a.getActorUsername()))
                .findFirst().orElseThrow();
        assertEquals("203.0.113.7", failed.getIpAddress());
        assertEquals("Mozilla/5.0 KiemThu", failed.getUserAgent());
    }

    @Test
    void filters_byCategoryEntityAndDateRange() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        entry("GRADE_TOPIC", "TopicRegistration", "77", "score=8.5", now.minusDays(1));
        entry("GRADE_CHANGED", "TopicRegistration", "77", "8.5 -> 9", now);
        entry("CREATE_TOPIC", "Topic", "5", "x", now);
        entry("LOGIN_FAILED", "User", "1", "x", now.minusDays(10));

        mvc.perform(get("/api/audit").param("actor", tag).param("category", "GRADE").header(AUTH, bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.items[*].action", everyItem(startsWith("GRADE_"))))
                .andExpect(jsonPath("$.items[0].action").value("GRADE_CHANGED")); // mới nhất trước
        mvc.perform(get("/api/audit").param("actor", tag).param("entity", "TopicRegistration").param("entityId", "77")
                        .header(AUTH, bearer(admin)))
                .andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/audit").param("actor", tag).param("category", "auth").header(AUTH, bearer(admin)))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/audit").param("actor", tag).param("from", now.minusDays(2).toString()).header(AUTH, bearer(admin)))
                .andExpect(jsonPath("$.totalElements").value(3));
        mvc.perform(get("/api/audit").param("category", "KHONG_CO").header(AUTH, bearer(admin)))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/audit/options").header(AUTH, bearer(admin))).andExpect(status().isOk())
                .andExpect(jsonPath("$.actions", hasItem("GRADE_CHANGED")))
                .andExpect(jsonPath("$.entities", hasItem("TopicRegistration")))
                .andExpect(jsonPath("$.categories", hasItem("GRADE")));
    }

    @Test
    void export_isUtf8CsvWithTheSameFilters_andNeutralisesFormulas() throws Exception {
        entry("GRADE_CHANGED", "TopicRegistration", "9", "Lý do: \"nhập nhầm\", sửa lại", LocalDateTime.now());
        entry("CREATE_TOPIC", "Topic", "5", "=HYPERLINK(\"http://x\")", LocalDateTime.now());

        byte[] body = mvc.perform(get("/api/audit/export").param("actor", tag).header(AUTH, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, startsWith("text/csv")))
                .andReturn().getResponse().getContentAsByteArray();
        String csv = new String(body, StandardCharsets.UTF_8);
        assertTrue(csv.startsWith("﻿Thời gian,Người thực hiện,Hành động"), csv);
        List<String> lines = csv.lines().toList();
        assertEquals(3, lines.size(), csv);
        assertTrue(csv.contains("\"Lý do: \"\"nhập nhầm\"\", sửa lại\""), csv);
        assertTrue(csv.contains("\"'=HYPERLINK(\"\"http://x\"\")\""), "công thức Excel phải bị vô hiệu: " + csv);

        String grades = new String(mvc.perform(get("/api/audit/export").param("actor", tag).param("category", "GRADE")
                .header(AUTH, bearer(admin))).andReturn().getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertEquals(2, grades.lines().count());
    }

    @Test
    void retention_deletesOldEntries_butKeepsGradeHistoryForever() {
        LocalDateTime old = LocalDateTime.now().minusMonths(13);
        AuditLog oldLogin = entry("LOGIN_FAILED", "User", "1", "x", old);
        AuditLog oldGrade = entry("GRADE_CHANGED", "TopicRegistration", "1", "x", old);
        AuditLog recent = entry("CREATE_TOPIC", "Topic", "1", "x", LocalDateTime.now().minusMonths(11));

        assertTrue(queryService.purgeExpired() >= 1);

        assertFalse(auditRepo.existsById(oldLogin.getId()));
        assertTrue(auditRepo.existsById(oldGrade.getId()), "nhật ký điểm số giữ lâu dài");
        assertTrue(auditRepo.existsById(recent.getId()));
    }

    @Test
    void onlyAdmin_canReadOrExport() throws Exception {
        for (String role : List.of("DEPARTMENT_ADMIN", "LECTURER", "STUDENT")) {
            User u = newUser("x", role, newDepartment("AL"));
            for (String path : List.of("/api/audit", "/api/audit/options", "/api/audit/export")) {
                mvc.perform(get(path).header(AUTH, bearer(u))).andExpect(status().isForbidden());
            }
        }
    }
}
