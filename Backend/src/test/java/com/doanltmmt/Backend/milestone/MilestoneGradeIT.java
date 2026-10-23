package com.doanltmmt.Backend.milestone;

import com.doanltmmt.Backend.entity.AcademicClass;
import com.doanltmmt.Backend.entity.Department;
import com.doanltmmt.Backend.entity.Lecturer;
import com.doanltmmt.Backend.entity.Milestone;
import com.doanltmmt.Backend.entity.ProgressReport;
import com.doanltmmt.Backend.entity.Student;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.Workspace;
import com.doanltmmt.Backend.entity.enums.ProgressStatus;
import com.doanltmmt.Backend.entity.enums.WorkspaceStatus;
import com.doanltmmt.Backend.repository.MilestoneRepository;
import com.doanltmmt.Backend.repository.ProgressReportRepository;
import com.doanltmmt.Backend.security.AbstractSecurityIT;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** P4-05 — mốc có trọng số, bảng nộp bài (đúng/trễ hạn), chấm theo mốc, điểm đề xuất có trọng số, lịch gồm mốc. */
@SuppressWarnings("null")
class MilestoneGradeIT extends AbstractSecurityIT {

    @Autowired private MilestoneRepository milestoneRepo;
    @Autowired private ProgressReportRepository progressRepo;

    private Workspace ws;
    private Topic topic;
    private Lecturer lecturer;
    private Student a, b;
    private Milestone m1, m2;

    @BeforeEach
    void setUp() {
        Department dept = newDepartment("MG");
        AcademicClass cls = newClass(dept);
        ws = newWorkspace(dept, "IN_PROGRESS");
        lecturer = newLecturer("gvmg", dept);
        topic = newTopic(lecturer, ws, 3);
        a = newStudent("sva", dept, cls);
        b = newStudent("svb", dept, cls);
        newRegistration(a, topic, true);
        newRegistration(b, topic, true);
        m1 = milestone("Đề cương", LocalDateTime.now().minusDays(2), 40);
        m2 = milestone("Báo cáo cuối", LocalDateTime.now().plusDays(10), 60);
        submit(a, m1, LocalDateTime.now().minusDays(3)); // đúng hạn
        submit(b, m1, LocalDateTime.now().minusDays(1)); // trễ 1 ngày
    }

    private Milestone milestone(String title, LocalDateTime deadline, Integer weight) {
        Milestone m = new Milestone();
        m.setTopic(topic);
        m.setTitle(title);
        m.setDeadline(deadline);
        m.setWeight(weight);
        m.setCreatedAt(LocalDateTime.now());
        return milestoneRepo.save(m);
    }

    private void submit(Student s, Milestone m, LocalDateTime at) {
        ProgressReport p = new ProgressReport();
        p.setTopic(topic);
        p.setStudent(s);
        p.setMilestone(m);
        p.setTitle("Nộp " + m.getTitle());
        p.setContent("Nội dung");
        p.setStatus(ProgressStatus.DONE);
        p.setCreatedAt(at);
        progressRepo.save(p);
    }

