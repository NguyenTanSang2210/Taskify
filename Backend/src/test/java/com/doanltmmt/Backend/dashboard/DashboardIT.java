package com.doanltmmt.Backend.dashboard;

import com.doanltmmt.Backend.entity.AcademicClass;
import com.doanltmmt.Backend.entity.Department;
import com.doanltmmt.Backend.entity.Lecturer;
import com.doanltmmt.Backend.entity.Milestone;
import com.doanltmmt.Backend.entity.ProgressReport;
import com.doanltmmt.Backend.entity.Student;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.TopicRegistration;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.entity.Workspace;
import com.doanltmmt.Backend.entity.enums.ProgressStatus;
import com.doanltmmt.Backend.repository.MilestoneRepository;
import com.doanltmmt.Backend.repository.ProgressReportRepository;
import com.doanltmmt.Backend.security.AbstractSecurityIT;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P4-01 — dashboard: thống kê tổng hợp theo khoa/đợt (ADMIN, quản trị khoa), việc cần làm của giảng viên,
 * tiến độ của sinh viên, xuất Excel, phạm vi truy cập.
 */
@SuppressWarnings("null")
class DashboardIT extends AbstractSecurityIT {

    @Autowired private ProgressReportRepository progressRepo;
    @Autowired private MilestoneRepository milestoneRepo;

    private Department dept;
    private Workspace ws;
    private Topic topic;
    private Lecturer lecturer;
    private User deptAdmin;
    private Student graded, approvedSilent, pending;
    private ProgressReport report;

    @BeforeEach
    void setUp() {
        dept = newDepartment("DB");
        AcademicClass cls = newClass(dept);
        ws = newWorkspace(dept, "IN_PROGRESS");
        linkClass(ws, cls);
        lecturer = newLecturer("gvdb", dept);
        topic = newTopic(lecturer, ws, 3);
        deptAdmin = newUser("qtk", "DEPARTMENT_ADMIN", dept);

        graded = newStudent("sva", dept, cls);
        approvedSilent = newStudent("svb", dept, cls);
        pending = newStudent("svc", dept, cls);
        Student rejected = newStudent("svd", dept, cls);
        TopicRegistration ra = newRegistration(graded, topic, true);
        ra.setScore(8.5);
        regRepo.save(ra);
        newRegistration(approvedSilent, topic, true);
        newRegistration(pending, topic, null);
        newRegistration(rejected, topic, false);

        report = new ProgressReport();
        report.setTopic(topic);
        report.setStudent(graded);
        report.setTitle("Tuần 1");
        report.setContent("Đã xong phân tích");
        report.setStatus(ProgressStatus.DONE);
        report.setCreatedAt(LocalDateTime.now().minusHours(5));
        report = progressRepo.save(report);

        Milestone m = new Milestone();
        m.setTopic(topic);
        m.setTitle("Nộp đề cương");
        m.setDeadline(LocalDateTime.now().plusDays(3));
        m.setCreatedAt(LocalDateTime.now());
        milestoneRepo.save(m);
    }

