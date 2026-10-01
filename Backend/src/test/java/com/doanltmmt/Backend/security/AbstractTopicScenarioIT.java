package com.doanltmmt.Backend.security;

import com.doanltmmt.Backend.entity.AcademicClass;
import com.doanltmmt.Backend.entity.Department;
import com.doanltmmt.Backend.entity.Lecturer;
import com.doanltmmt.Backend.entity.Student;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.entity.Workspace;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * Kịch bản dùng chung cho các test phân quyền THEO ĐỀ TÀI (IDOR):
 *
 * <pre>
 *   Khoa A: deptAdminA · giảng viên lecA · đề tài topicA (workspace wsA, đang thực hiện)
 *           sinh viên stuA1 (ĐÃ DUYỆT trên topicA) · stuA2 (CHỜ DUYỆT trên topicA)
 *   Khoa B: deptAdminB · giảng viên lecB · đề tài topicB (workspace wsB) · sinh viên stuB1 (đã duyệt trên topicB)
 *   Hệ thống: admin
 * </pre>
 * Mọi quy tắc "A không được đụng vào dữ liệu của B" đều kiểm tra trên kịch bản này.
 */
@SuppressWarnings("null")
abstract class AbstractTopicScenarioIT extends AbstractSecurityIT {

    protected Department deptA;
    protected Department deptB;
    protected User admin;
    protected User deptAdminA;
    protected User deptAdminB;
    protected Lecturer lecA;
    protected Lecturer lecB;
    protected Student stuA1;
    protected Student stuA2;
    protected Student stuB1;
    protected Workspace wsA;
    protected Workspace wsB;
    protected Topic topicA;
    protected Topic topicB;

    @BeforeEach
    void buildScenario() {
        deptA = newDepartment("A");
        deptB = newDepartment("B");
        AcademicClass classA = newClass(deptA);
        AcademicClass classB = newClass(deptB);

        admin = newUser("adm", "ADMIN", null);
        deptAdminA = newUser("dpa", "DEPARTMENT_ADMIN", deptA);
        deptAdminB = newUser("dpb", "DEPARTMENT_ADMIN", deptB);

        lecA = newLecturer("leca", deptA);
        lecB = newLecturer("lecb", deptB);

        wsA = newWorkspace(deptA, "IN_PROGRESS");
        wsB = newWorkspace(deptB, "IN_PROGRESS");
        topicA = newTopic(lecA, wsA, 3);
        topicB = newTopic(lecB, wsB, 3);

        stuA1 = newStudent("sa1", deptA, classA);
        stuA2 = newStudent("sa2", deptA, classA);
        stuB1 = newStudent("sb1", deptB, classB);
        newRegistration(stuA1, topicA, true);
        newRegistration(stuA2, topicA, null);
        newRegistration(stuB1, topicB, true);
    }

    // ------------------------------------------------------------------ gọi API dưới danh nghĩa một người dùng

    protected User userOf(Lecturer l) {
        return l.getUser();
    }

    protected User userOf(Student s) {
        return s.getUser();
    }

    protected ResultActions get(String url, User as, Object... uriVars) throws Exception {
        return mvc.perform(request(HttpMethod.GET, url, uriVars).header(AUTH, bearer(as)));
    }

    protected ResultActions call(HttpMethod method, String url, User as, Object body, Object... uriVars) throws Exception {
        MockHttpServletRequestBuilder builder = request(method, url, uriVars).header(AUTH, bearer(as));
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        }
        return mvc.perform(builder);
    }

    protected ResultActions post(String url, User as, Object body, Object... uriVars) throws Exception {
        return call(HttpMethod.POST, url, as, body, uriVars);
    }
}
