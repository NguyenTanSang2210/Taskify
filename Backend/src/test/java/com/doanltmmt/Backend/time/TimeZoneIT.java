package com.doanltmmt.Backend.time;

import com.doanltmmt.Backend.config.AppTimeZone;
import com.doanltmmt.Backend.entity.AcademicClass;
import com.doanltmmt.Backend.entity.Department;
import com.doanltmmt.Backend.entity.Lecturer;
import com.doanltmmt.Backend.entity.Message;
import com.doanltmmt.Backend.entity.Notification;
import com.doanltmmt.Backend.entity.Student;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.Workspace;
import com.doanltmmt.Backend.repository.MessageRepository;
import com.doanltmmt.Backend.repository.NotificationRepository;
import com.doanltmmt.Backend.security.AbstractSecurityIT;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Giờ ứng dụng là giờ Việt Nam; CSDL lưu UTC. Một thời điểm 16:38 (VN) phải được lưu là 09:38 (UTC)
 * và trả về giao diện đúng 16:38 — kể cả qua truy vấn hộp thư.
 */
@SuppressWarnings("null")
class TimeZoneIT extends AbstractSecurityIT {

    private static final LocalDateTime VN_TIME = LocalDateTime.of(2030, 1, 15, 16, 38, 0);

    @Autowired private NotificationRepository notificationRepo;
    @Autowired private MessageRepository messageRepo;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager em;

    @Test
    void applicationRunsOnVietnamTime() {
        assertEquals(ZoneId.of(AppTimeZone.DEFAULT), ZoneId.systemDefault());
    }

    @Test
    void storedAsUtc_returnedAsVietnamTime() throws Exception {
        Student s = newStudent("stz", newDepartment("TZ"), null);
        Notification n = new Notification();
        n.setUser(s.getUser());
        n.setType("INFO");
        n.setTitle("Giờ");
        n.setContent("Kiểm tra múi giờ");
        n.setCreatedAt(VN_TIME);
        Long id = notificationRepo.saveAndFlush(n).getId();

        String raw = jdbc.queryForObject("select date_format(created_at, '%Y-%m-%d %H:%i') from notifications where id = ?", String.class, id);
        assertEquals("2030-01-15 09:38", raw, "CSDL lưu UTC");

        em.clear();
        assertEquals(VN_TIME, notificationRepo.findById(id).orElseThrow().getCreatedAt());
        mvc.perform(get("/api/notifications/mine").param("userId", s.getUser().getId().toString()).header(AUTH, bearer(s.getUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].createdAt").value("2030-01-15T16:38:00"));
    }

    @Test
    void inboxTimes_matchTheEntity() {
        Department dept = newDepartment("TZ2");
        AcademicClass cls = newClass(dept);
        Lecturer lec = newLecturer("ltz", dept);
        Workspace ws = newWorkspace(dept, "IN_PROGRESS");
        Topic topic = newTopic(lec, ws, 1);
        Student s = newStudent("stz2", dept, cls);
        Message m = new Message();
        m.setSender(s.getUser());
        m.setRecipient(lec.getUser());
        m.setTopic(topic);
        m.setWorkspace(ws);
        m.setContent("Chào thầy");
        m.setCreatedAt(VN_TIME);
        messageRepo.saveAndFlush(m);
        em.clear();

        MessageRepository.InboxRow row = messageRepo.findInboxRowsByUserId(lec.getUser().getId()).get(0);
        assertEquals(VN_TIME, row.getCreatedAt());
        assertEquals(topic.getId(), row.getTopicId());
    }
}
