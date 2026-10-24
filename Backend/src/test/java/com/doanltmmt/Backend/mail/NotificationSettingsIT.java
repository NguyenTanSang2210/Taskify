package com.doanltmmt.Backend.mail;

import com.doanltmmt.Backend.entity.Department;
import com.doanltmmt.Backend.entity.Lecturer;
import com.doanltmmt.Backend.entity.Student;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.TopicRegistration;
import com.doanltmmt.Backend.entity.Workspace;
import com.doanltmmt.Backend.repository.EmailDigestItemRepository;
import com.doanltmmt.Backend.security.AbstractSecurityIT;
import com.doanltmmt.Backend.service.NotificationMailer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** P4-06 — cài đặt email thông báo: tắt từng nhóm, gom thư tổng hợp hằng ngày; thông báo trong app không bị ảnh hưởng. */
@SuppressWarnings("null")
class NotificationSettingsIT extends AbstractSecurityIT {

    @Autowired private EmailDigestItemRepository digestRepo;
    @Autowired private NotificationMailer mailer;

    private Lecturer lecturer;
    private Student student;
    private Topic topic;

    @BeforeEach
    void setUp() {
        Department dept = newDepartment("NS");
        Workspace ws = newWorkspace(dept, "OPEN_REGISTRATION");
        lecturer = newLecturer("gvns", dept);
        topic = newTopic(lecturer, ws, 3);
        student = newStudent("svns", dept, newClass(dept));
    }

    private ResultActions saveSettings(Map<String, Boolean> body) throws Exception {
        return mvc.perform(put("/api/notification-settings").header(AUTH, bearer(student.getUser()))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private void approve() throws Exception {
        TopicRegistration r = newRegistration(student, topic, null);
        mvc.perform(post("/api/registration/approve/{id}", r.getId()).header(AUTH, bearer(lecturer.getUser())))
                .andExpect(status().isOk());
    }

    @Test
    void defaults_everyCategoryOn_sentImmediately() throws Exception {
        mvc.perform(get("/api/notification-settings").header(AUTH, bearer(student.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailDigest").value(false))
                .andExpect(jsonPath("$.registration").value(true))
                .andExpect(jsonPath("$.reminder").value(true))
                .andExpect(jsonPath("$.pendingDigest").value(0));
        approve();
        verify(emailService).sendEmail(eq(student.getUser().getEmail()), anyString(), anyString());
    }

    @Test
    void disabledCategory_skipsTheEmail_butKeepsTheInAppNotification() throws Exception {
        saveSettings(Map.of("emailDigest", false, "registration", false, "progress", true, "grade", true, "reminder", true))
                .andExpect(status().isOk()).andExpect(jsonPath("$.registration").value(false));
        approve();
        verify(emailService, never()).sendEmail(eq(student.getUser().getEmail()), anyString(), anyString());
        mvc.perform(get("/api/notifications/mine").param("userId", student.getUser().getId().toString()).header(AUTH, bearer(student.getUser())))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void digestMode_queuesEmails_andSendsOneSummaryEachMorning() throws Exception {
        saveSettings(Map.of("emailDigest", true, "registration", true, "progress", true, "grade", true, "reminder", true))
                .andExpect(status().isOk());
        approve();
        verify(emailService, never()).sendEmail(eq(student.getUser().getEmail()), anyString(), anyString());
        assertEquals(1, digestRepo.countByUser_Id(student.getUser().getId()));
        mvc.perform(get("/api/notification-settings").header(AUTH, bearer(student.getUser())))
                .andExpect(jsonPath("$.emailDigest").value(true)).andExpect(jsonPath("$.pendingDigest").value(1));

        clearInvocations(emailService);
        assertTrue(mailer.sendDigests() >= 1);
        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(emailService, times(1)).sendEmail(eq(student.getUser().getEmail()), subject.capture(), html.capture());
        assertEquals("Taskify - Tổng hợp 1 thông báo", subject.getValue());
        assertTrue(html.getValue().contains(topic.getTitle().replace("&", "&amp;")), html.getValue());
        assertEquals(0, digestRepo.countByUser_Id(student.getUser().getId()), "đã gửi thì xóa khỏi hàng chờ");
    }

    @Test
    void securityEmails_ignoreTheSettings() throws Exception {
        saveSettings(Map.of("emailDigest", true, "registration", false, "progress", false, "grade", false, "reminder", false))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/password/forgot").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("identifier", student.getUser().getUsername()))))
                .andExpect(status().isOk());
        verify(emailService).sendEmail(eq(student.getUser().getEmail()), anyString(), anyString());
        assertEquals(0, digestRepo.countByUser_Id(student.getUser().getId()));
    }

    @Test
    void settingsAreValidated_andRequireLogin() throws Exception {
        mvc.perform(put("/api/notification-settings").header(AUTH, bearer(student.getUser()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"emailDigest\":true}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(get("/api/notification-settings")).andExpect(status().isUnauthorized());
    }
}
