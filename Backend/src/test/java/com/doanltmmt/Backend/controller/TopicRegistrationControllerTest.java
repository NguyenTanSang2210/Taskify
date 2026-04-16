package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.DataSeeder;
import com.doanltmmt.Backend.dto.TopicRegistrationDTO;
import com.doanltmmt.Backend.service.TopicRegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("null")
public class TopicRegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TopicRegistrationService topicRegistrationService;

    @MockBean
    private DataSeeder dataSeeder;

    @Test
    @WithMockUser(username = "student1", roles = {"STUDENT"})
    public void registerTopic_ShouldSuccess() throws Exception {
        Long studentId = 1L;
        Long topicId = 10L;

        TopicRegistrationDTO dto = new TopicRegistrationDTO(null);
        dto.setId(99L);
        dto.setStudentId(studentId);

        given(topicRegistrationService.registerTopic(eq(studentId), eq(topicId))).willReturn(dto);

        mockMvc.perform(post("/api/registration/register")
                .param("studentId", studentId.toString())
                .param("topicId", topicId.toString()))
                .andExpect(status().isOk());
    }
}
