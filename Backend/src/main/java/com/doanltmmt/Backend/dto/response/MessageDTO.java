package com.doanltmmt.Backend.dto.response;

import com.doanltmmt.Backend.entity.Message;
import java.time.LocalDateTime;

/** Tin nhắn; {@code topic} là đề tài của cuộc trò chuyện (giao diện lọc tin theo đề tài đang chọn). */
public record MessageDTO(Long id, String content, LocalDateTime createdAt,
                         UserSummaryDTO sender, UserSummaryDTO recipient, TopicRefDTO topic) {
    public static MessageDTO from(Message m) {
        return m == null ? null : new MessageDTO(m.getId(), m.getContent(), m.getCreatedAt(),
                UserSummaryDTO.from(m.getSender()), UserSummaryDTO.from(m.getRecipient()), TopicRefDTO.from(m.getTopic()));
    }
}
