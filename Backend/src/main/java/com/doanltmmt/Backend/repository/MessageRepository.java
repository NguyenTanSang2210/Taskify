package com.doanltmmt.Backend.repository;

import com.doanltmmt.Backend.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

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

    @Query(value = """
            select
                m.id as id,
                s.id as senderId,
                coalesce(s.full_name, s.username) as senderName,
                r.id as recipientId,
                m.topic_id as topicId,
                m.content as content,
                m.created_at as createdAt
            from messages m
            join users s on s.id = m.sender_id
            join users r on r.id = m.recipient_id
            where (m.sender_id = :userId or m.recipient_id = :userId)
              and exists (select 1 from workspaces w where w.id = m.workspace_id)
            order by m.created_at desc
            """, nativeQuery = true)
    List<InboxRow> findInboxRowsByUserId(@Param("userId") Long userId);
}
