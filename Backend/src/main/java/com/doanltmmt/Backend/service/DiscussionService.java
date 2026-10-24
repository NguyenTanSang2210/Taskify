package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.dto.request.DiscussionPostRequest;
import com.doanltmmt.Backend.dto.request.DiscussionThreadRequest;
import com.doanltmmt.Backend.dto.response.*;
import com.doanltmmt.Backend.dto.response.PageResponse;
import com.doanltmmt.Backend.entity.DiscussionPost;
import com.doanltmmt.Backend.entity.DiscussionThread;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.repository.DiscussionPostRepository;
import com.doanltmmt.Backend.repository.DiscussionThreadRepository;
import com.doanltmmt.Backend.util.Pages;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Thảo luận theo chủ đề trong phạm vi đề tài. */
@Service
@SuppressWarnings("null")
public class DiscussionService {

    private final DiscussionThreadRepository threadRepo;
    private final DiscussionPostRepository postRepo;
    private final AuditLogService auditLogService;
    private final SecurityScopeService scope;
    private final TopicAccessService topicAccess;

    public DiscussionService(DiscussionThreadRepository threadRepo,
                             DiscussionPostRepository postRepo,
                             AuditLogService auditLogService,
                             SecurityScopeService scope,
                             TopicAccessService topicAccess) {
        this.threadRepo = threadRepo;
        this.postRepo = postRepo;
        this.auditLogService = auditLogService;
        this.scope = scope;
        this.topicAccess = topicAccess;
    }

    @Transactional(readOnly = true)
    public List<DiscussionThreadDTO> listThreads(Long topicId) {
        Topic topic = topicAccess.loadTopicOr404(topicId);
        topicAccess.requireMember(topic);
        return threadRepo.findByTopic_IdOrderByCreatedAtDesc(topicId).stream().map(DiscussionThreadDTO::from).toList();
    }

    @Transactional
    public DiscussionThreadDTO createThread(DiscussionThreadRequest body) {
        Topic topic = topicAccess.loadTopicOr404(body.topicId());
        topicAccess.requireMember(topic);
        String title = body.title();

        User user = scope.requireCurrentUser();
        DiscussionThread t = new DiscussionThread();
        t.setTopic(topic);
        t.setTitle(title);
        t.setCreatedBy(user);
        DiscussionThread saved = threadRepo.save(t);
        auditLogService.log("CREATE_THREAD", "DiscussionThread", saved.getId().toString(), "Title: " + saved.getTitle());
        return DiscussionThreadDTO.from(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<DiscussionPostDTO> listPosts(Long threadId, int page, int size) {
        DiscussionThread th = loadThread(threadId);
        topicAccess.requireMember(th.getTopic());
        // cũ → mới (đọc như một cuộc trò chuyện); trang 0 là các bài đầu tiên
        return PageResponse.of(postRepo.findByThread_Id(threadId, Pages.of(page, size, Sort.by("createdAt", "id"))),
                DiscussionPostDTO::from);
    }

    @Transactional
    public DiscussionPostDTO addPost(Long threadId, DiscussionPostRequest body) {
        DiscussionThread th = loadThread(threadId);
        topicAccess.requireMember(th.getTopic());
        String content = body.content();

        User user = scope.requireCurrentUser();
        DiscussionPost p = new DiscussionPost();
        p.setThread(th);
        p.setAuthor(user);
        p.setContent(content);
        DiscussionPost saved = postRepo.save(p);
        auditLogService.log("CREATE_POST", "DiscussionPost", saved.getId().toString(), "Thread " + th.getId());
        return DiscussionPostDTO.from(saved);
    }

    private DiscussionThread loadThread(Long threadId) {
        return threadRepo.findById(threadId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy chủ đề thảo luận"));
    }
}
