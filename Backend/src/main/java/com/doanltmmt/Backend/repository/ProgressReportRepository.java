package com.doanltmmt.Backend.repository;

import com.doanltmmt.Backend.entity.ProgressReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProgressReportRepository extends JpaRepository<ProgressReport, Long> {
    List<ProgressReport> findByStudent_IdOrderByCreatedAtDesc(Long studentId);
    List<ProgressReport> findByTopic_IdOrderByCreatedAtDesc(Long topicId);
    List<ProgressReport> findByStudent_IdAndTopic_IdOrderByCreatedAtDesc(Long studentId, Long topicId);
    ProgressReport findByFileUrlEndingWith(String filename);
}
