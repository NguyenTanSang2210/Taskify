package com.doanltmmt.Backend.flow;

import com.doanltmmt.Backend.entity.enums.TopicStatus;
import com.doanltmmt.Backend.entity.AcademicClass;
import com.doanltmmt.Backend.entity.Department;
import com.doanltmmt.Backend.entity.Lecturer;
import com.doanltmmt.Backend.entity.Student;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.repository.GradeHistoryRepository;
import com.doanltmmt.Backend.repository.NotificationRepository;
import com.doanltmmt.Backend.security.AbstractSecurityIT;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P2-12 — hồi quy nghiệp vụ toàn luồng, chỉ qua API như người dùng thật:
 * tạo đợt → OPEN_TOPIC → GV tạo đề tài (2 chỗ) → gán lớp → OPEN_REGISTRATION → 2 SV đăng ký → GV duyệt (đề tài FULL)
 * → LOCK → IN_PROGRESS → SV nộp báo cáo → GV nhận xét → GV chấm điểm (đề tài COMPLETED) → CLOSED
 * → điểm bị khóa → yêu cầu chấm lại → quản trị khoa duyệt → GV sửa điểm → khóa lại, có lịch sử.
 */
@SuppressWarnings("null")
class FullLifecycleIT extends AbstractSecurityIT {

    @Autowired private GradeHistoryRepository historyRepo;
    @Autowired private NotificationRepository notificationRepo;

    private ResultActions call(HttpMethod method, String url, User as, Object body, Object... vars) throws Exception {
        MockHttpServletRequestBuilder b = request(method, url, vars).header(AUTH, bearer(as));
        if (body != null) b.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        return mvc.perform(b);
    }

