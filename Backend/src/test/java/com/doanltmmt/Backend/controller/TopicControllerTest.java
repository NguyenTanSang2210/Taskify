package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.DataSeeder;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.repository.LecturerRepository;
import com.doanltmmt.Backend.repository.TopicRegistrationRepository;
import com.doanltmmt.Backend.repository.TopicRepository;
import com.doanltmmt.Backend.repository.UserRepository;
import com.doanltmmt.Backend.repository.WorkspaceRepository;
import com.doanltmmt.Backend.service.AuditLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings({"removal", "unchecked", "null"})
public class TopicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TopicRepository topicRepo;

    @MockBean
    private LecturerRepository lecturerRepo;

    @MockBean
    private TopicRegistrationRepository regRepo;

    @MockBean
    private UserRepository userRepo;

    @MockBean
    private WorkspaceRepository workspaceRepo;

    @MockBean
    private AuditLogService auditLogService;
    
    @MockBean
    private DataSeeder dataSeeder;

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void getAllTopics_ShouldReturnTopics() throws Exception {
        Topic t = new Topic();
        t.setId(1L);
        t.setTitle("Test Topic");
        t.setStatus("OPEN");
        
        given(topicRepo.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(t)));
        
        given(regRepo.countByTopic_Id(1L)).willReturn(5L);

        mockMvc.perform(get("/api/topics")
                .param("page", "0")
                .param("size", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("Test Topic"))
                .andExpect(jsonPath("$.items[0].registrationCount").value(5));
    }
}
