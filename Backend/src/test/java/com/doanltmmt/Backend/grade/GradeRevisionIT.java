package com.doanltmmt.Backend.grade;

import com.doanltmmt.Backend.entity.AcademicClass;
import com.doanltmmt.Backend.entity.Department;
import com.doanltmmt.Backend.entity.GradeRevisionRequest;
import com.doanltmmt.Backend.entity.Lecturer;
import com.doanltmmt.Backend.entity.Student;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.TopicRegistration;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.entity.Workspace;
import com.doanltmmt.Backend.repository.GradeHistoryRepository;
import com.doanltmmt.Backend.repository.GradeRevisionRequestRepository;
import com.doanltmmt.Backend.repository.NotificationRepository;
import com.doanltmmt.Backend.security.AbstractSecurityIT;
import com.doanltmmt.Backend.service.GradeService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** P2-13 (D-08) — khóa điểm khi đợt đóng, quy trình chấm lại có duyệt, mọi thay đổi điểm đều có lịch sử. */
@SuppressWarnings("null")
class GradeRevisionIT extends AbstractSecurityIT {

    @Autowired private GradeService gradeService;
    @Autowired private GradeRevisionRequestRepository revisionRepo;
    @Autowired private GradeHistoryRepository historyRepo;
    @Autowired private NotificationRepository notificationRepo;

    private Lecturer lecturer;
    private User deptAdmin;
    private User otherDeptAdmin;
    private Workspace ws;
    private Student student;
    private TopicRegistration reg;

    @BeforeEach
    void setUp() {
        Department dept = newDepartment("GRD");
        AcademicClass cls = newClass(dept);
        lecturer = newLecturer("lgrd", dept);
        deptAdmin = newUser("dgrd", "DEPARTMENT_ADMIN", dept);
        otherDeptAdmin = newUser("dgrd2", "DEPARTMENT_ADMIN", newDepartment("GRD2"));
        ws = newWorkspace(dept, "IN_PROGRESS");
        Topic topic = newTopic(lecturer, ws, 2);
        student = newStudent("sgrd", dept, cls);
        reg = newRegistration(student, topic, true);
    }

    private ResultActions call(HttpMethod method, String url, User as, Object body, Object... vars) throws Exception {
        MockHttpServletRequestBuilder b = request(method, url, vars).header(AUTH, bearer(as));
        if (body != null) b.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        return mvc.perform(b);
    }

