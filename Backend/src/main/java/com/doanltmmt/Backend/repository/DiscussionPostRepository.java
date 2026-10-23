package com.doanltmmt.Backend.repository;

import com.doanltmmt.Backend.entity.DiscussionPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiscussionPostRepository extends JpaRepository<DiscussionPost, Long> {
    @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = {"author", "thread"})
    Page<DiscussionPost> findByThread_Id(Long threadId, Pageable pageable);
}
