package com.doanltmmt.Backend.repository;

import com.doanltmmt.Backend.entity.ProgressReport;
import com.doanltmmt.Backend.service.SimilarityService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProgressReportRepository extends JpaRepository<ProgressReport, Long> {
    List<ProgressReport> findByStudent_IdOrderByCreatedAtDesc(Long studentId);
    List<ProgressReport> findByTopic_IdOrderByCreatedAtDesc(Long topicId);
    List<ProgressReport> findByStudent_IdAndTopic_IdOrderByCreatedAtDesc(Long studentId, Long topicId);

    /** Nội dung các báo cáo cùng workspace — ứng viên so trùng lặp (chỉ lấy id + nội dung). */
    @Query("select new com.doanltmmt.Backend.service.SimilarityService$Candidate(p.id, p.content) "
            + "from ProgressReport p where p.topic.workspace.id = :workspaceId")
    List<SimilarityService.Candidate> findSimilarityCandidatesInWorkspace(@Param("workspaceId") Long workspaceId);

    @Query("select new com.doanltmmt.Backend.service.SimilarityService$Candidate(p.id, p.content) "
            + "from ProgressReport p where p.topic.id = :topicId")
    List<SimilarityService.Candidate> findSimilarityCandidatesInTopic(@Param("topicId") Long topicId);
}
