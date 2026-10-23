package com.doanltmmt.Backend.progress;

import com.doanltmmt.Backend.entity.AcademicClass;
import com.doanltmmt.Backend.entity.Department;
import com.doanltmmt.Backend.entity.Lecturer;
import com.doanltmmt.Backend.entity.Student;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.Workspace;
import com.doanltmmt.Backend.security.AbstractSecurityIT;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.nullValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.jayway.jsonpath.JsonPath;

import java.util.Map;

/** P2-08 — báo cáo gần giống báo cáo của SV khác cùng workspace: GV thấy cảnh báo, SV không thấy. */
@SuppressWarnings("null")
class ProgressSimilarityIT extends AbstractSecurityIT {

    private static final String REPORT = "Tuần này em đã xây dựng hệ thống quản lý đề tài, hoàn thành chức năng đăng nhập "
            + "và phân quyền người dùng bằng Spring Security.";
    private static final String COPY = "Trong tuần này em xây dựng hệ thống quản lý đề tài: đã hoàn thành chức năng "
            + "đăng nhập, phân quyền người dùng bằng Spring Security!";

    private Lecturer lecturer;
    private Topic topicA;
    private Topic topicB;
    private Student s1;
    private Student s2;

    @BeforeEach
    void setUp() {
        Department dept = newDepartment("SIM");
        AcademicClass cls = newClass(dept);
        lecturer = newLecturer("lsim", dept);
        Workspace ws = newWorkspace(dept, "IN_PROGRESS");
        topicA = newTopic(lecturer, ws, 2);
        topicB = newTopic(lecturer, ws, 2);
        s1 = newStudent("ssim1", dept, cls);
        s2 = newStudent("ssim2", dept, cls);
        newRegistration(s1, topicA, true);
        newRegistration(s2, topicB, true);
    }

    private long submit(Student s, Topic t, String content) throws Exception {
        String body = mvc.perform(post("/api/progress/create").param("studentId", s.getId().toString())
                        .param("topicId", t.getId().toString()).header(AUTH, bearer(s.getUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("title", "Báo cáo tuần", "content", content))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.similarityScore").value(nullValue()))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    @Test
    void copiedReportFromAnotherStudent_isFlaggedForLecturerOnly() throws Exception {
        long original = submit(s1, topicA, REPORT);
        long copy = submit(s2, topicB, COPY);

        mvc.perform(get("/api/progress/topic/{id}", topicB.getId()).header(AUTH, bearer(lecturer.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(copy))
                .andExpect(jsonPath("$.items[0].similarityScore").value(greaterThanOrEqualTo(0.8)))
                .andExpect(jsonPath("$.items[0].similarToReportId").value(original));

        // SV không thấy điểm trùng lặp (không lộ báo cáo của bạn khác)
        mvc.perform(get("/api/progress/student/{id}", s2.getId()).param("topicId", topicB.getId().toString())
                        .header(AUTH, bearer(s2.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].similarityScore").value(nullValue()))
                .andExpect(jsonPath("$[0].similarToReportId").value(nullValue()));
    }
}
