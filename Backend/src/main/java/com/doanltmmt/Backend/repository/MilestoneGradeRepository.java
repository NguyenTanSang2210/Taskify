package com.doanltmmt.Backend.repository;

import com.doanltmmt.Backend.entity.MilestoneGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MilestoneGradeRepository extends JpaRepository<MilestoneGrade, Long> {

    Optional<MilestoneGrade> findByMilestone_IdAndStudent_Id(Long milestoneId, Long studentId);

    @Query("select g from MilestoneGrade g where g.milestone.topic.id = :topicId")
    List<MilestoneGrade> findByTopic(@Param("topicId") Long topicId);
}
