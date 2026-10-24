package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.DataSeeder;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.enums.TopicStatus;
import com.doanltmmt.Backend.repository.LecturerRepository;
import com.doanltmmt.Backend.repository.TopicRegistrationRepository;
import com.doanltmmt.Backend.repository.TopicRepository;
import com.doanltmmt.Backend.repository.UserRepository;
import com.doanltmmt.Backend.repository.WorkspaceRepository;
import com.doanltmmt.Backend.service.AuditLogService;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings({"unchecked", "null"})
public class TopicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TopicRepository topicRepo;

    @MockitoBean
    private LecturerRepository lecturerRepo;

    @MockitoBean
    private TopicRegistrationRepository regRepo;

    @MockitoBean
    private UserRepository userRepo;

    @MockitoBean
    private WorkspaceRepository workspaceRepo;

    @MockitoBean
    private AuditLogService auditLogService;
    
    @MockitoBean
    private DataSeeder dataSeeder;

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void getAllTopics_ShouldReturnTopics() throws Exception {
        Topic t = new Topic();
        t.setId(1L);
        t.setTitle("Test Topic");
        t.setStatus(TopicStatus.OPEN);
        
        given(topicRepo.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(t)));
        
        TopicRegistrationRepository.TopicRegistrationCounts counts =
                org.mockito.Mockito.mock(TopicRegistrationRepository.TopicRegistrationCounts.class);
        given(counts.getTopicId()).willReturn(1L);
        given(counts.getTotal()).willReturn(5L);
        given(regRepo.countByTopicIds(List.of(1L))).willReturn(List.of(counts));

        mockMvc.perform(get("/api/topics")
                .param("page", "0")
                .param("size", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("Test Topic"))
                .andExpect(jsonPath("$.items[0].registrationCount").value(5));
    }
}