    @Test
    void lecturerComment_recordsFirstResponseTime_andOverviewAggregatesTheDepartment() throws Exception {
        mvc.perform(put("/api/progress/{id}/comment", report.getId()).header(AUTH, bearer(lecturer.getUser()))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("lecturerComment", "Tốt"))))
                .andExpect(status().isOk());
        LocalDateTime first = progressRepo.findById(report.getId()).orElseThrow().getCommentedAt();
        assertNotNull(first, "lần nhận xét đầu tiên được ghi thời điểm");
        mvc.perform(put("/api/progress/{id}/comment", report.getId()).header(AUTH, bearer(lecturer.getUser()))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("lecturerComment", "Sửa lại")))).andExpect(status().isOk());
        assertEquals(first, progressRepo.findById(report.getId()).orElseThrow().getCommentedAt(), "nhận xét sau không đổi mốc phản hồi");

        mvc.perform(get("/api/dashboard/overview").header(AUTH, bearer(deptAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departmentId").value(dept.getId()))
                .andExpect(jsonPath("$.departments", hasSize(0)))
                .andExpect(jsonPath("$.topicsByStatus.OPEN").value(1))
                .andExpect(jsonPath("$.students.total").value(4))
                .andExpect(jsonPath("$.students.withTopic").value(2))
                .andExpect(jsonPath("$.students.rate").value(50.0))
                .andExpect(jsonPath("$.registrations.pending").value(1))
                .andExpect(jsonPath("$.registrations.approved").value(2))
                .andExpect(jsonPath("$.registrations.rejected").value(1))
                .andExpect(jsonPath("$.registrations.graded").value(1))
                .andExpect(jsonPath("$.registrations.completionRate").value(50.0))
                .andExpect(jsonPath("$.scores.average").value(8.5))
                .andExpect(jsonPath("$.scores.histogram", hasSize(10)))
                .andExpect(jsonPath("$.scores.histogram[8].label").value("8–9"))
                .andExpect(jsonPath("$.scores.histogram[8].count").value(1))
                .andExpect(jsonPath("$.lecturerLoad[0].name").value(lecturer.getUser().getFullName()))
                .andExpect(jsonPath("$.lecturerLoad[0].topics").value(1))
                .andExpect(jsonPath("$.lecturerLoad[0].approvedStudents").value(2))
                .andExpect(jsonPath("$.feedback.measured").value(1))
                .andExpect(jsonPath("$.feedback.avgHours").value(5.0))
                .andExpect(jsonPath("$.feedback.uncommented").value(0));

        // lọc theo đợt: số SV tính theo các lớp đã gán vào đợt
        mvc.perform(get("/api/dashboard/overview").param("workspaceId", ws.getId().toString()).header(AUTH, bearer(deptAdmin)))
                .andExpect(jsonPath("$.workspaceId").value(ws.getId()))
                .andExpect(jsonPath("$.students.total").value(4))
                .andExpect(jsonPath("$.registrations.approved").value(2));
        Workspace empty = newWorkspace(dept, "DRAFT");
        mvc.perform(get("/api/dashboard/overview").param("workspaceId", empty.getId().toString()).header(AUTH, bearer(deptAdmin)))
                .andExpect(jsonPath("$.students.total").value(0))
                .andExpect(jsonPath("$.students.rate").doesNotExist())
                .andExpect(jsonPath("$.registrations.approved").value(0))
                .andExpect(jsonPath("$.scores.average").doesNotExist());
    }

    @Test
    void scope_departmentAdminSeesOnlyOwnDepartment_adminChooses() throws Exception {
        Department other = newDepartment("DB2");
        Workspace otherWs = newWorkspace(other, "DRAFT");
        mvc.perform(get("/api/dashboard/overview").param("departmentId", other.getId().toString()).header(AUTH, bearer(deptAdmin)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/dashboard/overview").param("workspaceId", otherWs.getId().toString()).header(AUTH, bearer(deptAdmin)))
                .andExpect(status().isForbidden());

        User admin = newUser("adm", "ADMIN", null);
        mvc.perform(get("/api/dashboard/overview").param("departmentId", dept.getId().toString()).header(AUTH, bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.registrations.approved").value(2))
                .andExpect(jsonPath("$.departmentName").value(dept.getName()));
        mvc.perform(get("/api/dashboard/overview").header(AUTH, bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.departmentId").doesNotExist())
                .andExpect(jsonPath("$.departments[?(@.id == " + dept.getId() + ")]").exists());

        for (User u : List.of(lecturer.getUser(), graded.getUser())) {
            mvc.perform(get("/api/dashboard/overview").header(AUTH, bearer(u))).andExpect(status().isForbidden());
            mvc.perform(get("/api/dashboard/overview/export").header(AUTH, bearer(u))).andExpect(status().isForbidden());
        }
    }

    @Test
    void export_isAWorkbookWithSummaryHistogramAndLecturers() throws Exception {
        byte[] body = mvc.perform(get("/api/dashboard/overview/export").header(AUTH, bearer(deptAdmin)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(body))) {
            assertEquals(List.of("Tổng quan", "Phân bố điểm", "Giảng viên"),
                    List.of(wb.getSheetName(0), wb.getSheetName(1), wb.getSheetName(2)));
            assertEquals("Khoa " + dept.getName(), wb.getSheetAt(0).getRow(1).getCell(1).getStringCellValue());
            assertEquals(lecturer.getUser().getFullName(), wb.getSheetAt(2).getRow(1).getCell(0).getStringCellValue());
            assertEquals(2.0, wb.getSheetAt(2).getRow(1).getCell(2).getNumericCellValue());
        }
    }

    @Test
    void lecturer_seesPendingWork_silentStudents_andUpcomingMilestones() throws Exception {
        mvc.perform(get("/api/dashboard/lecturer").header(AUTH, bearer(lecturer.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myTopics").value(1))
                .andExpect(jsonPath("$.approvedStudents").value(2))
                .andExpect(jsonPath("$.pendingRegistrations").value(1))
                .andExpect(jsonPath("$.uncommentedReports").value(1))
                .andExpect(jsonPath("$.silentStudents", hasSize(1)))
                .andExpect(jsonPath("$.silentStudents[0].studentCode").value(approvedSilent.getStudentCode()))
                .andExpect(jsonPath("$.silentStudents[0].lastReportAt").doesNotExist())
                .andExpect(jsonPath("$.upcomingMilestones[0].title").value("Nộp đề cương"))
                .andExpect(jsonPath("$.upcomingMilestones[0].topicId").value(topic.getId()));
    }

    @Test
    void student_seesTopicProgressScoreAndMilestones() throws Exception {
        mvc.perform(get("/api/dashboard/student").header(AUTH, bearer(graded.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registration.status").value("APPROVED"))
                .andExpect(jsonPath("$.registration.topicId").value(topic.getId()))
                .andExpect(jsonPath("$.registration.lecturerName").value(lecturer.getUser().getFullName()))
                .andExpect(jsonPath("$.registration.score").value(8.5))
                .andExpect(jsonPath("$.progress.total").value(1))
                .andExpect(jsonPath("$.progress.done").value(1))
                .andExpect(jsonPath("$.progress.percent").value(100.0))
                .andExpect(jsonPath("$.upcomingMilestones[0].title").value("Nộp đề cương"));
        mvc.perform(get("/api/dashboard/student").header(AUTH, bearer(pending.getUser())))
                .andExpect(jsonPath("$.registration.status").value("PENDING"))
                .andExpect(jsonPath("$.progress.percent").doesNotExist())
                .andExpect(jsonPath("$.upcomingMilestones", hasSize(0)));
        Student none = newStudent("sve", dept, null);
        mvc.perform(get("/api/dashboard/student").header(AUTH, bearer(none.getUser())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.registration").doesNotExist());
        mvc.perform(get("/api/dashboard/student").header(AUTH, bearer(lecturer.getUser()))).andExpect(status().isForbidden());
    }
}
