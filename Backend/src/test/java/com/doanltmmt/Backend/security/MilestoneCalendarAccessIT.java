package com.doanltmmt.Backend.security;

import com.doanltmmt.Backend.entity.enums.CalendarEventType;
import com.doanltmmt.Backend.entity.CalendarEvent;
import com.doanltmmt.Backend.entity.Milestone;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.repository.CalendarEventRepository;
import com.doanltmmt.Backend.repository.MilestoneRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Milestone và lịch của đề tài: chỉ người QUẢN LÝ đề tài đó được tạo/sửa/xóa; chỉ THÀNH VIÊN đề tài được xem.
 * Bao phủ test bắt buộc #5 (giảng viên A tạo/sửa/xóa milestone đề tài của giảng viên B → 403) và #6 (xóa lịch).
 */
@SuppressWarnings("null")
class MilestoneCalendarAccessIT extends AbstractTopicScenarioIT {

    @Autowired private MilestoneRepository milestoneRepo;
    @Autowired private CalendarEventRepository calendarRepo;

    private Milestone milestoneOn(com.doanltmmt.Backend.entity.Topic topic) {
        Milestone m = new Milestone();
        m.setTopic(topic);
        m.setTitle("Mốc thử nghiệm");
        m.setDeadline(LocalDateTime.now().plusDays(7));
        return milestoneRepo.saveAndFlush(m);
    }

    private CalendarEvent eventOn(com.doanltmmt.Backend.entity.Topic topic) {
        CalendarEvent e = new CalendarEvent();
        e.setTopic(topic);
        e.setTitle("Buổi họp thử nghiệm");
        e.setStartTime(LocalDateTime.now().plusDays(1));
        e.setType(CalendarEventType.MEETING);
        return calendarRepo.saveAndFlush(e);
    }

    // ================================================================== MILESTONE — ghi (test #5)

    @Test
    void lecturerB_cannotCreateMilestoneOnLecturerAsTopic() throws Exception {
        post("/api/milestones", userOf(lecB), Map.of("topicId", topicA.getId(), "title", "Lạ", "deadline", "2030-01-01T00:00"))
                .andExpect(status().isForbidden());

        assertEquals(0, milestoneRepo.findByTopic_IdOrderByDeadlineAsc(topicA.getId()).size(), "không được tạo được dòng nào");
    }

    @Test
    void lecturerB_cannotUpdateOrDeleteMilestoneOfAnotherLecturersTopic() throws Exception {
        Milestone m = milestoneOn(topicA);

        call(HttpMethod.PUT, "/api/milestones/{id}", userOf(lecB), Map.of("title", "Bị sửa"), m.getId()).andExpect(status().isForbidden());
        call(HttpMethod.DELETE, "/api/milestones/{id}", userOf(lecB), null, m.getId()).andExpect(status().isForbidden());

        Milestone after = milestoneRepo.findById(m.getId()).orElseThrow();
        assertEquals("Mốc thử nghiệm", after.getTitle(), "tiêu đề không được bị đổi");
    }

    @Test
    void supervisingLecturer_canCreateUpdateAndDeleteOwnMilestone() throws Exception {
        MvcResult created = post("/api/milestones", userOf(lecA),
                Map.of("topicId", topicA.getId(), "title", "Giai đoạn 1", "deadline", "2030-01-01T00:00:00"))
                .andExpect(status().isOk()).andReturn();
        int id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        call(HttpMethod.PUT, "/api/milestones/{id}", userOf(lecA), Map.of("title", "Giai đoạn 1 (sửa)"), id)
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Giai đoạn 1 (sửa)"));
        call(HttpMethod.DELETE, "/api/milestones/{id}", userOf(lecA), null, id).andExpect(status().isOk());
        assertTrue(milestoneRepo.findById((long) id).isEmpty());
    }

    @Test
    void admin_canManageMilestoneOfAnyTopic() throws Exception {
        post("/api/milestones", admin, Map.of("topicId", topicB.getId(), "title", "Do admin tạo", "deadline", "2030-03-01T23:59")).andExpect(status().isOk());
    }

