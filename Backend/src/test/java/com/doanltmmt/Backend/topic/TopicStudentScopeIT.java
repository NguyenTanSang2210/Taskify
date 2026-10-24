package com.doanltmmt.Backend.topic;

import com.doanltmmt.Backend.entity.AcademicClass;
import com.doanltmmt.Backend.entity.Department;
import com.doanltmmt.Backend.entity.Lecturer;
import com.doanltmmt.Backend.entity.Student;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.entity.Workspace;
import com.doanltmmt.Backend.entity.WorkspaceClass;
import com.doanltmmt.Backend.security.AbstractSecurityIT;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phạm vi xem đề tài của sinh viên (docs/SECURITY.md — "Đề tài — Xem danh sách / chi tiết"): chỉ đề tài trong các
 * đợt đang gắn với lớp mình, cộng đề tài mình đã đăng ký. Danh sách và chi tiết áp cùng một quy tắc.
 */
@SuppressWarnings("null")
class TopicStudentScopeIT extends AbstractSecurityIT {

    private Lecturer lecturer;
    private Topic topicA, topicB, topicUnlinked;
    private Student studentA, studentB;
    private AcademicClass classA;

    @BeforeEach
    void setUp() {
        Department dept = newDepartment("SC");
        classA = newClass(dept);
        AcademicClass classB = newClass(dept);
        Workspace wsA = newWorkspace(dept, "OPEN_REGISTRATION");
        Workspace wsB = newWorkspace(dept, "OPEN_REGISTRATION");
        Workspace wsUnlinked = newWorkspace(dept, "OPEN_REGISTRATION");
        linkClass(wsA, classA);
        linkClass(wsB, classB);
        // lớp A từng gắn rồi bị gỡ khỏi đợt này → không còn tính
        WorkspaceClass removed = linkClass(wsUnlinked, classA);
        removed.setActive(false);
        workspaceClassRepo.save(removed);

        lecturer = newLecturer("gvsc", dept);
        topicA = newTopic(lecturer, wsA, 2);
        topicB = newTopic(lecturer, wsB, 2);
        topicUnlinked = newTopic(lecturer, wsUnlinked, 2);
        studentA = newStudent("sva", dept, classA);
        studentB = newStudent("svb", dept, classB);
    }

    /** Id các đề tài của giảng viên trong bài test mà {@code viewer} thấy ở GET /api/topics. */
    private Set<Long> visibleTopicIds(User viewer) throws Exception {
        String body = mvc.perform(get("/api/topics").param("lecturerId", lecturer.getId().toString())
                        .param("size", "50").header(AUTH, bearer(viewer)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Number> ids = JsonPath.read(body, "$.items[*].id");
        Set<Long> result = ids.stream().map(Number::longValue).collect(Collectors.toSet());
        assertEquals(result.size(), ((Number) JsonPath.read(body, "$.totalElements")).intValue(), "totalElements khớp số mục");
        return result;
    }

    private void expectDetail(User viewer, Topic topic, int status) throws Exception {
        mvc.perform(get("/api/topics/{id}", topic.getId()).header(AUTH, bearer(viewer)))
                .andExpect(status().is(status));
    }

    @Test
    void student_listsOnlyTopicsOfWorkspacesLinkedToOwnClass() throws Exception {
        assertEquals(Set.of(topicA.getId()), visibleTopicIds(studentA.getUser()));
        assertEquals(Set.of(topicB.getId()), visibleTopicIds(studentB.getUser()));
    }

    @Test
    void student_detailOutsideOwnClassWorkspaces_is404() throws Exception {
        expectDetail(studentA.getUser(), topicA, 200);
        expectDetail(studentA.getUser(), topicB, 404);
        expectDetail(studentA.getUser(), topicUnlinked, 404);
        expectDetail(studentB.getUser(), topicB, 200);
        expectDetail(studentB.getUser(), topicA, 404);
    }

    @Test
    void student_alwaysSeesTopicsTheyRegistered_evenAfterChangingClass() throws Exception {
        // SV đăng ký đề tài B khi còn ở lớp B, sau đó chuyển sang lớp A
        newRegistration(studentB, topicB, true);
        studentB.setAcademicClass(classA);
        studentRepo.save(studentB);

        assertEquals(Set.of(topicA.getId(), topicB.getId()), visibleTopicIds(studentB.getUser()));
        mvc.perform(get("/api/topics/{id}", topicB.getId()).header(AUTH, bearer(studentB.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myRegistrationStatus").value("DA_DUYET"))
                .andExpect(jsonPath("$.member").value(true));
    }

    @Test
    void studentWithoutClass_seesNothingUnlessRegistered() throws Exception {
        Student noClass = newStudent("svnolop", topicA.getWorkspace().getDepartment(), null);
        assertEquals(Set.of(), visibleTopicIds(noClass.getUser()));
        expectDetail(noClass.getUser(), topicA, 404);

        newRegistration(noClass, topicUnlinked, false);
        assertEquals(Set.of(topicUnlinked.getId()), visibleTopicIds(noClass.getUser()));
        expectDetail(noClass.getUser(), topicUnlinked, 200);
    }

    @Test
    void lecturerAndAdmin_areNotRestrictedByClass() throws Exception {
        Set<Long> all = Set.of(topicA.getId(), topicB.getId(), topicUnlinked.getId());
        assertEquals(all, visibleTopicIds(lecturer.getUser()));
        assertEquals(all, visibleTopicIds(newUser("adsc", "ADMIN", null)));
        expectDetail(lecturer.getUser(), topicB, 200);
    }
}
