package com.doanltmmt.Backend.validation;

import com.doanltmmt.Backend.entity.AcademicClass;
import com.doanltmmt.Backend.entity.Department;
import com.doanltmmt.Backend.entity.Lecturer;
import com.doanltmmt.Backend.entity.Student;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.TopicRegistration;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.entity.Workspace;
import com.doanltmmt.Backend.security.AbstractSecurityIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** P2-09 — dữ liệu đầu vào sai trả 400 với thông báo rõ ràng, không còn 500 hay bị bỏ qua im lặng. */
@SuppressWarnings("null")
class BusinessValidationIT extends AbstractSecurityIT {

    private Department dept;
    private Lecturer lecturer;
    private Workspace wsOpenTopic;
    private Workspace wsInProgress;
    private Topic topic;
    private Student student;
    private TopicRegistration reg;

    @BeforeEach
    void setUp() {
        dept = newDepartment("VAL");
        AcademicClass cls = newClass(dept);
        lecturer = newLecturer("lval", dept);
        wsOpenTopic = newWorkspace(dept, "OPEN_TOPIC");
        wsInProgress = newWorkspace(dept, "IN_PROGRESS");
        topic = newTopic(lecturer, wsInProgress, 2);
        student = newStudent("sval", dept, cls);
        reg = newRegistration(student, topic, true);
    }

    private ResultActions call(HttpMethod method, String url, User as, Object body, Object... vars) throws Exception {
        MockHttpServletRequestBuilder b = request(method, url, vars).header(AUTH, bearer(as));
        if (body != null) b.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        return mvc.perform(b);
    }

    private User lec() {
        return lecturer.getUser();
    }

    // ---------------------------------------------------------------- 1. điểm

