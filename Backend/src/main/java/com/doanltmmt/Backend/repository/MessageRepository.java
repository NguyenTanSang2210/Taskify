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

    /** JPQL (không dùng SQL thuần) để thời gian được quy đổi UTC → giờ ứng dụng như mọi truy vấn khác. */
    @Query("""
            select m.id as id, s.id as senderId, coalesce(s.fullName, s.username) as senderName,
                   r.id as recipientId, t.id as topicId, m.content as content, m.createdAt as createdAt
            from Message m join m.sender s join m.recipient r join m.workspace w left join m.topic t
            where s.id = :userId or r.id = :userId
            order by m.createdAt desc
            """)
    List<InboxRow> findInboxRowsByUserId(@Param("userId") Long userId);
}
