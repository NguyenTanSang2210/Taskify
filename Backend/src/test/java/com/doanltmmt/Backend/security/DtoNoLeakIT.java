package com.doanltmmt.Backend.security;

import com.doanltmmt.Backend.entity.DiscussionPost;
import com.doanltmmt.Backend.entity.DiscussionThread;
import com.doanltmmt.Backend.entity.Milestone;
import com.doanltmmt.Backend.entity.Notification;
import com.doanltmmt.Backend.entity.ProgressReport;
import com.doanltmmt.Backend.repository.DiscussionPostRepository;
import com.doanltmmt.Backend.repository.DiscussionThreadRepository;
import com.doanltmmt.Backend.repository.MilestoneRepository;
import com.doanltmmt.Backend.repository.NotificationRepository;
import com.doanltmmt.Backend.repository.ProgressReportRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test bắt buộc #18: phản hồi không còn rò email / SĐT / quyền / mật khẩu của người dùng,
 * đồng thời giữ đúng các đường dẫn JSON mà frontend đang đọc.
 */
@SuppressWarnings("null")
class DtoNoLeakIT extends AbstractTopicScenarioIT {

    @Autowired private ProgressReportRepository progressRepo;
    @Autowired private DiscussionThreadRepository threadRepo;
    @Autowired private DiscussionPostRepository postRepo;
    @Autowired private NotificationRepository notificationRepo;
    @Autowired private MilestoneRepository milestoneRepo;

    private static final String[] FORBIDDEN = {"\"email\"", "\"phone\"", "\"privileges\"", "\"password\"", "@"};

    private String body(ResultActions r) throws Exception {
        return r.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private void assertClean(String json) {
        for (String bad : FORBIDDEN) {
            assertFalse(json.contains(bad), "Phản hồi không được chứa " + bad + ": " + json);
        }
    }

    @Test
    void progress_hasNoPiiAndKeepsFrontendPaths() throws Exception {
        ProgressReport pr = new ProgressReport();
        pr.setTitle("BC1");
        pr.setContent("nội dung");
        pr.setStatus("TODO");
        pr.setTopic(topicA);
        pr.setStudent(stuA1);
        progressRepo.saveAndFlush(pr);

        ResultActions r = get("/api/progress/topic/{id}", userOf(lecA), topicA.getId());
        String json = body(r);
        assertClean(json);
        get("/api/progress/topic/{id}", userOf(lecA), topicA.getId())
                .andExpect(jsonPath("$[0].student.user.fullName").exists())
                .andExpect(jsonPath("$[0].student.studentCode").exists())
                .andExpect(jsonPath("$[0].topic.id").value(topicA.getId()))
                .andExpect(jsonPath("$[0].topic.lecturer").doesNotExist());
    }

    @Test
    void discussionAndMilestone_haveNoPii() throws Exception {
        DiscussionThread t = new DiscussionThread();
        t.setTopic(topicA);
        t.setTitle("Chủ đề");
        t.setCreatedBy(userOf(lecA));
        t = threadRepo.saveAndFlush(t);
        DiscussionPost p = new DiscussionPost();
        p.setThread(t);
        p.setAuthor(userOf(stuA1));
        p.setContent("xin chào");
        postRepo.saveAndFlush(p);
        Milestone m = new Milestone();
        m.setTopic(topicA);
        m.setTitle("Mốc 1");
        m.setDeadline(LocalDateTime.now().plusDays(3));
        milestoneRepo.saveAndFlush(m);

        assertClean(body(get("/api/discuss/threads?topicId={id}", userOf(stuA1), topicA.getId())));
        assertClean(body(get("/api/discuss/threads/{id}/posts", userOf(stuA1), t.getId())));
        assertClean(body(get("/api/milestones?topicId={id}", userOf(stuA1), topicA.getId())));
        get("/api/discuss/threads/{id}/posts", userOf(stuA1), t.getId())
                .andExpect(jsonPath("$[0].author.fullName").exists());
        get("/api/milestones/{id}", userOf(stuA1), m.getId())
                .andExpect(jsonPath("$.topic.id").value(topicA.getId()))
                .andExpect(jsonPath("$.topic.title").exists());
    }

    @Test
    void notifications_haveNoUserObject() throws Exception {
        Notification n = new Notification();
        n.setUser(userOf(stuA1));
        n.setType("INFO");
        n.setTitle("Tiêu đề");
        n.setContent("Nội dung");
        notificationRepo.saveAndFlush(n);

        String json = body(get("/api/notifications/mine?userId={id}", userOf(stuA1), userOf(stuA1).getId()));
        assertClean(json);
        assertTrue(json.contains("Tiêu đề"));
        assertFalse(json.contains("\"user\""));
    }
}
