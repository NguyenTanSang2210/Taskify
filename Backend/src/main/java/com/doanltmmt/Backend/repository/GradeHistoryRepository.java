package com.doanltmmt.Backend.repository;

import com.doanltmmt.Backend.entity.GradeHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface GradeHistoryRepository extends JpaRepository<GradeHistory, Long> {

    List<GradeHistory> findByRegistration_IdOrderByChangedAtDescIdDesc(Long registrationId);

    /** Số lần sửa một điểm đã có (không tính lần chấm đầu tiên). */
    long countByRegistration_IdAndOldScoreIsNotNull(Long registrationId);

    /** [registrationId, số lần sửa điểm] cho mọi đăng ký có ít nhất một lần sửa — dùng khi xuất Excel. */
    @Query("select h.registration.id, count(h) from GradeHistory h where h.oldScore is not null group by h.registration.id")
    List<Object[]> countRevisionsPerRegistration();

    Optional<GradeHistory> findFirstByRegistration_IdAndOldScoreIsNotNullOrderByChangedAtDescIdDesc(Long registrationId);
}
