package com.doanltmmt.Backend.security;

import com.doanltmmt.Backend.config.DataMigrationRunner;
import com.doanltmmt.Backend.entity.Department;
import com.doanltmmt.Backend.entity.Lecturer;
import com.doanltmmt.Backend.entity.Privilege;
import com.doanltmmt.Backend.entity.Role;
import com.doanltmmt.Backend.entity.Student;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.entity.Workspace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P2-01 — giảng viên tự tạo/sửa/đóng/mở đề tài của mình (quyền TOPIC_MANAGE), không đụng được đề tài của người khác;
 * quyền do quản trị viên chỉnh không bị ghi đè khi khởi động lại.
 */
@SuppressWarnings("null")
class TopicManagementIT extends AbstractSecurityIT {

    @Autowired
    @Qualifier("initRolesAndPrivileges")
    private CommandLineRunner roleInitializer;

    @Autowired
    private DataMigrationRunner dataMigrations;

    private Lecturer lecA;
    private Lecturer lecA2;
    private Workspace wsOpenTopic;
    private Student student;

    @BeforeEach
    void setUp() {
        Department dept = newDepartment("TM");
        lecA = newLecturer("lta", dept);
        lecA2 = newLecturer("ltb", dept);
        wsOpenTopic = newWorkspace(dept, "OPEN_TOPIC");
        student = newStudent("stm", dept, newClass(dept));
    }

    private ResultActions call(HttpMethod method, String url, User as, Object body, Object... vars) throws Exception {
        MockHttpServletRequestBuilder b = request(method, url, vars).header(AUTH, bearer(as));
        if (body != null) b.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        return mvc.perform(b);
    }

    private Map<String, Object> topicBody(String title) {
        return Map.of("title", title, "description", "Mô tả", "capacity", 2, "workspace", Map.of("id", wsOpenTopic.getId()));
    }

    @Test
    void lecturerRole_hasTopicManage() {
        Role lecturer = roleRepo.findByName("LECTURER").orElseThrow();
        assertTrue(lecturer.getPrivileges().stream().anyMatch(p -> p.getName().equals("TOPIC_MANAGE")));
    }

    @Test
    void lecturer_createsOwnTopic_inOpenTopicWorkspace() throws Exception {
        call(HttpMethod.POST, "/api/topics/create?lecturerId={id}", lecA.getUser(), topicBody("Đề tài của tôi"), lecA.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Đề tài của tôi"))
                .andExpect(jsonPath("$.lecturerId").value(lecA.getId()));
    }

    @Test
    void lecturer_cannotCreateTopicInSomeoneElsesName() throws Exception {
        call(HttpMethod.POST, "/api/topics/create?lecturerId={id}", lecA.getUser(), topicBody("Mạo danh"), lecA2.getId())
                .andExpect(status().isForbidden());
    }

    @Test
    void lecturer_updatesOpensAndClosesOwnTopic() throws Exception {
        Topic t = newTopic(lecA, wsOpenTopic, 2);
        call(HttpMethod.PUT, "/api/topics/{id}?lecturerId={l}", lecA.getUser(), Map.of("title", "Tên mới"), t.getId(), lecA.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Tên mới"));
        call(HttpMethod.POST, "/api/topics/{id}/close?lecturerId={l}", lecA.getUser(), null, t.getId(), lecA.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
        call(HttpMethod.POST, "/api/topics/{id}/open?lecturerId={l}", lecA.getUser(), null, t.getId(), lecA.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void lecturer_cannotModifyAnotherLecturersTopic() throws Exception {
        Topic t = newTopic(lecA2, wsOpenTopic, 2);
        call(HttpMethod.PUT, "/api/topics/{id}?lecturerId={l}", lecA.getUser(), Map.of("title", "Chiếm"), t.getId(), lecA.getId())
                .andExpect(status().isForbidden());
        call(HttpMethod.POST, "/api/topics/{id}/close?lecturerId={l}", lecA.getUser(), null, t.getId(), lecA.getId())
                .andExpect(status().isForbidden());
        call(HttpMethod.DELETE, "/api/topics/{id}?lecturerId={l}", lecA.getUser(), null, t.getId(), lecA.getId())
                .andExpect(status().isForbidden());
    }

    @Test
    void student_cannotCreateTopic() throws Exception {
        call(HttpMethod.POST, "/api/topics/create?lecturerId={id}", student.getUser(), topicBody("SV tạo"), student.getId())
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_cannotAssignTopicToNonLecturerAccount() throws Exception {
        User admin = newUser("adm", "ADMIN", null);
        call(HttpMethod.POST, "/api/topics/create?lecturerId={id}", admin, topicBody("Gán nhầm"), student.getUser().getId())
                .andExpect(status().isBadRequest())
                .andExpect(errorMessageContains("giảng viên"));
        assertFalse(lecturerRepo.existsById(student.getUser().getId()), "không được tạo bản ghi giảng viên cho sinh viên");
    }

    @Test
    void privilegesEditedByAdmin_surviveRestart() throws Exception {
        Role lecturer = roleRepo.findByName("LECTURER").orElseThrow();
        lecturer.getPrivileges().removeIf(p -> p.getName().equals("TOPIC_MANAGE"));
        roleRepo.saveAndFlush(lecturer);

        roleInitializer.run(); // giả lập khởi động lại
        dataMigrations.run();

        Role after = roleRepo.findByName("LECTURER").orElseThrow();
        assertFalse(after.getPrivileges().stream().map(Privilege::getName).anyMatch("TOPIC_MANAGE"::equals),
                "khởi động lại không được ghi đè quyền quản trị viên đã chỉnh");
        assertEquals(1, after.getPrivileges().size());
    }
}
