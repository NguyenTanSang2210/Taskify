package com.doanltmmt.Backend.repository;

import com.doanltmmt.Backend.entity.Milestone;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MilestoneRepository extends JpaRepository<Milestone, Long> {
    List<Milestone> findByTopic_IdOrderByDeadlineAsc(Long topicId);
}
