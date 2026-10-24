package com.doanltmmt.Backend.topic;

import com.doanltmmt.Backend.entity.AcademicClass;
import com.doanltmmt.Backend.entity.Department;
import com.doanltmmt.Backend.entity.Lecturer;
import com.doanltmmt.Backend.entity.Student;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.entity.Workspace;
import com.doanltmmt.Backend.security.AbstractSecurityIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** P4-11 — GET /api/topics/{id}: số liệu đăng ký, trạng thái của SV đang xem, danh sách thành viên chỉ cho thành viên. */
@SuppressWarnings("null")
class TopicDetailIT extends AbstractSecurityIT {

    private Topic topic;
    private Lecturer lecturer;
    private Student approved, pending, outsider;
    private Department dept;

    @BeforeEach
    void setUp() {
        dept = newDepartment("TD");
        AcademicClass cls = newClass(dept);
        Workspace ws = newWorkspace(dept, "OPEN_REGISTRATION");
        lecturer = newLecturer("gvtd", dept);
        topic = newTopic(lecturer, ws, 3);
        approved = newStudent("svok", dept, cls);
        pending = newStudent("svcho", dept, cls);
        outsider = newStudent("svngoai", dept, cls);
        newRegistration(approved, topic, true);
        newRegistration(pending, topic, null);
    }

    @Test
    void memberStudent_seesCountsStatusAndMembers() throws Exception {
        mvc.perform(get("/api/topics/{id}", topic.getId()).header(AUTH, bearer(approved.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(topic.getTitle()))
                .andExpect(jsonPath("$.capacity").value(3))
                .andExpect(jsonPath("$.approvedCount").value(1))
                .andExpect(jsonPath("$.pendingCount").value(1))
                .andExpect(jsonPath("$.myRegistrationStatus").value("DA_DUYET"))
                .andExpect(jsonPath("$.member").value(true))
                .andExpect(jsonPath("$.owner").value(false))
                .andExpect(jsonPath("$.lecturer.email").value(lecturer.getUser().getEmail()))
                .andExpect(jsonPath("$.lecturer.departmentName").value(dept.getName()))
                .andExpect(jsonPath("$.workspace.status").value("OPEN_REGISTRATION"))
                .andExpect(jsonPath("$.members", hasSize(1)))
                .andExpect(jsonPath("$.members[0].studentCode").value(approved.getStudentCode()));
    }

    @Test
    void nonMemberStudent_seesOwnStatusButNoMemberList() throws Exception {
        mvc.perform(get("/api/topics/{id}", topic.getId()).header(AUTH, bearer(pending.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myRegistrationStatus").value("CHO_DUYET"))
                .andExpect(jsonPath("$.member").value(false))
                .andExpect(jsonPath("$.members").value(nullValue()));
        mvc.perform(get("/api/topics/{id}", topic.getId()).header(AUTH, bearer(outsider.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myRegistrationStatus").value("CHUA_DANG_KY"));
    }

    @Test
    void owningLecturer_isOwnerWithoutStudentStatus() throws Exception {
        mvc.perform(get("/api/topics/{id}", topic.getId()).header(AUTH, bearer(lecturer.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner").value(true))
                .andExpect(jsonPath("$.myRegistrationStatus").value(nullValue()))
                .andExpect(jsonPath("$.members", hasSize(1)));
    }

    @Test
    void departmentAdminOfOtherDepartment_gets404_ownDepartmentGets200() throws Exception {
        User other = newUser("qtkkhac", "DEPARTMENT_ADMIN", newDepartment("KHAC"));
        mvc.perform(get("/api/topics/{id}", topic.getId()).header(AUTH, bearer(other)))
                .andExpect(status().isNotFound());
        User same = newUser("qtkcung", "DEPARTMENT_ADMIN", dept);
        mvc.perform(get("/api/topics/{id}", topic.getId()).header(AUTH, bearer(same)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner").value(true));
    }

    @Test
    void unknownTopic_gets404_anonymousGets401() throws Exception {
        mvc.perform(get("/api/topics/{id}", 999_999L).header(AUTH, bearer(approved.getUser())))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/topics/{id}", topic.getId()))
                .andExpect(status().isUnauthorized());
    }
}
