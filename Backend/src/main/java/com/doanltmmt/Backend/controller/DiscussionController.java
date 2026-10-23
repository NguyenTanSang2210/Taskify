package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.dto.request.DiscussionPostRequest;
import com.doanltmmt.Backend.dto.request.DiscussionThreadRequest;
import com.doanltmmt.Backend.dto.response.*;
import com.doanltmmt.Backend.service.DiscussionService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Thảo luận theo đề tài: chỉ thành viên đề tài (đã được duyệt / giảng viên hướng dẫn / quản trị) xem và đăng bài. */
@RestController
@RequestMapping("/api/discuss")
public class DiscussionController {

    private final DiscussionService service;

    public DiscussionController(DiscussionService service) {
        this.service = service;
    }

    @GetMapping("/threads")
    @PreAuthorize("isAuthenticated()")
    public List<DiscussionThreadDTO> listThreads(@RequestParam Long topicId) {
        return service.listThreads(topicId);
    }

    @PostMapping("/threads")
    @PreAuthorize("isAuthenticated()")
    public DiscussionThreadDTO createThread(@Valid @RequestBody DiscussionThreadRequest body) {
        return service.createThread(body);
    }

    @GetMapping("/threads/{threadId}/posts")
    @PreAuthorize("isAuthenticated()")
    public List<DiscussionPostDTO> listPosts(@PathVariable Long threadId) {
        return service.listPosts(threadId);
    }

    @PostMapping("/threads/{threadId}/posts")
    @PreAuthorize("isAuthenticated()")
    public DiscussionPostDTO addPost(@PathVariable Long threadId, @Valid @RequestBody DiscussionPostRequest body) {
        return service.addPost(threadId, body);
    }
}