    @Test
    void departmentAdmin_andStudents_cannotWriteMilestones_byRole() throws Exception {
        Map<String, Object> body = Map.of("topicId", topicA.getId(), "title", "x", "deadline", "2030-01-01T00:00");
        post("/api/milestones", deptAdminA, body).andExpect(status().isForbidden());
        post("/api/milestones", userOf(stuA1), body).andExpect(status().isForbidden());
    }

    @Test
    void createMilestone_requiresATopicId_andAnExistingTopic() throws Exception {
        post("/api/milestones", userOf(lecA), Map.of("title", "thiếu topicId")).andExpect(status().isBadRequest());
        post("/api/milestones", userOf(lecA), Map.of("topicId", 999_999_999L, "title", "x", "deadline", "2030-01-01T00:00"))
                .andExpect(status().isNotFound());
    }

    // ================================================================== MILESTONE — đọc

    @Test
    void milestoneList_isVisibleOnlyToTopicMembers() throws Exception {
        milestoneOn(topicA);
        String url = "/api/milestones?topicId={id}";

        for (User allowed : new User[]{userOf(lecA), userOf(stuA1), admin, deptAdminA}) {
            get(url, allowed, topicA.getId()).andExpect(status().isOk());
        }
        for (User denied : new User[]{userOf(stuA2) /* chờ duyệt */, userOf(stuB1), userOf(lecB), deptAdminB}) {
            get(url, denied, topicA.getId()).andExpect(status().isForbidden());
        }
    }

    @Test
    void singleMilestone_isVisibleOnlyToTopicMembers() throws Exception {
        Milestone m = milestoneOn(topicA);
        String url = "/api/milestones/{id}";

        get(url, userOf(stuA1), m.getId()).andExpect(status().isOk());
        get(url, userOf(lecA), m.getId()).andExpect(status().isOk());
        get(url, userOf(stuA2), m.getId()).andExpect(status().isForbidden());
        get(url, userOf(stuB1), m.getId()).andExpect(status().isForbidden());
        get(url, userOf(lecB), m.getId()).andExpect(status().isForbidden());
        get(url, admin, 999_999_999L).andExpect(status().isNotFound());
    }

    // ================================================================== LỊCH (test #6)

    @Test
    void lecturerB_cannotDeleteOrCreateCalendarEventOnAnotherLecturersTopic() throws Exception {
        CalendarEvent e = eventOn(topicA);

        call(HttpMethod.DELETE, "/api/calendar/{id}", userOf(lecB), null, e.getId()).andExpect(status().isForbidden());
        post("/api/calendar", userOf(lecB), Map.of("topicId", topicA.getId(), "title", "Lạ")).andExpect(status().isForbidden());

        assertTrue(calendarRepo.findById(e.getId()).isPresent(), "sự kiện không được bị xóa");
        assertEquals(1, calendarRepo.findByTopic_IdOrderByStartTimeAsc(topicA.getId()).size());
    }

    @Test
    void supervisingLecturer_canCreateAndDeleteCalendarEvent() throws Exception {
        MvcResult created = post("/api/calendar", userOf(lecA),
                Map.of("topicId", topicA.getId(), "title", "Họp nhóm", "startTime", "2030-01-01T09:00:00"))
                .andExpect(status().isOk()).andReturn();
        int id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        call(HttpMethod.DELETE, "/api/calendar/{id}", userOf(lecA), null, id).andExpect(status().isOk());
        assertTrue(calendarRepo.findById((long) id).isEmpty());
    }

    @Test
    void calendar_isVisibleOnlyToTopicMembers() throws Exception {
        eventOn(topicA);
        String url = "/api/calendar?topicId={id}";

        for (User allowed : new User[]{userOf(lecA), userOf(stuA1), admin, deptAdminA}) {
            get(url, allowed, topicA.getId()).andExpect(status().isOk());
        }
        for (User denied : new User[]{userOf(stuA2), userOf(stuB1), userOf(lecB), deptAdminB}) {
            get(url, denied, topicA.getId()).andExpect(status().isForbidden());
        }
    }

    @Test
    void students_cannotWriteCalendar_byRole() throws Exception {
        post("/api/calendar", userOf(stuA1), Map.of("topicId", topicA.getId(), "title", "x")).andExpect(status().isForbidden());
    }
}
