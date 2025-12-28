package com.doanltmmt.Backend.repository;

import com.doanltmmt.Backend.entity.DiscussionThread;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DiscussionThreadRepository extends JpaRepository<DiscussionThread, Long> {
    List<DiscussionThread> findByTopic_IdOrderByCreatedAtDesc(Long topicId);
}
