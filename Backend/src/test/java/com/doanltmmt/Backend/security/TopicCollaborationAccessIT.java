package com.doanltmmt.Backend.security;

import com.doanltmmt.Backend.entity.DiscussionThread;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.repository.DiscussionThreadRepository;
import com.doanltmmt.Backend.repository.MessageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Thảo luận, tin nhắn, nhật ký hệ thống: người ngoài đề tài không đọc/ghi được (test bắt buộc #7, #8, #10).
 */
@SuppressWarnings("null")
class TopicCollaborationAccessIT extends AbstractTopicScenarioIT {

    @Autowired private DiscussionThreadRepository threadRepo;
    @Autowired private MessageRepository messageRepo;

    private DiscussionThread threadOn(com.doanltmmt.Backend.entity.Topic topic) {
        DiscussionThread t = new DiscussionThread();
        t.setTopic(topic);
        t.setTitle("Chủ đề thử nghiệm");
        t.setCreatedBy(userOf(lecA));
        return threadRepo.saveAndFlush(t);
    }

    // ================================================================== THẢO LUẬN (#7)

    @Test
    void discussion_outsidersCannotListThreads() throws Exception {
        threadOn(topicA);
        for (User denied : new User[]{userOf(stuA2), userOf(stuB1), userOf(lecB), deptAdminB}) {
            get("/api/discuss/threads?topicId={id}", denied, topicA.getId()).andExpect(status().isForbidden());
        }
        for (User allowed : new User[]{userOf(stuA1), userOf(lecA), admin, deptAdminA}) {
            get("/api/discuss/threads?topicId={id}", allowed, topicA.getId()).andExpect(status().isOk());
        }
    }

    @Test
    void discussion_outsidersCannotCreateThread() throws Exception {
        post("/api/discuss/threads", userOf(stuB1), Map.of("topicId", topicA.getId(), "title", "Xâm nhập")).andExpect(status().isForbidden());
        post("/api/discuss/threads", userOf(stuA2), Map.of("topicId", topicA.getId(), "title", "Chờ duyệt")).andExpect(status().isForbidden());
        assertEquals(0, threadRepo.findByTopic_IdOrderByCreatedAtDesc(topicA.getId()).size());

        post("/api/discuss/threads", userOf(stuA1), Map.of("topicId", topicA.getId(), "title", "Hợp lệ")).andExpect(status().isOk());
    }

    @Test
    void discussion_outsidersCannotReadOrWritePosts() throws Exception {
        DiscussionThread t = threadOn(topicA);
        String url = "/api/discuss/threads/{id}/posts";

        get(url, userOf(stuB1), t.getId()).andExpect(status().isForbidden());
        post(url, userOf(stuB1), Map.of("content", "spam"), t.getId()).andExpect(status().isForbidden());
        post(url, userOf(lecB), Map.of("content", "spam"), t.getId()).andExpect(status().isForbidden());

        post(url, userOf(stuA1), Map.of("content", "ok"), t.getId()).andExpect(status().isOk());
        get(url, userOf(lecA), t.getId()).andExpect(status().isOk());
        get(url, admin, 999_999_999L).andExpect(status().isNotFound());
    }

    // ================================================================== TIN NHẮN (#8)

    @Test
    void messages_cannotBeSentToSomeoneOutsideTheTopic() throws Exception {
        // sinh viên khoa B (không thuộc đề tài A) là người nhận → bị chặn dù người gửi hợp lệ
        post("/api/messages/send", userOf(stuA1),
                Map.of("recipientId", userOf(stuB1).getId(), "topicId", topicA.getId(), "content", "xin chào"))
                .andExpect(status().isForbidden());
        // sinh viên đang chờ duyệt cũng chưa phải thành viên
        post("/api/messages/send", userOf(lecA),
                Map.of("recipientId", userOf(stuA2).getId(), "topicId", topicA.getId(), "content", "xin chào"))
                .andExpect(status().isForbidden());
        assertEquals(0, messageRepo.count());
    }

    @Test
    void messages_cannotBeSentByANonMember() throws Exception {
        post("/api/messages/send", userOf(stuB1),
                Map.of("recipientId", userOf(lecA).getId(), "topicId", topicA.getId(), "content", "xin chào"))
                .andExpect(status().isForbidden());
        post("/api/messages/send", userOf(lecB),
                Map.of("recipientId", userOf(stuA1).getId(), "topicId", topicA.getId(), "content", "xin chào"))
                .andExpect(status().isForbidden());
        assertEquals(0, messageRepo.count());
    }

    @Test
    void messages_betweenTopicMembersAreDelivered() throws Exception {
        post("/api/messages/send", userOf(stuA1),
                Map.of("recipientId", userOf(lecA).getId(), "topicId", topicA.getId(), "content", "Em chào thầy"))
                .andExpect(status().isOk());
        post("/api/messages/send", userOf(lecA),
                Map.of("recipientId", userOf(stuA1).getId(), "topicId", topicA.getId(), "content", "Chào em"))
                .andExpect(status().isOk());
        assertEquals(2, messageRepo.count());
    }

    @Test
    void messages_validateInput() throws Exception {
        Object recipient = userOf(lecA).getId();
        post("/api/messages/send", userOf(stuA1), Map.of("recipientId", recipient, "topicId", topicA.getId(), "content", "  "))
                .andExpect(status().isBadRequest());
        post("/api/messages/send", userOf(stuA1), Map.of("recipientId", recipient, "content", "thiếu topic"))
                .andExpect(status().isBadRequest());
        post("/api/messages/send", userOf(stuA1), Map.of("recipientId", recipient, "topicId", 999_999_999L, "content", "x"))
                .andExpect(status().isNotFound());
        post("/api/messages/send", userOf(stuA1),
                Map.of("recipientId", recipient, "topicId", topicA.getId(), "content", "a".repeat(5001)))
                .andExpect(status().isBadRequest());
    }

    // ================================================================== NHẬT KÝ (#10) + XUẤT EXCEL

    @Test
    void auditLog_isAdminOnly() throws Exception {
        for (User denied : new User[]{deptAdminA, userOf(lecA), userOf(stuA1)}) {
            get("/api/audit/recent", denied).andExpect(status().isForbidden());
        }
        get("/api/audit/recent", admin).andExpect(status().isOk());
    }

    @Test
    void export_isDeniedToStudents() throws Exception {
        get("/api/export/excel", userOf(stuA1)).andExpect(status().isForbidden());
        call(HttpMethod.GET, "/api/export/excel", admin, null).andExpect(status().isOk());
    }
}
