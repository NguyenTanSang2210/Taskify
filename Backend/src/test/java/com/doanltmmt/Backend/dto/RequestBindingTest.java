package com.doanltmmt.Backend.dto;

import com.doanltmmt.Backend.config.JacksonConfig;
import com.doanltmmt.Backend.dto.request.LecturerAssignRequest;
import com.doanltmmt.Backend.dto.request.MilestoneRequest;
import com.doanltmmt.Backend.dto.request.UserCreateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** P3-02 — request DTO nhận đúng dạng dữ liệu form frontend gửi (chuỗi rỗng, số dạng chuỗi, ngày không có giờ). */
class RequestBindingTest {

    private final ObjectMapper json = Jackson2ObjectMapperBuilder.json()
            .modulesToInstall(new JacksonConfig().lenientLocalDateTime()).build();

    @Test
    void emptyStringsBecomeNull_andNumericStringsAreParsed() throws Exception {
        LecturerAssignRequest r = json.readValue(
                "{\"workspaceId\":\"5\",\"lecturerId\":7,\"type\":\" assistant \",\"quotaMaxGroups\":\"\",\"reason\":\"  \"}",
                LecturerAssignRequest.class);
        assertEquals(5L, r.workspaceId());
        assertEquals("ASSISTANT", r.type());
        assertEquals(0, r.quotaMaxGroups());
        assertNull(r.reason());
    }

    @Test
    void dateOnlyMeansStartOfDay_andBlankDateIsNull() throws Exception {
        MilestoneRequest m = json.readValue("{\"title\":\" M1 \",\"startDate\":\"\",\"deadline\":\"2030-12-31\"}", MilestoneRequest.class);
        assertEquals("M1", m.title());
        assertNull(m.startDate());
        assertEquals(LocalDateTime.of(2030, 12, 31, 0, 0), m.deadline());
        m = json.readValue("{\"deadline\":\"2030-12-31T08:30\"}", MilestoneRequest.class);
        assertEquals(LocalDateTime.of(2030, 12, 31, 8, 30), m.deadline());
    }

    @Test
    void optionalIdsMayBeEmpty() throws Exception {
        UserCreateRequest u = json.readValue(
                "{\"username\":\"sv1\",\"fullName\":\"A\",\"roleId\":\"3\",\"departmentId\":\"\",\"academicClassId\":null,\"active\":\"true\"}",
                UserCreateRequest.class);
        assertEquals(3L, u.roleId());
        assertNull(u.departmentId());
        assertEquals(Boolean.TRUE, u.active());
    }
}
