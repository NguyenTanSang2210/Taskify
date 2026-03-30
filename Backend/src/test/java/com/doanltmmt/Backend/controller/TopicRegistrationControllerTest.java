package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.entity.Student;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.TopicRegistration;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.repository.*;
import com.doanltmmt.Backend.service.AuditLogService;
import com.doanltmmt.Backend.service.EmailService;
import com.doanltmmt.Backend.service.EventPublisher;
import com.doanltmmt.Backend.service.SecurityScopeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
public class TopicRegistrationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TopicRepository topicRepo;

    @Mock
    private StudentRepository studentRepo;

    @Mock
    private TopicRegistrationRepository regRepo;

    @Mock
    private LecturerRepository lecturerRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private NotificationRepository notificationRepo;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private EmailService emailService;

    @Mock
    private SecurityScopeService scope;

    @Mock
    private WorkspaceClassRepository workspaceClassRepo;

    @InjectMocks
    private TopicRegistrationController regController;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(regController).build();
    }

    @Test
    public void registerTopic_ShouldSuccess() throws Exception {
        Long studentId = 1L;
        Long topicId = 10L;

        Topic topic = new Topic();
        topic.setId(topicId);
        topic.setStatus("OPEN");
        com.doanltmmt.Backend.entity.Workspace ws = new com.doanltmmt.Backend.entity.Workspace();
        ws.setId(100L);
        ws.setStatus("OPEN_REGISTRATION");
        topic.setWorkspace(ws);

        User u = new User();
        u.setFullName("Test Student");
        com.doanltmmt.Backend.entity.AcademicClass cls = new com.doanltmmt.Backend.entity.AcademicClass();
        cls.setId(200L);
        Student student = new Student();
        student.setId(studentId);
        student.setUser(u);
        student.setStudentCode("SVTEST");
        student.setAcademicClass(cls);

        given(topicRepo.findById(topicId)).willReturn(Optional.of(topic));
        given(studentRepo.findById(studentId)).willReturn(Optional.of(student));
        User currentUser = new User();
        currentUser.setId(studentId);
        given(scope.requireCurrentUser()).willReturn(currentUser);
        given(workspaceClassRepo.existsByWorkspace_IdAndAcademicClass_IdAndActiveTrue(eq(100L), eq(200L))).willReturn(true);
        given(regRepo.existsByStudent_IdAndTopic_Workspace_IdAndApprovedTrue(eq(studentId), eq(100L))).willReturn(false);
        given(regRepo.existsByStudent_IdAndTopic_Workspace_IdAndApprovedIsNull(eq(studentId), eq(100L))).willReturn(false);
        given(regRepo.findTopByStudent_IdAndTopic_IdOrderByRegisteredAtDesc(eq(studentId), eq(topicId))).willReturn(Optional.empty());

        given(regRepo.save(any(TopicRegistration.class))).willAnswer(invocation -> {
            TopicRegistration reg = invocation.getArgument(0);
            reg.setId(99L);
            return reg;
        });

        mockMvc.perform(post("/api/registration/register")
                .param("studentId", studentId.toString())
                .param("topicId", topicId.toString()))
                .andExpect(status().isOk());
    }
}
