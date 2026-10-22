package com.doanltmmt.Backend.dto.response;

import com.doanltmmt.Backend.entity.DiscussionPost;
import java.time.LocalDateTime;

public record DiscussionPostDTO(Long id, String content, LocalDateTime createdAt, Long threadId,
                                UserSummaryDTO author) {
    public static DiscussionPostDTO from(DiscussionPost p) {
        return p == null ? null : new DiscussionPostDTO(p.getId(), p.getContent(), p.getCreatedAt(),
                p.getThread() == null ? null : p.getThread().getId(), UserSummaryDTO.from(p.getAuthor()));
    }
}
