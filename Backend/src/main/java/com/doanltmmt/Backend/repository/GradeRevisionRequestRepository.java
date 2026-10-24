package com.doanltmmt.Backend.repository;

import com.doanltmmt.Backend.entity.enums.GradeRevisionStatus;
import com.doanltmmt.Backend.entity.GradeRevisionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface GradeRevisionRequestRepository extends JpaRepository<GradeRevisionRequest, Long> {

    Optional<GradeRevisionRequest> findFirstByRegistration_IdOrderByIdDesc(Long registrationId);

    boolean existsByRegistration_IdAndStatus(Long registrationId, GradeRevisionStatus status);

    /** Yêu cầu đã duyệt và còn trong thời gian mở khóa. */
    Optional<GradeRevisionRequest> findFirstByRegistration_IdAndStatusAndUnlockUntilAfterOrderByIdDesc(
            Long registrationId, GradeRevisionStatus status, LocalDateTime now);

    List<GradeRevisionRequest> findByStatusAndUnlockUntilBefore(GradeRevisionStatus status, LocalDateTime now);

    /** Danh sách cho người duyệt; {@code null} = không lọc theo trường đó. */
    @Query("""
            select r from GradeRevisionRequest r
            where (:status is null or r.status = :status)
              and (:departmentId is null or r.registration.topic.workspace.department.id = :departmentId)
            """)
    Page<GradeRevisionRequest> search(@Param("status") GradeRevisionStatus status, @Param("departmentId") Long departmentId,
                                      Pageable pageable);
}