    private ResultActions grade(Object score) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("score", score);
        body.put("feedback", "Ổn");
        return call(HttpMethod.POST, "/api/registration/grade/{id}", lec(), body, reg.getId());
    }

    @Test
    void grade_rejectsMissingOutOfRangeAndOddScores() throws Exception {
        grade(null).andExpect(status().isBadRequest()).andExpect(errorMessageContains("nhập điểm"));
        grade(-1).andExpect(status().isBadRequest()).andExpect(errorMessageContains("từ 0 đến 10"));
        grade(10.5).andExpect(status().isBadRequest()).andExpect(errorMessageContains("từ 0 đến 10"));
        grade("abc").andExpect(status().isBadRequest()).andExpect(errorMessageContains("là một số"));
        grade(7.3).andExpect(status().isBadRequest()).andExpect(errorMessageContains("0,25"));
        grade(7.75).andExpect(status().isOk()).andExpect(jsonPath("$.score").value(7.75));
    }

    // ---------------------------------------------------------------- 2–3. đề tài

    @Test
    void updateTopic_ignoresStatusFromBody_andValidatesCapacity() throws Exception {
        Topic t = newTopic(lecturer, wsOpenTopic, 3);
        call(HttpMethod.PUT, "/api/topics/{id}?lecturerId={l}", lec(), Map.of("status", "COMPLETED", "title", "Mới"),
                t.getId(), lec().getId()).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("OPEN"));
        call(HttpMethod.PUT, "/api/topics/{id}?lecturerId={l}", lec(), Map.of("capacity", 0), t.getId(), lec().getId())
                .andExpect(status().isBadRequest()).andExpect(errorMessageContains("từ 1 đến 10"));
        call(HttpMethod.PUT, "/api/topics/{id}?lecturerId={l}", lec(), Map.of("capacity", 11), t.getId(), lec().getId())
                .andExpect(status().isBadRequest());
        call(HttpMethod.PUT, "/api/topics/{id}?lecturerId={l}", lec(), Map.of("title", "  "), t.getId(), lec().getId())
                .andExpect(status().isBadRequest());
    }

    @Test
    void capacity_cannotDropBelowApprovedStudents_andShrinkingMarksTopicFull() throws Exception {
        Topic t = newTopic(lecturer, wsOpenTopic, 3);
        newRegistration(newStudent("sv2", dept, newClass(dept)), t, true);
        newRegistration(newStudent("sv3", dept, newClass(dept)), t, true);
        call(HttpMethod.PUT, "/api/topics/{id}?lecturerId={l}", lec(), Map.of("capacity", 1), t.getId(), lec().getId())
                .andExpect(status().isBadRequest()).andExpect(errorMessageContains("đã được duyệt (2)"));
        call(HttpMethod.PUT, "/api/topics/{id}?lecturerId={l}", lec(), Map.of("capacity", 2), t.getId(), lec().getId())
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("FULL"));
    }

    @Test
    void createTopic_cannotOverwriteAnotherTopicThroughId() throws Exception {
        Topic other = newTopic(lecturer, wsOpenTopic, 2);
        String before = other.getTitle();
        Map<String, Object> body = Map.of("id", other.getId(), "title", "Ghi đè", "workspace", Map.of("id", wsOpenTopic.getId()));
        call(HttpMethod.POST, "/api/topics/create?lecturerId={l}", lec(), body, lec().getId())
                .andExpect(status().isOk()).andExpect(jsonPath("$.capacity").value(1));
        assertEquals(before, topicRepo.findById(other.getId()).orElseThrow().getTitle());
    }

    @Test
    void lecturer_seesWorkspacesOpenForTopics_ofOwnDepartmentOnly() throws Exception {
        newWorkspace(newDepartment("VAL2"), "OPEN_TOPIC");
        call(HttpMethod.GET, "/api/workspaces/open-for-topics", lec(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(wsOpenTopic.getId()));
        call(HttpMethod.GET, "/api/workspaces/open-for-topics", student.getUser(), null).andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- 6. tiến độ

    @Test
    void progress_invalidDeadline_isRejected_notSilentlyIgnored() throws Exception {
        call(HttpMethod.POST, "/api/progress/create?studentId={s}&topicId={t}", student.getUser(),
                Map.of("title", "Tuần 1", "content", "Nội dung", "deadline", "31/12/2030"), student.getId(), topic.getId())
                .andExpect(status().isBadRequest()).andExpect(errorMessageContains("Hạn chót không đúng định dạng"));
        call(HttpMethod.POST, "/api/progress/create?studentId={s}&topicId={t}", student.getUser(),
                Map.of("title", "", "content", "Nội dung"), student.getId(), topic.getId())
                .andExpect(status().isBadRequest()).andExpect(errorMessageContains("Tiêu đề báo cáo"));
    }

    // ---------------------------------------------------------------- 7. milestone

    @Test
    void milestone_datesMustBeOrderedAndInsideWorkspace() throws Exception {
        wsInProgress.setStartAt(LocalDateTime.of(2030, 1, 1, 0, 0));
        wsInProgress.setEndAt(LocalDateTime.of(2030, 6, 30, 23, 59));
        workspaceRepo.save(wsInProgress);
        String url = "/api/milestones";
        call(HttpMethod.POST, url, lec(), Map.of("topicId", topic.getId(), "title", "M1",
                "startDate", "2030-03-10T00:00", "deadline", "2030-03-01T00:00"))
                .andExpect(status().isBadRequest()).andExpect(errorMessageContains("sau ngày bắt đầu"));
        call(HttpMethod.POST, url, lec(), Map.of("topicId", topic.getId(), "title", "M1", "deadline", "2030-08-01T00:00"))
                .andExpect(status().isBadRequest()).andExpect(errorMessageContains("thời gian của đợt"));
        call(HttpMethod.POST, url, lec(), Map.of("topicId", topic.getId(), "title", "M1"))
                .andExpect(status().isBadRequest()).andExpect(errorMessageContains("Hạn chót là bắt buộc"));
        call(HttpMethod.POST, url, lec(), Map.of("topicId", "abc", "title", "M1", "deadline", "2030-03-01T00:00"))
                .andExpect(status().isBadRequest());
        call(HttpMethod.POST, url, lec(), Map.of("topicId", topic.getId(), "title", "M1", "deadline", "2030-03-01T23:59"))
                .andExpect(status().isOk());
    }

    // ---------------------------------------------------------------- 8. workspace

    @Test
    void workspace_endMustNotPrecedeStart() throws Exception {
        User deptAdmin = newUser("dval", "DEPARTMENT_ADMIN", dept);
        call(HttpMethod.POST, "/api/workspaces", deptAdmin,
                Map.of("name", "Đợt lỗi", "startAt", "2030-06-01T00:00", "endAt", "2030-01-01T00:00"))
                .andExpect(status().isBadRequest()).andExpect(errorMessageContains("Ngày kết thúc phải sau"));
        call(HttpMethod.POST, "/api/workspaces", deptAdmin, Map.of("name", "Đợt lỗi", "startAt", "không-phải-ngày"))
                .andExpect(status().isBadRequest());
        call(HttpMethod.POST, "/api/workspaces", newUser("aval", "ADMIN", null), Map.of("name", "Thiếu khoa"))
                .andExpect(status().isBadRequest()).andExpect(errorMessageContains("departmentId"));
    }

    // ---------------------------------------------------------------- 9–10. tin nhắn, thảo luận

    @Test
    void messagesAndDiscussions_validateRecipientAndLength() throws Exception {
        User s = student.getUser();
        call(HttpMethod.POST, "/api/messages/send", s, Map.of("topicId", topic.getId().toString(), "content", "Chào thầy"))
                .andExpect(status().isBadRequest()).andExpect(errorMessageContains("Người nhận"));
        call(HttpMethod.POST, "/api/discuss/threads", s, Map.of("topicId", topic.getId().toString(), "title", " "))
                .andExpect(status().isBadRequest()).andExpect(errorMessageContains("Tiêu đề chủ đề"));
        String threadBody = call(HttpMethod.POST, "/api/discuss/threads", s,
                Map.of("topicId", topic.getId().toString(), "title", "Hỏi về đề tài"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Number threadId = com.jayway.jsonpath.JsonPath.read(threadBody, "$.id");
        call(HttpMethod.POST, "/api/discuss/threads/{id}/posts", s, Map.of("content", "x".repeat(5001)), threadId)
                .andExpect(status().isBadRequest()).andExpect(errorMessageContains("tối đa 5000"));
        call(HttpMethod.POST, "/api/discuss/threads/{id}/posts", s, Map.of("content", ""), threadId)
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- id thiếu/sai ở các API quản trị

    @Test
    void adminEndpoints_missingOrInvalidIds_are400_not500() throws Exception {
        User deptAdmin = newUser("dval2", "DEPARTMENT_ADMIN", dept);
        call(HttpMethod.POST, "/api/workspace-classes/assign", deptAdmin, Map.of())
                .andExpect(status().isBadRequest()).andExpect(errorMessageContains("workspaceId là bắt buộc"));
        call(HttpMethod.POST, "/api/assignments/assign", deptAdmin, Map.of("workspaceId", "abc", "lecturerId", 1))
                .andExpect(status().isBadRequest()).andExpect(errorMessageContains("workspaceId không hợp lệ"));
        call(HttpMethod.POST, "/api/assignments/assign", deptAdmin,
                Map.of("workspaceId", wsOpenTopic.getId(), "lecturerId", lec().getId(), "quotaMaxGroups", -1))
                .andExpect(status().isBadRequest()).andExpect(errorMessageContains("không được âm"));
        call(HttpMethod.POST, "/api/assignments/revoke", deptAdmin, Map.of()).andExpect(status().isBadRequest());
    }
}
