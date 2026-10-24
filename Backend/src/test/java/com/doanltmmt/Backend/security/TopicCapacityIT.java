package com.doanltmmt.Backend.security;

import com.doanltmmt.Backend.entity.enums.WorkspaceStatus;
import com.doanltmmt.Backend.entity.enums.TopicStatus;
import com.doanltmmt.Backend.entity.AcademicClass;
import com.doanltmmt.Backend.entity.Department;
import com.doanltmmt.Backend.entity.Lecturer;
import com.doanltmmt.Backend.entity.Student;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.TopicRegistration;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.entity.Workspace;
import com.doanltmmt.Backend.entity.WorkspaceClass;
import com.doanltmmt.Backend.repository.WorkspaceClassRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P2-04 (D-01 = B): nhiều sinh viên độc lập trên một đề tài theo {@code capacity}.
 */
@SuppressWarnings("null")
class TopicCapacityIT extends AbstractSecurityIT {

    @Autowired private WorkspaceClassRepository workspaceClassRepo;

    private Lecturer lecturer;
    private Workspace ws;
    private Topic topic;
    private final List<Student> students = new ArrayList<>();

    @BeforeEach
    void setUp() {
        Department dept = newDepartment("CAP");
        AcademicClass cls = newClass(dept);
        lecturer = newLecturer("lcap", dept);
        ws = newWorkspace(dept, "OPEN_REGISTRATION");
        WorkspaceClass link = new WorkspaceClass();
        link.setWorkspace(ws);
        link.setAcademicClass(cls);
        link.setActive(true);
        workspaceClassRepo.save(link);
        topic = newTopic(lecturer, ws, 3);
        for (int i = 0; i < 5; i++) students.add(newStudent("scap" + i, dept, cls));
    }

    private ResultActions register(Student s) throws Exception {
        return mvc.perform(post("/api/registration/register").param("studentId", s.getId().toString())
                .param("topicId", topic.getId().toString()).header(AUTH, bearer(s.getUser())));
    }

    private Long registerOk(Student s) throws Exception {
        String body = register(s).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private ResultActions asLecturer(String url, Long regId) throws Exception {
        return mvc.perform(post(url, regId).header(AUTH, bearer(lecturer.getUser())));
    }

    private TopicStatus topicStatus() {
        return topicRepo.findById(topic.getId()).orElseThrow().getStatus();
    }

    @Test
    void capacity3_approvesThreeStudents_thenFull_fourthCannotRegister() throws Exception {
        for (int i = 0; i < 3; i++) {
            asLecturer("/api/registration/approve/{id}", registerOk(students.get(i))).andExpect(status().isOk());
        }
        assertEquals(TopicStatus.FULL, topicStatus());
        assertEquals(3, regRepo.countByTopic_IdAndApprovedTrue(topic.getId()));

        register(students.get(3)).andExpect(status().isBadRequest()).andExpect(errorMessageContains("đủ số sinh viên"));
    }

    @Test
    void approvingBeyondCapacity_isRejected() throws Exception {
        // 4 SV đăng ký khi đề tài còn mở, GV duyệt lần lượt → lượt thứ 4 bị chặn
        List<Long> regs = new ArrayList<>();
        for (int i = 0; i < 4; i++) regs.add(registerOk(students.get(i)));
        for (int i = 0; i < 3; i++) asLecturer("/api/registration/approve/{id}", regs.get(i)).andExpect(status().isOk());
        asLecturer("/api/registration/approve/{id}", regs.get(3))
                .andExpect(status().isBadRequest()).andExpect(errorMessageContains("đủ số sinh viên"));
        assertEquals(3, regRepo.countByTopic_IdAndApprovedTrue(topic.getId()));
    }

    @Test
    void rejectingAnApprovedStudent_reopensTheTopic() throws Exception {
        List<Long> regs = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Long id = registerOk(students.get(i));
            regs.add(id);
            asLecturer("/api/registration/approve/{id}", id).andExpect(status().isOk());
        }
        assertEquals(TopicStatus.FULL, topicStatus());

        asLecturer("/api/registration/reject/{id}", regs.get(0)).andExpect(status().isOk());
        assertEquals(TopicStatus.OPEN, topicStatus());
        registerOk(students.get(3));
    }

    @Test
    void topicCompletes_onlyWhenEveryApprovedStudentIsGraded() throws Exception {
        TopicRegistration r1 = newRegistration(students.get(0), topic, true);
        TopicRegistration r2 = newRegistration(students.get(1), topic, true);
        ws.setStatus(WorkspaceStatus.IN_PROGRESS);
        workspaceRepo.save(ws);

        grade(r1).andExpect(status().isOk());
        assertEquals(TopicStatus.OPEN, topicStatus(), "còn SV chưa có điểm thì đề tài chưa hoàn thành");
        grade(r2).andExpect(status().isOk());
        assertEquals(TopicStatus.COMPLETED, topicStatus());
    }

    private ResultActions grade(TopicRegistration r) throws Exception {
        return mvc.perform(post("/api/registration/grade/{id}", r.getId()).header(AUTH, bearer(lecturer.getUser()))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("score", 8.5, "feedback", "Tốt"))));
    }

    @Test
    void reopeningAFullTopic_keepsItFull() throws Exception {
        ws.setStatus(WorkspaceStatus.OPEN_REGISTRATION);
        Topic small = newTopic(lecturer, ws, 1);
        newRegistration(students.get(0), small, true);
        small.setStatus(TopicStatus.CLOSED);
        topicRepo.save(small);
        User lec = lecturer.getUser();
        mvc.perform(post("/api/topics/{id}/open", small.getId()).param("lecturerId", lec.getId().toString()).header(AUTH, bearer(lec)))
                .andExpect(status().isOk());
        assertEquals(TopicStatus.FULL, topicRepo.findById(small.getId()).orElseThrow().getStatus());
    }
}
