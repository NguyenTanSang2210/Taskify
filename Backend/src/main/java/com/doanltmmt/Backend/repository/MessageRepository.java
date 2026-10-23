package com.doanltmmt.Backend.repository;

import com.doanltmmt.Backend.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface MessageRepository extends JpaRepository<Message, Long> {
    interface InboxRow {
        Long getId();
        Long getSenderId();
        String getSenderName();
        Long getRecipientId();
        Long getTopicId();
        String getContent();
        LocalDateTime getCreatedAt();
    }

    /** JPQL (không dùng SQL thuần) để thời gian được quy đổi UTC → giờ ứng dụng như mọi truy vấn khác. */
    @Query(value = """
            select m.id as id, s.id as senderId, coalesce(s.fullName, s.username) as senderName,
                   r.id as recipientId, t.id as topicId, m.content as content, m.createdAt as createdAt
            from Message m join m.sender s join m.recipient r join m.workspace w left join m.topic t
            where s.id = :userId or r.id = :userId
            """,
            countQuery = "select count(m) from Message m where m.sender.id = :userId or m.recipient.id = :userId")
    Page<InboxRow> findInboxRowsByUserId(@Param("userId") Long userId, Pageable pageable);

    /** Hội thoại giữa hai người (tùy chọn: trong một đề tài). */
    @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = {"sender", "recipient", "topic"})
    @Query("""
            select m from Message m
            where ((m.sender.id = :me and m.recipient.id = :peer) or (m.sender.id = :peer and m.recipient.id = :me))
              and (:topicId is null or m.topic.id = :topicId)
            """)
    Page<Message> findConversation(@Param("me") Long me, @Param("peer") Long peer, @Param("topicId") Long topicId,
                                   Pageable pageable);
}
