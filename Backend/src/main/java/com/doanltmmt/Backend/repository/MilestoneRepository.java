package com.doanltmmt.Backend.repository;

import com.doanltmmt.Backend.entity.Milestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MilestoneRepository extends JpaRepository<Milestone, Long> {
    List<Milestone> findByTopic_IdOrderByDeadlineAsc(Long topicId);

    /** Milestone có hạn trong [from, to) của đề tài thuộc workspace đang thực hiện. */
    @Query("""
            select m from Milestone m join fetch m.topic t
            where m.deadline >= :from and m.deadline < :to and t.workspace.status = 'IN_PROGRESS'
            """)
    List<Milestone> findDueBetweenInActiveWorkspaces(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
