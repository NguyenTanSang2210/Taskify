package com.doanltmmt.Backend.paging;

import com.doanltmmt.Backend.entity.AcademicClass;
import com.doanltmmt.Backend.entity.Department;
import com.doanltmmt.Backend.entity.Lecturer;
import com.doanltmmt.Backend.entity.Notification;
import com.doanltmmt.Backend.entity.Student;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.entity.enums.NotificationType;
import com.doanltmmt.Backend.repository.NotificationRepository;
import com.doanltmmt.Backend.security.AbstractSecurityIT;
import com.doanltmmt.Backend.service.AuditLogService;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

/** P3-06 — API danh sách trả {items, page, size, totalElements, totalPages}, không bao giờ tải cả bảng. */
class PaginationIT extends AbstractSecurityIT {

    @Autowired private NotificationRepository notificationRepo;
    @Autowired private AuditLogService auditLogService;

    private Department dept;
    private User admin;
    private Student student;
    private Lecturer lecturer;
    private Topic topic;

    @BeforeEach
    void setUp() {
        dept = newDepartment("PG");
        AcademicClass cls = newClass(dept);
        admin = newUser("apg", "ADMIN", null);
        lecturer = newLecturer("lpg", dept);
        student = newStudent("spg", dept, cls);
        topic = newTopic(lecturer, newWorkspace(dept, "IN_PROGRESS"), 3);
        newRegistration(student, topic, true);
    }

    @Test
    void notifications_arePaged_andSizeIsCappedAt100() throws Exception {
        for (int i = 0; i < 105; i++) {
            Notification n = new Notification();
            n.setUser(student.getUser());
            n.setType(NotificationType.INFO);
            n.setTitle("TB " + i);
            notificationRepo.save(n);
        }
        String uid = student.getUser().getId().toString();
        mvc.perform(get("/api/notifications/mine").param("userId", uid).header(AUTH, bearer(student.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(20)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(105))
                .andExpect(jsonPath("$.totalPages").value(6));
        mvc.perform(get("/api/notifications/mine").param("userId", uid).param("size", "1000").header(AUTH, bearer(student.getUser())))
                .andExpect(jsonPath("$.size").value(100))
                .andExpect(jsonPath("$.items", hasSize(100)));
        mvc.perform(get("/api/notifications/mine").param("userId", uid).param("page", "-3").header(AUTH, bearer(student.getUser())))
                .andExpect(jsonPath("$.page").value(0));
        mvc.perform(get("/api/notifications/recent").param("userId", uid).param("limit", "7").header(AUTH, bearer(student.getUser())))
                .andExpect(jsonPath("$", hasSize(7)));
    }

    @Test
    void users_canBeSearched_andPaged() throws Exception {
        newUser("timkiemxyz", "STUDENT", dept);
        mvc.perform(get("/api/users").param("q", "TIMKIEMXYZ").header(AUTH, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].username").value(org.hamcrest.Matchers.startsWith("timkiemxyz")));
        User deptAdmin = newUser("dpg", "DEPARTMENT_ADMIN", dept);
        mvc.perform(get("/api/users/by-department").param("role", "LECTURER").header(AUTH, bearer(deptAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].role.name").value(everyItem(is("LECTURER"))));
    }

    @Test
    void audit_isFilterable_andAdminOnly() throws Exception {
        auditLogService.log("PG_TEST_ACTION", "Test", "1", "x");
        mvc.perform(get("/api/audit").param("action", "PG_TEST_ACTION").param("size", "5").header(AUTH, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.items[*].action").value(everyItem(is("PG_TEST_ACTION"))));
        mvc.perform(get("/api/audit").header(AUTH, bearer(student.getUser()))).andExpect(status().isForbidden());
    }

    @Test
    void conversation_isPaged_andLimitedToTopicMembers() throws Exception {
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/messages/send").header(AUTH, bearer(student.getUser())).contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(Map.of("recipientId", lecturer.getId(), "topicId", topic.getId(), "content", "Tin " + i))))
                    .andExpect(status().isOk());
        }
        mvc.perform(get("/api/messages/conversation/{peer}", lecturer.getId()).param("topicId", topic.getId().toString())
                        .param("size", "2").header(AUTH, bearer(student.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].content").value("Tin 2")); // mới nhất trước
        Student outsider = newStudent("opg", dept, newClass(dept));
        mvc.perform(get("/api/messages/conversation/{peer}", lecturer.getId()).param("topicId", topic.getId().toString())
                        .header(AUTH, bearer(outsider.getUser())))
                .andExpect(status().isForbidden());
    }
}