    private String ok(HttpMethod method, String url, User as, Object body, Object... vars) throws Exception {
        return call(method, url, as, body, vars).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private static long idOf(String body) {
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private void transition(long wsId, String to, User deptAdmin) throws Exception {
        call(HttpMethod.POST, "/api/workspaces/{id}/transition?to={to}", deptAdmin, null, wsId, to)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(to));
    }

    private TopicStatus topicStatus(long topicId) {
        return topicRepo.findById(topicId).orElseThrow().getStatus();
    }

    @Test
    void wholeProjectLifecycle_throughTheApi() throws Exception {
        Department dept = newDepartment("FLOW");
        AcademicClass cls = newClass(dept);
        User deptAdmin = newUser("dflow", "DEPARTMENT_ADMIN", dept);
        Lecturer lecturer = newLecturer("lflow", dept);
        User lec = lecturer.getUser();
        Student s1 = newStudent("sflow1", dept, cls);
        Student s2 = newStudent("sflow2", dept, cls);

        // 1. Quản trị khoa tạo đợt và mở nhận đề tài
        long wsId = idOf(ok(HttpMethod.POST, "/api/workspaces", deptAdmin,
                Map.of("name", "Đồ án tốt nghiệp", "semester", "HK1", "startAt", "2030-01-01T00:00", "endAt", "2030-06-30T23:59")));
        transition(wsId, "OPEN_TOPIC", deptAdmin);
        ok(HttpMethod.POST, "/api/assignments/assign", deptAdmin, Map.of("workspaceId", wsId, "lecturerId", lec.getId(), "type", "MAIN"));

        // 2. Giảng viên thấy đợt và tự tạo đề tài 2 chỗ
        call(HttpMethod.GET, "/api/workspaces/open-for-topics", lec, null)
                .andExpect(jsonPath("$[?(@.id == " + wsId + ")]").isNotEmpty());
        long topicId = idOf(ok(HttpMethod.POST, "/api/topics/create?lecturerId={l}", lec,
                Map.of("title", "Hệ thống quản lý đồ án", "description", "Spring Boot + React", "capacity", 2,
                        "workspace", Map.of("id", wsId)), lec.getId()));
        assertEquals(TopicStatus.OPEN, topicStatus(topicId));

        // 3. Gán lớp, mở đăng ký, 2 SV đăng ký, GV duyệt cả hai → đề tài đủ chỗ
        ok(HttpMethod.POST, "/api/workspace-classes/assign", deptAdmin, Map.of("workspaceId", wsId, "classId", cls.getId()));
        transition(wsId, "OPEN_REGISTRATION", deptAdmin);
        long reg1 = idOf(ok(HttpMethod.POST, "/api/registration/register?studentId={s}&topicId={t}", s1.getUser(), null, s1.getId(), topicId));
        long reg2 = idOf(ok(HttpMethod.POST, "/api/registration/register?studentId={s}&topicId={t}", s2.getUser(), null, s2.getId(), topicId));
        ok(HttpMethod.POST, "/api/registration/approve/{id}", lec, null, reg1);
        ok(HttpMethod.POST, "/api/registration/approve/{id}", lec, null, reg2);
        assertEquals(TopicStatus.FULL, topicStatus(topicId));

        // 4. Khóa đăng ký → thực hiện
        transition(wsId, "LOCK_REGISTRATION", deptAdmin);
        transition(wsId, "IN_PROGRESS", deptAdmin);

        // 5. SV nộp báo cáo, GV nhận xét → SV nhận thông báo
        long reportId = idOf(ok(HttpMethod.POST, "/api/progress/create?studentId={s}&topicId={t}", s1.getUser(),
                Map.of("title", "Báo cáo tuần 1", "content", "Đã hoàn thành phân tích yêu cầu và thiết kế CSDL",
                        "deadline", "2030-02-01T23:59"), s1.getId(), topicId));
        ok(HttpMethod.PUT, "/api/progress/{id}/comment", lec, Map.of("lecturerComment", "Tốt, tiếp tục", "status", "DONE"), reportId);
        assertTrue(notificationRepo.findByUser_IdOrderByCreatedAtDesc(s1.getUser().getId()).stream()
                .anyMatch(n -> n.getTitle().startsWith("Giảng viên nhận xét")));

        // 6. GV chấm điểm cả hai SV → đề tài hoàn thành
        call(HttpMethod.POST, "/api/registration/grade/{id}", lec, Map.of("score", 8.5, "feedback", "Tốt"), reg1)
                .andExpect(status().isOk()).andExpect(jsonPath("$.gradeState").value("OPEN"));
        assertEquals(TopicStatus.FULL, topicStatus(topicId), "còn SV chưa có điểm");
        ok(HttpMethod.POST, "/api/registration/grade/{id}", lec, Map.of("score", 7.75, "feedback", "Khá"), reg2);
        assertEquals(TopicStatus.COMPLETED, topicStatus(topicId));

        // 7. Đóng đợt → điểm bị khóa
        transition(wsId, "CLOSED", deptAdmin);
        call(HttpMethod.POST, "/api/registration/grade/{id}", lec, Map.of("score", 9, "reason", "Sửa"), reg1)
                .andExpect(status().isBadRequest()).andExpect(errorMessageContains("Điểm đã bị khóa"));

        // 8. Chấm lại: GV gửi yêu cầu → quản trị khoa duyệt → GV sửa một lần → điểm khóa lại
        long requestId = idOf(ok(HttpMethod.POST, "/api/registration/{id}/grade-revisions", lec,
                Map.of("reason", "Hội đồng điều chỉnh điểm bảo vệ"), reg1));
        ok(HttpMethod.POST, "/api/grade-revisions/{id}/approve", deptAdmin, Map.of("unlockHours", 48), requestId);
        Map<String, Object> regrade = new HashMap<>(Map.of("score", 9.0, "feedback", "Xuất sắc", "reason", "Theo biên bản hội đồng"));
        call(HttpMethod.POST, "/api/registration/grade/{id}", lec, regrade, reg1)
                .andExpect(status().isOk()).andExpect(jsonPath("$.score").value(9.0)).andExpect(jsonPath("$.gradeState").value("LOCKED"));
        call(HttpMethod.POST, "/api/registration/grade/{id}", lec, regrade, reg1).andExpect(status().isBadRequest());

        // 9. Truy vết: lịch sử điểm, SV thấy điểm mới
        assertEquals(2, historyRepo.findByRegistration_IdOrderByChangedAtDescIdDesc(reg1).size());
        call(HttpMethod.GET, "/api/registration/mine?studentId={id}", s1.getUser(), null, s1.getId())
                .andExpect(jsonPath("$[0].score").value(9.0))
                .andExpect(jsonPath("$[0].scoreRevisedAt").isNotEmpty());
        call(HttpMethod.GET, "/api/export/excel?topicId={t}", lec, null, topicId).andExpect(status().isOk());
    }
}
