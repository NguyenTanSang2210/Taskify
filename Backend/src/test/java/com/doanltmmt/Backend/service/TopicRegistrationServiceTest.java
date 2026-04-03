package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.dto.TopicRegistrationDTO;
import com.doanltmmt.Backend.entity.*;
import com.doanltmmt.Backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TopicRegistrationServiceTest {

    @Mock private TopicRegistrationRepository regRepo;
    @Mock private TopicRepository topicRepo;
    @Mock private StudentRepository studentRepo;
    @Mock private LecturerRepository lecturerRepo;
    @Mock private UserRepository userRepo;
    @Mock private EventPublisher eventPublisher;
    @Mock private NotificationRepository notificationRepo;
    @Mock private AuditLogService auditLogService;
    @Mock private EmailService emailService;
    @Mock private SecurityScopeService scope;
    @Mock private WorkspaceClassRepository workspaceClassRepo;

    @InjectMocks
    private TopicRegistrationService registrationService;

    private User studentUser;
    private Student student;
    private Topic topic;
    private Workspace workspace;
    private AcademicClass academicClass;

    @BeforeEach
    void setUp() {
        studentUser = new User();
        studentUser.setId(3L);
        studentUser.setFullName("Lê Văn B");

        academicClass = new AcademicClass();
        academicClass.setId(10L);

        student = new Student();
        student.setId(3L);
        student.setUser(studentUser);
        student.setAcademicClass(academicClass);

        workspace = new Workspace();
        workspace.setId(1L);
        workspace.setStatus("OPEN_REGISTRATION");

        topic = new Topic();
        topic.setId(100L);
        topic.setWorkspace(workspace);
        topic.setCapacity(10);
        topic.setStatus("OPEN");
    }

    @Test
    void registerTopic_StudentHasPending_ShouldThrowException() {
        when(scope.requireCurrentUser()).thenReturn(studentUser);
        when(topicRepo.findById(100L)).thenReturn(Optional.of(topic));
        when(studentRepo.findById(3L)).thenReturn(Optional.of(student));
        when(workspaceClassRepo.existsByWorkspace_IdAndAcademicClass_IdAndActiveTrue(1L, 10L)).thenReturn(true);
        
        // Already has a pending registration
        when(regRepo.existsByStudent_IdAndTopic_Workspace_IdAndApprovedIsNull(3L, 1L)).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> {
            registrationService.registerTopic(3L, 100L);
        });
    }

    @Test
    void registerTopic_Success_ShouldReturnDTO() {
        when(scope.requireCurrentUser()).thenReturn(studentUser);
        when(topicRepo.findById(100L)).thenReturn(Optional.of(topic));
        when(studentRepo.findById(3L)).thenReturn(Optional.of(student));
        when(workspaceClassRepo.existsByWorkspace_IdAndAcademicClass_IdAndActiveTrue(1L, 10L)).thenReturn(true);
        when(regRepo.existsByStudent_IdAndTopic_Workspace_IdAndApprovedTrue(3L, 1L)).thenReturn(false);
        when(regRepo.existsByStudent_IdAndTopic_Workspace_IdAndApprovedIsNull(3L, 1L)).thenReturn(false);
        when(regRepo.findTopByStudent_IdAndTopic_IdOrderByRegisteredAtDesc(3L, 100L)).thenReturn(Optional.empty());
        
        when(regRepo.save(any())).thenAnswer(i -> {
            TopicRegistration r = i.getArgument(0);
            r.setId(500L);
            return r;
        });

        TopicRegistrationDTO result = registrationService.registerTopic(3L, 100L);

        assertNotNull(result);
        assertEquals(500L, result.getId());
        verify(auditLogService).log(eq("REGISTER_TOPIC"), any(), eq("500"), any());
        verify(eventPublisher).registrationChanged(any(), eq("REGISTRATION_CREATED"));
    }

    @Test
    void approve_Success_ShouldUpdateStatus() {
        TopicRegistration reg = new TopicRegistration();
        reg.setId(500L);
        reg.setTopic(topic);
        reg.setStudent(student);

        User lecturerUser = new User();
        lecturerUser.setId(2L);
        Lecturer lecturer = new Lecturer();
        lecturer.setId(2L);
        lecturer.setUser(lecturerUser);
        topic.setLecturer(lecturer);

        when(regRepo.findById(500L)).thenReturn(Optional.of(reg));
        when(scope.requireCurrentUser()).thenReturn(lecturerUser);
        when(regRepo.existsByTopic_IdAndApprovedTrue(100L)).thenReturn(false);
        when(regRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(notificationRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        TopicRegistrationDTO result = registrationService.approve(500L);

        assertTrue(result.getApproved());
        verify(auditLogService).log(eq("APPROVE_REGISTRATION"), any(), eq("500"), any());
        verify(emailService).sendEmail(any(), any(), any());
    }
}
