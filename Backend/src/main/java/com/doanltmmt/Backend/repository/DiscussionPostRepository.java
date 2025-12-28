package com.doanltmmt.Backend.repository;

import com.doanltmmt.Backend.entity.DiscussionPost;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DiscussionPostRepository extends JpaRepository<DiscussionPost, Long> {
    List<DiscussionPost> findByThread_IdOrderByCreatedAtAsc(Long threadId);
}
