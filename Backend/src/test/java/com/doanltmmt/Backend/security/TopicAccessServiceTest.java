package com.doanltmmt.Backend.security;

import com.doanltmmt.Backend.entity.Department;
import com.doanltmmt.Backend.entity.Lecturer;
import com.doanltmmt.Backend.entity.Role;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.entity.Workspace;
import com.doanltmmt.Backend.repository.TopicRegistrationRepository;
import com.doanltmmt.Backend.repository.TopicRepository;
import com.doanltmmt.Backend.service.SecurityScopeService;
import com.doanltmmt.Backend.service.TopicAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Ma trận quyền của TopicAccessService: 4 vai trò × (liên quan / không liên quan tới đề tài) + các trường hợp biên.
 * Thuần đơn vị (Mockito), không cần Spring/DB.
 */
class TopicAccessServiceTest {

    private TopicRegistrationRepository regRepo;
    private TopicRepository topicRepo;
    private SecurityScopeService scope;
    private TopicAccessService service;

    private Department deptA;
    private Department deptB;
    private Topic topic;          // đề tài của giảng viên 10, workspace thuộc khoa A

    @BeforeEach
    void setUp() {
        regRepo = mock(TopicRegistrationRepository.class);
        topicRepo = mock(TopicRepository.class);
        scope = mock(SecurityScopeService.class);
        service = new TopicAccessService(topicRepo, regRepo, scope);

        deptA = department(1L);
        deptB = department(2L);

        Workspace ws = new Workspace();
        ws.setId(5L);
        ws.setDepartment(deptA);

        Lecturer lecturer = new Lecturer();
        lecturer.setId(10L);

        topic = new Topic();
        topic.setId(100L);
        topic.setWorkspace(ws);
        topic.setLecturer(lecturer);
    }

    // ------------------------------------------------------------------ dựng dữ liệu

    private static Department department(long id) {
        Department d = new Department();
        d.setId(id);
        return d;
    }

    private static User user(long id, String roleName, Department dept) {
        User u = new User();
        u.setId(id);
        if (roleName != null) {
            u.setRole(new Role(roleName));
        }
        u.setDepartment(dept);
        return u;
    }

    // ------------------------------------------------------------------ ADMIN

    @Test
    void admin_isOwnerAndMemberOfEverything() {
        User admin = user(1L, "ADMIN", null);
        assertTrue(service.isOwner(topic, admin));
        assertTrue(service.isMember(topic, admin));
    }

    // ------------------------------------------------------------------ DEPARTMENT_ADMIN

    @Test
    void departmentAdmin_ofTheTopicsDepartment_isOwnerAndMember() {
        User da = user(2L, "DEPARTMENT_ADMIN", deptA);
        assertTrue(service.isOwner(topic, da));
        assertTrue(service.isMember(topic, da));
    }

    @Test
    void departmentAdmin_ofAnotherDepartment_hasNoAccess() {
        User da = user(3L, "DEPARTMENT_ADMIN", deptB);
        assertFalse(service.isOwner(topic, da));
        assertFalse(service.isMember(topic, da));
    }

    @Test
    void departmentAdmin_withoutDepartment_hasNoAccess() {
        assertFalse(service.isOwner(topic, user(4L, "DEPARTMENT_ADMIN", null)));
    }

    @Test
    void departmentAdmin_topicWithoutWorkspaceOrDepartment_hasNoAccess() {
        topic.setWorkspace(null);
        assertFalse(service.isOwner(topic, user(2L, "DEPARTMENT_ADMIN", deptA)));

        Workspace noDept = new Workspace();
        topic.setWorkspace(noDept);
        assertFalse(service.isOwner(topic, user(2L, "DEPARTMENT_ADMIN", deptA)));
    }

    // ------------------------------------------------------------------ LECTURER

    @Test
    void lecturer_whoSupervisesTheTopic_isOwnerAndMember() {
        User lecturer = user(10L, "LECTURER", deptA);
        assertTrue(service.isOwner(topic, lecturer));
        assertTrue(service.isMember(topic, lecturer));
    }

    @Test
    void lecturer_whoDoesNotSuperviseTheTopic_hasNoAccess_evenInTheSameDepartment() {
        User other = user(11L, "LECTURER", deptA);
        assertFalse(service.isOwner(topic, other));
        assertFalse(service.isMember(topic, other));
    }

    @Test
    void lecturer_topicWithoutLecturer_hasNoAccess() {
        topic.setLecturer(null);
        assertFalse(service.isOwner(topic, user(10L, "LECTURER", deptA)));
    }

    // ------------------------------------------------------------------ STUDENT

    @Test
    void student_withApprovedRegistration_isMemberButNotOwner() {
        User student = user(20L, "STUDENT", deptA);
        when(regRepo.existsByStudent_IdAndTopic_IdAndApprovedTrue(20L, 100L)).thenReturn(true);

        assertTrue(service.isMember(topic, student));
        assertFalse(service.isOwner(topic, student), "sinh viên không bao giờ là người quản lý đề tài");
    }

    @Test
    void student_withPendingOrRejectedOrNoRegistration_isNotMember() {
        User student = user(21L, "STUDENT", deptA);
        when(regRepo.existsByStudent_IdAndTopic_IdAndApprovedTrue(21L, 100L)).thenReturn(false);

        assertFalse(service.isMember(topic, student));
    }

    // ------------------------------------------------------------------ vai trò lạ / thiếu dữ liệu

    @Test
    void customRole_hasNoAccess_evenIfNamedLikeAStaffRole() {
        assertFalse(service.isOwner(topic, user(30L, "TEACHING_ASSISTANT", deptA)));
        assertFalse(service.isMember(topic, user(30L, "TEACHING_ASSISTANT", deptA)));
        assertFalse(service.isMember(topic, user(31L, "admin", null)), "tên vai trò phân biệt hoa thường");
    }

    @Test
    void userWithoutRole_hasNoAccess() {
        assertFalse(service.isOwner(topic, user(32L, null, deptA)));
        assertFalse(service.isMember(topic, user(32L, null, deptA)));
    }

    @Test
    void nullArguments_areNeverGranted() {
        assertFalse(service.isOwner(null, user(1L, "ADMIN", null)));
        assertFalse(service.isOwner(topic, null));
        assertFalse(service.isMember(null, user(1L, "ADMIN", null)));
        assertFalse(service.isMember(topic, null));
    }

    // ------------------------------------------------------------------ dạng dùng người dùng hiện tại

    @Test
    void requireOwner_throws403_forANonOwner_andPassesForAnOwner() {
        when(scope.requireCurrentUser()).thenReturn(user(11L, "LECTURER", deptA));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.requireOwner(topic));
        assertEquals(403, ex.getStatusCode().value());

        when(scope.requireCurrentUser()).thenReturn(user(10L, "LECTURER", deptA));
        service.requireOwner(topic);
    }

    @Test
    void requireMember_throws403_forANonMember() {
        when(scope.requireCurrentUser()).thenReturn(user(21L, "STUDENT", deptA));
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> service.requireMember(topic)).getStatusCode().value());
    }

    @Test
    void loadTopicOr404_distinguishesMissingIdFromMissingTopic() {
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.loadTopicOr404(null)).getStatusCode().value());

        when(topicRepo.findById(999L)).thenReturn(Optional.empty());
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> service.loadTopicOr404(999L)).getStatusCode().value());

        when(topicRepo.findById(100L)).thenReturn(Optional.of(topic));
        assertEquals(topic, service.loadTopicOr404(100L));
    }
}
