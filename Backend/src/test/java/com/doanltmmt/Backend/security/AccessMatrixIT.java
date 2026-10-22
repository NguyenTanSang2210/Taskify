package com.doanltmmt.Backend.security;

import com.doanltmmt.Backend.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Các test bắt buộc còn lại của P1-12: #9 (export), #11 (hồ sơ người khác), #12 (khoa A ↔ khoa B), #17 (gửi thông báo).
 */
@SuppressWarnings("null")
class AccessMatrixIT extends AbstractTopicScenarioIT {

    // ------------------------------------------------------------------ #9 export Excel

    @Test
    void export_lecturerMustChooseOwnTopic() throws Exception {
        get("/api/export/excel", userOf(lecA)).andExpect(status().isBadRequest());                       // thiếu topicId
        get("/api/export/excel?topicId={id}", userOf(lecA), topicB.getId()).andExpect(status().isForbidden()); // đề tài người khác
        get("/api/export/excel?topicId={id}", userOf(lecA), topicA.getId()).andExpect(status().isOk());
    }

    @Test
    void export_departmentAdminOnlyForTopicsOfOwnDepartment_adminForAll() throws Exception {
        get("/api/export/excel?topicId={id}", deptAdminA, topicA.getId()).andExpect(status().isOk());
        get("/api/export/excel?topicId={id}", deptAdminA, topicB.getId()).andExpect(status().isForbidden());
        get("/api/export/excel", deptAdminA).andExpect(status().isBadRequest());
        get("/api/export/excel", admin).andExpect(status().isOk());
        get("/api/export/excel?topicId={id}", admin, topicB.getId()).andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ #11 hồ sơ người dùng

    @Test
    void userProfile_otherPeopleOnlySeeIdAndName() throws Exception {
        User lecturer = userOf(lecA);
        // sinh viên, giảng viên khoa khác, quản trị khoa khác: chỉ id + fullName
        for (User viewer : new User[]{userOf(stuA1), userOf(stuB1), userOf(lecB), deptAdminB}) {
            get("/api/users/{id}", viewer, lecturer.getId())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(lecturer.getId()))
                    .andExpect(jsonPath("$.fullName").exists())
                    .andExpect(jsonPath("$.email").doesNotExist())
                    .andExpect(jsonPath("$.phone").doesNotExist())
                    .andExpect(jsonPath("$.role").doesNotExist());
        }
        // chính chủ / ADMIN / quản trị cùng khoa: đầy đủ
        for (User viewer : new User[]{lecturer, admin, deptAdminA}) {
            get("/api/users/{id}", viewer, lecturer.getId())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").exists());
        }
    }

    @Test
    void userList_isNotAvailableToStudentsOrLecturers() throws Exception {
        get("/api/users", userOf(stuA1)).andExpect(status().isForbidden());
        get("/api/users", userOf(lecA)).andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ #12 khoa A không chạm vào khoa B

    @Test
    void deptAdminA_cannotReadOrTouchWorkspaceOfDepartmentB() throws Exception {
        get("/api/workspaces/{id}", deptAdminA, wsB.getId()).andExpect(status().isForbidden());
        get("/api/workspaces?departmentId={d}", deptAdminA, deptB.getId()).andExpect(status().isForbidden());
        post("/api/workspaces/{id}/transition?to=OPEN_TOPIC", deptAdminA, null, wsB.getId()).andExpect(status().isForbidden());

        get("/api/workspaces/{id}", deptAdminA, wsA.getId()).andExpect(status().isOk());
        get("/api/workspaces/{id}", admin, wsB.getId()).andExpect(status().isOk());
    }

    @Test
    void deptAdminA_cannotSeeProgressOrAssignmentsOfDepartmentB() throws Exception {
        get("/api/progress/topic/{id}", deptAdminA, topicB.getId()).andExpect(status().isForbidden());
        get("/api/progress/student/{id}", deptAdminA, stuB1.getId()).andExpect(status().isForbidden());
        get("/api/assignments?workspaceId={id}", deptAdminA, wsB.getId()).andExpect(status().isForbidden());

        get("/api/progress/topic/{id}", deptAdminA, topicA.getId()).andExpect(status().isOk());
        get("/api/progress/student/{id}", deptAdminA, stuA1.getId()).andExpect(status().isOk());
        get("/api/assignments?workspaceId={id}", deptAdminA, wsA.getId()).andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ #17 gửi thông báo thủ công

    private org.springframework.test.web.servlet.ResultActions notify(User from, User to) throws Exception {
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/notifications/create?userId={u}&title=Nhac+viec&content=Noi+dung", to.getId())
                .header(AUTH, bearer(from)).contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void lecturer_canNotifyOnlyStudentsRegisteredToOwnTopics() throws Exception {
        notify(userOf(lecA), userOf(stuA1)).andExpect(status().isOk());        // SV của mình
        notify(userOf(lecB), userOf(stuA1)).andExpect(status().isForbidden()); // SV của GV khác (#17)
        notify(userOf(lecA), userOf(stuB1)).andExpect(status().isForbidden());
        notify(userOf(lecA), userOf(lecB)).andExpect(status().isForbidden());  // không phải sinh viên
    }

    @Test
    void admin_canNotifyAnyone_studentsAndDeptAdminsCannotSend() throws Exception {
        notify(admin, userOf(stuB1)).andExpect(status().isOk());
        notify(userOf(stuA1), userOf(stuA2)).andExpect(status().isForbidden());
        notify(deptAdminA, userOf(stuA1)).andExpect(status().isForbidden());
    }

    @Test
    void anotherUsersNotificationsCannotBeReadOrChanged() throws Exception {
        User victim = userOf(stuA1);
        notify(admin, victim).andExpect(status().isOk());
        String json = get("/api/notifications/mine?userId={id}", victim, victim.getId()).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long victimNotificationId = com.jayway.jsonpath.JsonPath.<Number>read(json, "$[0].id").longValue();

        // Người không phải ADMIN luôn bị ép về CHÍNH MÌNH, dù gửi userId của người khác: không thấy dữ liệu của nạn nhân
        get("/api/notifications/mine?userId={id}", userOf(stuA2), victim.getId())
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        get("/api/notifications/count?userId={id}", userOf(stuA2), victim.getId())
                .andExpect(status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string("0"));

        // Đánh dấu đã đọc thông báo của người khác: 403; "đọc tất cả" với userId của nạn nhân không đụng tới thông báo của họ
        post("/api/notifications/read/{id}", userOf(stuA2), null, victimNotificationId).andExpect(status().isForbidden());
        post("/api/notifications/read-all?userId={id}", userOf(stuA2), null, victim.getId()).andExpect(status().isOk());
        get("/api/notifications/count?userId={id}", victim, victim.getId())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string("1"));
    }
}
