package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.dto.TopicDTO;
import com.doanltmmt.Backend.entity.*;
import com.doanltmmt.Backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TopicServiceTest {

    @Mock private TopicRepository topicRepo;
    @Mock private LecturerRepository lecturerRepo;
    @Mock private TopicRegistrationRepository regRepo;
    @Mock private UserRepository userRepo;
    @Mock private WorkspaceRepository workspaceRepo;
    @Mock private SecurityScopeService scope;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private TopicService topicService;

    private User lecturerUser;
    private Lecturer lecturer;
    private Workspace workspace;

    @BeforeEach
    void setUp() {
        Department dept = new Department();
        dept.setId(1L);

        Role role = new Role();
        role.setName("LECTURER");

        lecturerUser = new User();
        lecturerUser.setId(2L);
        lecturerUser.setFullName("Nguyễn Văn A");
        lecturerUser.setRole(role);
        lecturerUser.setDepartment(dept);

        lecturer = new Lecturer();
        lecturer.setId(10L);
        lecturer.setUser(lecturerUser);

        workspace = new Workspace();
        workspace.setId(1L);
        workspace.setStatus("OPEN_TOPIC");
        workspace.setDepartment(dept);
    }

    @Test
    void createTopic_UnauthorizedLecturer_ShouldThrowException() {
        when(scope.hasRole("LECTURER")).thenReturn(true);
        when(scope.requireCurrentUser()).thenReturn(lecturerUser);

        Topic topic = new Topic();
        topic.setWorkspace(workspace);

        // Trying to create topic for lecturerId 99 while current is 2
        assertThrows(ResponseStatusException.class, () -> {
            topicService.createTopic(99L, topic);
        });
    }

    @Test
    void createTopic_Success_ShouldReturnDTO() {
        when(workspaceRepo.findById(1L)).thenReturn(Optional.of(workspace));
        when(lecturerRepo.findById(10L)).thenReturn(Optional.of(lecturer));
        when(topicRepo.save(any())).thenAnswer(i -> {
            Topic t = i.getArgument(0);
            t.setId(100L);
            return t;
        });

        Topic topic = new Topic();
        topic.setWorkspace(workspace);
        topic.setTitle("Test Topic");

        TopicDTO result = topicService.createTopic(10L, topic);

        assertEquals(100L, result.getId());
        assertEquals("Test Topic", result.getTitle());
        verify(auditLogService).log(eq("CREATE_TOPIC"), any(), eq("100"), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAll_ShouldBuildResponse() {
        Page<Topic> page = new PageImpl<>(List.of(new Topic()));
        when(topicRepo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Map<String, Object> response = topicService.getAll(null, "search", "OPEN", null, 0, 10);

        assertNotNull(response.get("items"));
        assertEquals(1, ((List<?>) response.get("items")).size());
    }
}
