package com.doanltmmt.Backend.repository;

import com.doanltmmt.Backend.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findBySender_IdOrRecipient_IdOrderByCreatedAtDesc(Long senderId, Long recipientId);
}
