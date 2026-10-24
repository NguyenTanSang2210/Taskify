package com.doanltmmt.Backend.security;

import com.doanltmmt.Backend.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** P2-11 (D-07) — tính năng ProjectPermission đã bị xóa hoàn toàn: không còn API, không còn bảng. */
class RemovedFeaturesIT extends AbstractSecurityIT {

    @Autowired private JdbcTemplate jdbc;

    @Test
    void projectPermissionsApi_isGone() throws Exception {
        User admin = newUser("rmadm", "ADMIN", null);
        mvc.perform(get("/api/permissions").header(AUTH, bearer(admin))).andExpect(status().isNotFound());
        mvc.perform(post("/api/permissions").header(AUTH, bearer(admin))).andExpect(status().isNotFound());
    }

    @Test
    void projectPermissionsTable_isDropped() {
        Integer tables = jdbc.queryForObject("select count(*) from information_schema.tables "
                + "where table_schema = database() and table_name = 'project_permissions'", Integer.class);
        assertEquals(0, tables);
    }
}
