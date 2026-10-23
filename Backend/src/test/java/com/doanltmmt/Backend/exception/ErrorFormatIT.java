package com.doanltmmt.Backend.exception;

import com.doanltmmt.Backend.entity.AcademicClass;
import com.doanltmmt.Backend.entity.Department;
import com.doanltmmt.Backend.entity.Student;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.entity.Workspace;
import com.doanltmmt.Backend.security.AbstractSecurityIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** P3-03 — mọi lỗi API cùng định dạng {timestamp, status, code, error, message, path, errors?}. */
class ErrorFormatIT extends AbstractSecurityIT {

    @Test
    void validationErrorListsFieldErrors() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value(ErrorCodes.VALIDATION_FAILED))
                .andExpect(jsonPath("$.timestamp").value(notNullValue()))
                .andExpect(jsonPath("$.path").value("/api/auth/login"))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("username")))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("password")));
    }

    @Test
    void malformedJsonIsBadRequestNot500() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.MALFORMED_REQUEST))
                .andExpect(jsonPath("$.message").value("Dữ liệu gửi lên không đúng định dạng JSON."));
    }

    @Test
    void unauthenticatedHasCodeAndPath() throws Exception {
        mvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCodes.UNAUTHORIZED))
                .andExpect(jsonPath("$.path").value("/api/users"));
    }

    @Test
    void unknownPathIs404WithCode() throws Exception {
        Department dept = newDepartment("ERR");
        User admin = newUser("aerr", "ADMIN", dept);
        mvc.perform(get("/api/khong-ton-tai").header(AUTH, bearer(admin)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCodes.NOT_FOUND))
                .andExpect(jsonPath("$.message").value("Không tìm thấy đường dẫn yêu cầu."));
    }

    @Test
    void businessRuleCarriesSpecificCode() throws Exception {
        Department dept = newDepartment("ERR");
        AcademicClass cls = newClass(dept);
        Workspace ws = newWorkspace(dept, "OPEN_TOPIC");
        Topic topic = newTopic(newLecturer("lerr", dept), ws, 2);
        Student s = newStudent("serr", dept, cls);
        mvc.perform(post("/api/registration/register").param("studentId", s.getId().toString())
                        .param("topicId", topic.getId().toString()).header(AUTH, bearer(s.getUser())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.WORKSPACE_PHASE))
                .andExpect(jsonPath("$.message").value("Đợt chưa mở đăng ký."));
    }

    @Test
    void wrongTypeNamesTheFieldInVietnamese_andDateOnlyIsAccepted() throws Exception {
        Department dept = newDepartment("ERR");
        com.doanltmmt.Backend.entity.Lecturer lec = newLecturer("lerr2", dept);
        Topic topic = newTopic(lec, newWorkspace(dept, "IN_PROGRESS"), 2);
        String bad = "{\"topicId\":" + topic.getId() + ",\"title\":\"M1\",\"deadline\":\"31/12/2030\"}";
        mvc.perform(post("/api/milestones").header(AUTH, bearer(lec.getUser()))
                        .contentType(MediaType.APPLICATION_JSON).content(bad))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.VALIDATION_FAILED))
                .andExpect(jsonPath("$.errors[0].field").value("deadline"))
                .andExpect(jsonPath("$.message").value("Hạn chót không đúng định dạng ngày giờ (yyyy-MM-ddTHH:mm)"));
        String dateOnly = "{\"topicId\":" + topic.getId() + ",\"title\":\"M1\",\"deadline\":\"2030-12-31\"}";
        mvc.perform(post("/api/milestones").header(AUTH, bearer(lec.getUser()))
                        .contentType(MediaType.APPLICATION_JSON).content(dateOnly))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deadline").value(org.hamcrest.Matchers.startsWith("2030-12-31T00:00")));
    }

    @Test
    void firstErrorFollowsDeclaredFieldOrder() throws Exception {
        Department dept = newDepartment("ERR");
        User deptAdmin = newUser("derr", "DEPARTMENT_ADMIN", dept);
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/workspace-classes/assign").header(AUTH, bearer(deptAdmin))
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("workspaceId"))
                    .andExpect(jsonPath("$.errors[1].field").value("classId"));
        }
    }

    @Test
    void errorBodyNeverLeaksInternals() throws Exception {
        mvc.perform(get("/api/users/{id}", "abc").header(AUTH, bearer(newUser("aerr2", "ADMIN", newDepartment("ERR")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.MALFORMED_REQUEST))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString("java."))));
    }
}