    private ResultActions grade(Milestone m, Student s, Object score) throws Exception {
        return mvc.perform(put("/api/milestones/{id}/grades/{sid}", m.getId(), s.getId()).header(AUTH, bearer(lecturer.getUser()))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("score", score, "feedback", "Ổn"))));
    }

    private JsonNode board(com.doanltmmt.Backend.entity.User as) throws Exception {
        return json.readTree(mvc.perform(get("/api/milestones/board").param("topicId", topic.getId().toString()).header(AUTH, bearer(as)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    @Test
    void board_showsOnTimeAndLateSubmissions_andWeightedSuggestion() throws Exception {
        grade(m1, a, 8).andExpect(status().isOk()).andExpect(jsonPath("$.score").value(8.0));
        grade(m2, a, 9.5).andExpect(status().isOk());
        grade(m1, b, 6).andExpect(status().isOk());

        JsonNode board = board(lecturer.getUser());
        assertEquals(100, board.get("weightTotal").asInt());
        assertEquals(true, board.get("canGrade").asBoolean());
        assertEquals(2, board.get("milestones").size());
        assertEquals(40, board.get("milestones").get(0).get("weight").asInt());
        JsonNode rowA = board.get("students").get(0).get("studentId").asLong() == a.getId() ? board.get("students").get(0) : board.get("students").get(1);
        JsonNode rowB = rowA == board.get("students").get(0) ? board.get("students").get(1) : board.get("students").get(0);
        assertEquals(false, rowA.get("cells").get(0).get("late").asBoolean());
        assertEquals(true, rowB.get("cells").get(0).get("late").asBoolean(), "nộp sau hạn chót = trễ");
        assertEquals(true, rowB.get("cells").get(1).get("submittedAt").isNull(), "chưa nộp mốc 2");
        assertEquals(8 * 0.4 + 9.5 * 0.6, rowA.get("weightedScore").asDouble(), 0.001);
        assertEquals(true, rowA.get("complete").asBoolean());
        assertEquals(6.0, rowB.get("weightedScore").asDouble(), 0.001, "chỉ tính các mốc có trọng số đã chấm");
        assertEquals(false, rowB.get("complete").asBoolean());

        // chấm lại: ghi đè, không tạo bản ghi mới
        grade(m1, b, 7).andExpect(status().isOk());
        assertEquals(7.0, rowScore(board(lecturer.getUser()), b, 0), 0.001);
    }

    private double rowScore(JsonNode board, Student s, int cell) {
        for (JsonNode r : board.get("students")) {
            if (r.get("studentId").asLong() == s.getId()) return r.get("cells").get(cell).get("score").asDouble();
        }
        throw new AssertionError();
    }

    @Test
    void student_seesOnlyOwnRow_andCannotGrade() throws Exception {
        grade(m1, a, 8).andExpect(status().isOk());
        JsonNode board = board(b.getUser());
        assertEquals(1, board.get("students").size());
        assertEquals(b.getId(), board.get("students").get(0).get("studentId").asLong());
        assertEquals(false, board.get("canGrade").asBoolean());
        mvc.perform(put("/api/milestones/{id}/grades/{sid}", m1.getId(), b.getId()).header(AUTH, bearer(b.getUser()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"score\":10}")).andExpect(status().isForbidden());

        Student outsider = newStudent("ngoai", newDepartment("MG2"), null);
        mvc.perform(get("/api/milestones/board").param("topicId", topic.getId().toString()).header(AUTH, bearer(outsider.getUser())))
                .andExpect(status().isForbidden());
        Lecturer other = newLecturer("gvkhac", newDepartment("MG3"));
        mvc.perform(put("/api/milestones/{id}/grades/{sid}", m1.getId(), a.getId()).header(AUTH, bearer(other.getUser()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"score\":10}")).andExpect(status().isForbidden());
    }

    @Test
    void grading_isValidated_andOnlyWhileTheWorkspaceIsInProgress() throws Exception {
        grade(m1, a, 11).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        Student notMember = newStudent("svx", ws.getDepartment(), null);
        grade(m1, notMember, 5).andExpect(status().isBadRequest()).andExpect(errorMessageContains("không thuộc đề tài"));

        ws.setStatus(WorkspaceStatus.CLOSED);
        workspaceRepo.save(ws);
        grade(m1, a, 8).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("WORKSPACE_PHASE"));
        assertEquals(false, board(lecturer.getUser()).get("canGrade").asBoolean());
    }

    @Test
    void milestoneWeight_isValidated_andMilestonesWithSubmissionsCannotBeDeleted() throws Exception {
        mvc.perform(post("/api/milestones").header(AUTH, bearer(lecturer.getUser())).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("topicId", topic.getId(), "title", "Demo", "deadline",
                                LocalDateTime.now().plusDays(5).withNano(0).toString(), "weight", 120))))
                .andExpect(status().isBadRequest()).andExpect(errorMessageContains("Trọng số từ 0 đến 100"));
        mvc.perform(post("/api/milestones").header(AUTH, bearer(lecturer.getUser())).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("topicId", topic.getId(), "title", "Demo", "deadline",
                                LocalDateTime.now().plusDays(5).withNano(0).toString(), "weight", 20))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.weight").value(20));

        mvc.perform(delete("/api/milestones/{id}", m1.getId()).header(AUTH, bearer(lecturer.getUser())))
                .andExpect(status().isBadRequest()).andExpect(errorMessageContains("đã có bài nộp"));
        mvc.perform(delete("/api/milestones/{id}", m2.getId()).header(AUTH, bearer(lecturer.getUser()))).andExpect(status().isOk());
    }

    @Test
    void calendar_includesMilestonesInChronologicalOrder() throws Exception {
        mvc.perform(get("/api/calendar").param("topicId", topic.getId().toString()).header(AUTH, bearer(a.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].type").value("MILESTONE"))
                .andExpect(jsonPath("$[0].milestoneId").value(m1.getId()))
                .andExpect(jsonPath("$[1].title").value("Báo cáo cuối"));
    }
}
