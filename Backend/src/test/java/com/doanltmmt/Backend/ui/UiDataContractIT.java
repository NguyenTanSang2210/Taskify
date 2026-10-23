package com.doanltmmt.Backend.ui;

import com.doanltmmt.Backend.entity.AcademicClass;
import com.doanltmmt.Backend.entity.Department;
import com.doanltmmt.Backend.entity.Lecturer;
import com.doanltmmt.Backend.entity.Student;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.entity.Workspace;
import com.doanltmmt.Backend.security.AbstractSecurityIT;
import static org.hamcrest.Matchers.nullValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

/**
 * Các trường mà giao diện đọc phải có trong phản hồi — trước đây nhiều trang đọc trường không tồn tại nên hiện trống
 * (tên GV ở tab duyệt đề tài, đợt của đề tài, đề tài của tin nhắn, số liệu thống kê khoa).
 */
@SuppressWarnings("null")
class UiDataContractIT extends AbstractSecurityIT {

    private Department dept;
    private Lecturer lecturer;
    private Workspace ws;
    private Topic topic;
    private Student student;
    private User deptAdmin;

    @BeforeEach
    void setUp() {
        dept = newDepartment("UIC");
        AcademicClass cls = newClass(dept);
        lecturer = newLecturer("luic", dept);
        deptAdmin = newUser("duic", "DEPARTMENT_ADMIN", dept);
        ws = newWorkspace(dept, "IN_PROGRESS");
        topic = newTopic(lecturer, ws, 4);
        student = newStudent("suic", dept, cls);
        newRegistration(student, topic, true);
    }

    @Test
    void topicList_carriesLecturerAndWorkspaceNames() throws Exception {
        mvc.perform(get("/api/topics").param("lecturerId", lecturer.getId().toString()).header(AUTH, bearer(deptAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].lecturerName").value(lecturer.getUser().getFullName()))
                .andExpect(jsonPath("$.items[0].workspaceId").value(ws.getId()))
                .andExpect(jsonPath("$.items[0].workspaceName").value(ws.getName()));
    }

    @Test
    void messages_carryTheirTopic_inSendAndInbox() throws Exception {
        mvc.perform(post("/api/messages/send").header(AUTH, bearer(student.getUser())).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("recipientId", lecturer.getId().toString(),
                                "topicId", topic.getId().toString(), "content", "Chào thầy"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topic.id").value(topic.getId()));
        mvc.perform(get("/api/messages/inbox").header(AUTH, bearer(lecturer.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].topic.id").value(topic.getId()));
    }

    @Test
    void departmentStats_areComputed_notHardCoded() throws Exception {
        // 4 chỗ, 1 SV đã duyệt → 25%; GV chưa được phân công vào đợt nào → 1
        mvc.perform(get("/api/dashboard/department-admin").header(AUTH, bearer(deptAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capacityTotal").value(4))
                .andExpect(jsonPath("$.approvedTotal").value(1))
                .andExpect(jsonPath("$.fillRate").value(25.0))
                .andExpect(jsonPath("$.lecturersWithoutAssignment").value(1));
    }

    @Test
    void departmentStats_forAccountWithoutDepartment_isNot500() throws Exception {
        mvc.perform(get("/api/dashboard/department-admin").header(AUTH, bearer(newUser("auic", "ADMIN", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departmentId").value(nullValue()))
                .andExpect(jsonPath("$.totalTopics").value(0));
    }

    @Test
    void updatingRolePrivileges_withWrongTypes_is400() throws Exception {
        User admin = newUser("auic2", "ADMIN", null);
        Long roleId = roleRepo.findByName("STUDENT").orElseThrow().getId();
        mvc.perform(put("/api/roles/{id}", roleId).header(AUTH, bearer(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"x\",\"privilegeIds\":\"abc\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/roles/{id}", roleId).header(AUTH, bearer(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"x\",\"privilegeIds\":[\"abc\"]}"))
                .andExpect(status().isBadRequest());
    }
}
