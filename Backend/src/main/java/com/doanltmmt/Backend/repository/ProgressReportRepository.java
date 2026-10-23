package com.doanltmmt.Backend.repository;

import com.doanltmmt.Backend.entity.ProgressReport;
import com.doanltmmt.Backend.service.SimilarityService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProgressReportRepository extends JpaRepository<ProgressReport, Long> {
    @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = {"student", "student.user", "topic", "milestone"})
    List<ProgressReport> findByStudent_IdOrderByCreatedAtDesc(Long studentId);
    @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = {"student", "student.user", "topic", "milestone"})
    Page<ProgressReport> findByTopic_Id(Long topicId, Pageable pageable);
    @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = {"student", "student.user", "topic", "milestone"})
    List<ProgressReport> findByStudent_IdAndTopic_IdOrderByCreatedAtDesc(Long studentId, Long topicId);

    /** Nội dung các báo cáo cùng workspace — ứng viên so trùng lặp (chỉ lấy id + nội dung). */
    @Query("select new com.doanltmmt.Backend.service.SimilarityService$Candidate(p.id, p.content) "
            + "from ProgressReport p where p.topic.workspace.id = :workspaceId")
    List<SimilarityService.Candidate> findSimilarityCandidatesInWorkspace(@Param("workspaceId") Long workspaceId);

    @Query("select new com.doanltmmt.Backend.service.SimilarityService$Candidate(p.id, p.content) "
            + "from ProgressReport p where p.topic.id = :topicId")
    List<SimilarityService.Candidate> findSimilarityCandidatesInTopic(@Param("topicId") Long topicId);
}