    private ResultActions grade(double score, String reason) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("score", score);
        body.put("feedback", "Nhận xét");
        if (reason != null) body.put("reason", reason);
        return call(HttpMethod.POST, "/api/registration/grade/{id}", lecturer.getUser(), body, reg.getId());
    }

    private void closeWorkspace() {
        ws.setStatus("CLOSED");
        workspaceRepo.save(ws);
    }

    private long requestRevision() throws Exception {
        String body = call(HttpMethod.POST, "/api/registration/{id}/grade-revisions", lecturer.getUser(),
                Map.of("reason", "Nhập nhầm điểm bảo vệ"), reg.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private ResultActions approve(long requestId, User as) throws Exception {
        return call(HttpMethod.POST, "/api/grade-revisions/{id}/approve", as, Map.of("note", "Đồng ý", "unlockHours", 24), requestId);
    }

    @Test
    void inProgress_gradingIsFree_butChangingAScoreNeedsAReason_andEveryChangeIsRecorded() throws Exception {
        grade(7, null).andExpect(status().isOk()).andExpect(jsonPath("$.gradeState").value("OPEN"));
        grade(8, null).andExpect(status().isBadRequest()).andExpect(errorMessageContains("lý do"));
        grade(8, "Cộng điểm phản biện").andExpect(status().isOk()).andExpect(jsonPath("$.scoreRevisionCount").value(1));

        call(HttpMethod.GET, "/api/registration/{id}/grade-history", lecturer.getUser(), null, reg.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].oldScore").value(7.0))
                .andExpect(jsonPath("$[0].newScore").value(8.0))
                .andExpect(jsonPath("$[0].reason").value("Cộng điểm phản biện"));
    }

    @Test
    void closedWorkspace_locksTheScore() throws Exception {
        grade(7, null).andExpect(status().isOk());
        closeWorkspace();
        grade(9, "Muốn sửa").andExpect(status().isBadRequest()).andExpect(errorMessageContains("Điểm đã bị khóa"));
        call(HttpMethod.GET, "/api/registration/topic/{id}", lecturer.getUser(), null, reg.getTopic().getId())
                .andExpect(jsonPath("$[0].gradeState").value("LOCKED"));
    }

    @Test
    void approvedRevision_allowsOneChange_thenLocksAgain() throws Exception {
        grade(6, null).andExpect(status().isOk());
        closeWorkspace();
        long id = requestRevision();
        assertTrue(notificationRepo.findByUser_IdOrderByCreatedAtDesc(deptAdmin.getId()).stream()
                .anyMatch(n -> "GRADE_REVISION".equals(n.getRefType()) && n.getRefId() == id), "quản trị khoa được báo");

        approve(id, deptAdmin).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
        grade(8.5, null).andExpect(status().isBadRequest()).andExpect(errorMessageContains("lý do"));
        grade(8.5, "Sửa lỗi nhập điểm").andExpect(status().isOk()).andExpect(jsonPath("$.score").value(8.5));

        assertEquals(GradeRevisionRequest.USED, revisionRepo.findById(id).orElseThrow().getStatus());
        assertEquals(id, historyRepo.findByRegistration_IdOrderByChangedAtDescIdDesc(reg.getId()).get(0).getRevisionRequest().getId());
        grade(9, "Sửa thêm").andExpect(status().isBadRequest()).andExpect(errorMessageContains("Điểm đã bị khóa"));

        // SV thấy điểm đã được cập nhật, nhưng không thấy yêu cầu chấm lại
        call(HttpMethod.GET, "/api/registration/mine?studentId={id}", student.getUser(), null, student.getId())
                .andExpect(jsonPath("$[0].scoreRevisedAt").isNotEmpty())
                .andExpect(jsonPath("$[0].latestRevision").doesNotExist());
    }

    @Test
    void expiredRevision_keepsTheScoreLocked() throws Exception {
        grade(6, null).andExpect(status().isOk());
        closeWorkspace();
        long id = requestRevision();
        approve(id, deptAdmin).andExpect(status().isOk());

        assertTrue(gradeService.expireOverdue(LocalDateTime.now().plusHours(25)) >= 1);
        assertEquals(GradeRevisionRequest.EXPIRED, revisionRepo.findById(id).orElseThrow().getStatus());
        grade(8, "Quá hạn").andExpect(status().isBadRequest()).andExpect(errorMessageContains("Điểm đã bị khóa"));
    }

    @Test
    void reviewers_areScopedToTheirDepartment() throws Exception {
        grade(6, null).andExpect(status().isOk());
        closeWorkspace();
        long id = requestRevision();

        approve(id, otherDeptAdmin).andExpect(status().isForbidden());
        call(HttpMethod.GET, "/api/grade-revisions?status=PENDING", otherDeptAdmin, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[?(@.id == " + id + ")]").isEmpty());
        call(HttpMethod.GET, "/api/grade-revisions?status=PENDING", deptAdmin, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[?(@.id == " + id + ")]").isNotEmpty());
        call(HttpMethod.GET, "/api/registration/{id}/grade-history", otherDeptAdmin, null, reg.getId())
                .andExpect(status().isForbidden());
        call(HttpMethod.GET, "/api/registration/{id}/grade-history", deptAdmin, null, reg.getId())
                .andExpect(status().isOk());
    }

    @Test
    void onlyOnePendingRequest_andRejectNeedsANote() throws Exception {
        grade(6, null).andExpect(status().isOk());
        closeWorkspace();
        long id = requestRevision();
        call(HttpMethod.POST, "/api/registration/{id}/grade-revisions", lecturer.getUser(), Map.of("reason", "Lần hai"), reg.getId())
                .andExpect(status().isBadRequest()).andExpect(errorMessageContains("đang chờ duyệt"));

        call(HttpMethod.POST, "/api/grade-revisions/{id}/reject", deptAdmin, Map.of(), id)
                .andExpect(status().isBadRequest()).andExpect(errorMessageContains("Lý do từ chối"));
        call(HttpMethod.POST, "/api/grade-revisions/{id}/reject", deptAdmin, Map.of("note", "Không đủ căn cứ"), id)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJECTED"));
        approve(id, deptAdmin).andExpect(status().isBadRequest()).andExpect(errorMessageContains("đã được xử lý"));
        assertTrue(notificationRepo.findByUser_IdOrderByCreatedAtDesc(lecturer.getUser().getId()).stream()
                .anyMatch(n -> n.getTitle().contains("bị từ chối")));
    }

    @Test
    void requestRules_needClosedWorkspaceAScoreAndValidHours() throws Exception {
        call(HttpMethod.POST, "/api/registration/{id}/grade-revisions", lecturer.getUser(), Map.of("reason", "Sớm"), reg.getId())
                .andExpect(status().isBadRequest()).andExpect(errorMessageContains("đợt đã đóng"));
        closeWorkspace();
        call(HttpMethod.POST, "/api/registration/{id}/grade-revisions", lecturer.getUser(), Map.of("reason", "Chưa chấm"), reg.getId())
                .andExpect(status().isBadRequest()).andExpect(errorMessageContains("chưa có điểm"));
        reg.setScore(5.0);
        regRepo.save(reg);
        long id = requestRevision();
        call(HttpMethod.POST, "/api/grade-revisions/{id}/approve", deptAdmin, Map.of("unlockHours", 500), id)
                .andExpect(status().isBadRequest()).andExpect(errorMessageContains("từ 1 đến 168"));
        call(HttpMethod.POST, "/api/registration/{id}/grade-revisions", student.getUser(), Map.of("reason", "x"), reg.getId())
                .andExpect(status().isForbidden());
    }
}
