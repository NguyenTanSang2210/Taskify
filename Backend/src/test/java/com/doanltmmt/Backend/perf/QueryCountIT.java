package com.doanltmmt.Backend.perf;

import com.doanltmmt.Backend.entity.AcademicClass;
import com.doanltmmt.Backend.entity.Department;
import com.doanltmmt.Backend.entity.Lecturer;
import com.doanltmmt.Backend.entity.Notification;
import com.doanltmmt.Backend.entity.ProgressReport;
import com.doanltmmt.Backend.entity.Student;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.entity.Workspace;
import com.doanltmmt.Backend.entity.enums.NotificationType;
import com.doanltmmt.Backend.entity.enums.ProgressStatus;
import com.doanltmmt.Backend.repository.NotificationRepository;
import com.doanltmmt.Backend.repository.ProgressReportRepository;
import com.doanltmmt.Backend.security.AbstractSecurityIT;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.RequestBuilder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * P3-07 — đếm số câu SQL thật (đo trước/sau tối ưu: đề tài 55→7, thông báo 7→4 (có thêm câu đếm của trang), tiến độ 11→7,
 * đăng ký theo đề tài 18→10, đăng ký của tôi 14→8; mỗi request đã gồm 2 câu nạp người dùng để xác thực).
 * Đếm số câu SQL thật (Hibernate statistics) của các màn hình hay dùng, để chặn N+1 quay lại.
 * Dữ liệu được COMMIT (không chạy trong transaction của test, vì cache cấp 1 làm số đếm thấp giả) và xóa ở @AfterEach.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class QueryCountIT extends AbstractSecurityIT {

    @Autowired private EntityManagerFactory emf;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private NotificationRepository notificationRepo;
    @Autowired private ProgressReportRepository progressRepo;

    private Department dept;
    private Workspace workspace;
    private Lecturer lecturer;
    private final List<Student> students = new ArrayList<>();
    private final List<Topic> topics = new ArrayList<>();

    @Autowired private PlatformTransactionManager txManager;

    @BeforeEach
    void seed() {
        new TransactionTemplate(txManager).executeWithoutResult(status -> seedData());
    }

    private void seedData() {
        dept = newDepartment("QC");
        AcademicClass cls = newClass(dept);
        Workspace ws = newWorkspace(dept, "IN_PROGRESS");
        workspace = ws;
        linkClass(ws, cls);
        lecturer = newLecturer("lqc", dept);
        for (int i = 0; i < 6; i++) {
            Lecturer owner = i == 0 ? lecturer : newLecturer("lqc" + i, dept);
            topics.add(newTopic(owner, ws, 5));
        }
        for (int i = 0; i < 6; i++) students.add(newStudent("sqc" + i, dept, cls));
        for (Topic t : topics) {
            for (int i = 0; i < 3; i++) newRegistration(students.get((topics.indexOf(t) + i) % 6), t, i == 0 ? Boolean.TRUE : null);
        }
        for (int i = 0; i < 30; i++) {
            Notification n = new Notification();
            n.setUser(students.get(0).getUser());
            n.setType(NotificationType.INFO);
            n.setTitle("TB " + i);
            notificationRepo.save(n);
        }
        for (int i = 0; i < 10; i++) {
            ProgressReport pr = new ProgressReport();
            pr.setTopic(topics.get(0));
            pr.setStudent(students.get(i % 3));
            pr.setTitle("Tuần " + i);
            pr.setContent("Nội dung báo cáo " + i);
            pr.setStatus(ProgressStatus.TODO);
            progressRepo.save(pr);
        }
    }

    @AfterEach
    void cleanup() {
        Long d = dept.getId();
        String users = "(select id from users where department_id = " + d + ")";
        String ws = "(select id from workspaces where department_id = " + d + ")";
        String tps = "(select id from (select id from topics where workspace_id in " + ws + ") x)";
        jdbc.update("delete from notifications where user_id in " + users);
        jdbc.update("delete from messages where topic_id in " + tps);
        jdbc.update("delete from discussion_posts where thread_id in (select id from (select id from discussion_threads where topic_id in " + tps + ") y)");
        jdbc.update("delete from discussion_threads where topic_id in " + tps);
        jdbc.update("delete from milestones where topic_id in " + tps);
        jdbc.update("delete from calendar_events where topic_id in " + tps);
        jdbc.update("delete from announcements where topic_id in " + tps);
        jdbc.update("delete from lecturer_assignments where workspace_id in " + ws);
        jdbc.update("delete from workspace_classes where workspace_id in " + ws);
        jdbc.update("delete from progress_report where topic_id in " + tps);
        jdbc.update("delete from topic_registration where topic_id in " + tps);
        jdbc.update("delete from topics where workspace_id in " + ws);
        jdbc.update("delete from workspaces where department_id = " + d);
        jdbc.update("delete from students where id in " + users);
        jdbc.update("delete from lecturers where id in " + users);
        jdbc.update("delete from users where department_id = " + d);
        jdbc.update("delete from academic_classes where department_id = " + d);
        jdbc.update("delete from departments where id = " + d);
    }

    private long statements(RequestBuilder request) throws Exception {
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        mvc.perform(request).andExpect(status().isOk());
        return stats.getPrepareStatementCount();
    }

    private void report(String label, long count, long max) {
        System.out.printf("[QUERY-COUNT] %-45s %3d câu SQL (ngưỡng %d)%n", label, count, max);
        assertTrue(count <= max, label + ": " + count + " câu SQL > ngưỡng " + max);
    }

    @Test
    void hotPathsStayWithinQueryBudget() throws Exception {
        User student = students.get(0).getUser();
        report("Danh sách đề tài (6 mục, SV)", statements(get("/api/topics").param("studentId", student.getId().toString())
                .param("size", "6").header(AUTH, bearer(student))), 8);
        report("Thông báo của tôi (30)", statements(get("/api/notifications/mine").param("userId", student.getId().toString())
                .header(AUTH, bearer(student))), 5);
        report("Tiến độ theo đề tài (10 báo cáo, GV)", statements(get("/api/progress/topic/{id}", topics.get(0).getId())
                .header(AUTH, bearer(lecturer.getUser()))), 8);
        report("Đăng ký theo đề tài (3, GV)", statements(get("/api/registration/topic/{id}", topics.get(0).getId())
                .header(AUTH, bearer(lecturer.getUser()))), 11);
        report("Đăng ký của tôi (SV)", statements(get("/api/registration/mine").param("studentId", student.getId().toString())
                .header(AUTH, bearer(student))), 9);
    }

    /**
     * Mọi API GET trả dữ liệu thật NGOÀI transaction của test (như server thật): bắt LazyInitializationException mà các IT
     * thường (chạy trong một transaction, dùng chung session) không thấy được.
     */
    @Test
    void readEndpointsWorkWithoutTheTestTransaction() throws Exception {
        User student = students.get(0).getUser();
        User lec = lecturer.getUser();
        User admin = newUser("aqc", "ADMIN", dept);
        User deptAdmin = newUser("dqc", "DEPARTMENT_ADMIN", dept);
        Long topicId = topics.get(0).getId();
        String json = "application/json";

        String thread = mvc.perform(post("/api/discuss/threads").header(AUTH, bearer(student)).contentType(json)
                .content("{\"topicId\":" + topicId + ",\"title\":\"Hỏi\"}")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long threadId = com.jayway.jsonpath.JsonPath.<Number>read(thread, "$.id").longValue();
        mvc.perform(post("/api/discuss/threads/{id}/posts", threadId).header(AUTH, bearer(lec)).contentType(json)
                .content("{\"content\":\"Trả lời\"}")).andExpect(status().isOk());
        String milestone = mvc.perform(post("/api/milestones").header(AUTH, bearer(lec)).contentType(json)
                .content("{\"topicId\":" + topicId + ",\"title\":\"M1\",\"deadline\":\"2030-12-31\"}")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long milestoneId = com.jayway.jsonpath.JsonPath.<Number>read(milestone, "$.id").longValue();
        mvc.perform(post("/api/calendar").header(AUTH, bearer(lec)).contentType(json)
                .content("{\"topicId\":" + topicId + ",\"title\":\"Họp\"}")).andExpect(status().isOk());
        mvc.perform(post("/api/announcements").header(AUTH, bearer(lec)).contentType(json)
                .content("{\"topicId\":" + topicId + ",\"title\":\"TB\",\"content\":\"Nội dung\"}")).andExpect(status().isOk());
        mvc.perform(post("/api/messages/send").header(AUTH, bearer(student)).contentType(json)
                .content("{\"recipientId\":" + lec.getId() + ",\"topicId\":" + topicId + ",\"content\":\"Chào thầy\"}"))
                .andExpect(status().isOk());

        Object[][] calls = {
                {admin, "/api/users"}, {admin, "/api/users?q=qc"}, {admin, "/api/users/" + student.getId()},
                {admin, "/api/users/roles"}, {admin, "/api/roles"}, {admin, "/api/roles/privileges"},
                {admin, "/api/audit"}, {admin, "/api/audit/recent"}, {admin, "/api/departments"},
                {admin, "/api/dashboard/admin"}, {admin, "/api/workspaces"},
                {deptAdmin, "/api/users/by-department"}, {deptAdmin, "/api/users/by-department?role=LECTURER"},
                {deptAdmin, "/api/classes"}, {deptAdmin, "/api/workspaces/" + workspace.getId()},
                {deptAdmin, "/api/assignments?workspaceId=" + workspace.getId()},
                {deptAdmin, "/api/workspace-classes?workspaceId=" + workspace.getId()},
                {deptAdmin, "/api/grade-revisions"}, {deptAdmin, "/api/dashboard/department-admin"},
                {deptAdmin, "/api/progress/topic/" + topicId}, {deptAdmin, "/api/topics"},
                {lec, "/api/dashboard/lecturer"}, {lec, "/api/workspaces/open-for-topics"},
                {lec, "/api/registration/topic/" + topicId}, {lec, "/api/progress/student/" + student.getId() + "?topicId=" + topicId},
                {lec, "/api/messages/inbox"}, {lec, "/api/messages/conversation/" + student.getId() + "?topicId=" + topicId},
                {lec, "/api/export/excel?topicId=" + topicId},
                {student, "/api/discuss/threads?topicId=" + topicId}, {student, "/api/discuss/threads/" + threadId + "/posts"},
                {student, "/api/milestones?topicId=" + topicId}, {student, "/api/milestones/" + milestoneId},
                {student, "/api/calendar?topicId=" + topicId}, {student, "/api/announcements?topicId=" + topicId},
                {student, "/api/announcements"}, {student, "/api/notifications/mine?userId=" + student.getId()},
                {student, "/api/notifications/recent?userId=" + student.getId()},
                {student, "/api/notifications/count?userId=" + student.getId()},
                {student, "/api/registration/mine?studentId=" + student.getId()},
                {student, "/api/progress/student/" + student.getId()}, {student, "/api/topics?studentId=" + student.getId()},
                {student, "/api/users/" + lec.getId()},
        };
        for (Object[] c : calls) {
            User as = (User) c[0];
            String url = (String) c[1];
            int code = mvc.perform(get(url).header(AUTH, bearer(as))).andReturn().getResponse().getStatus();
            assertEquals(200, code, "GET " + url + " (" + as.getUsername() + ")");
        }
    }
}
