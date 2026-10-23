package com.doanltmmt.Backend.repository;

import com.doanltmmt.Backend.entity.TopicRegistration;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;

public interface TopicRegistrationRepository extends JpaRepository<TopicRegistration, Long> {

    /** SV đã được duyệt, workspace đang thực hiện, chưa gửi báo cáo tiến độ nào kể từ {@code since}. */
    @Query("""
            select r from TopicRegistration r
              join fetch r.student s join fetch s.user join fetch r.topic t
            where r.approved = true and t.workspace.status = com.doanltmmt.Backend.entity.enums.WorkspaceStatus.IN_PROGRESS
              and not exists (select p.id from ProgressReport p
                              where p.student = r.student and p.topic = r.topic and p.createdAt >= :since)
            """)
    List<TopicRegistration> findApprovedWithoutProgressSince(@Param("since") LocalDateTime since);

    List<TopicRegistration> findByTopic_IdAndApprovedTrue(Long topicId);

    long countByTopic_Workspace_Department_IdAndApprovedTrue(Long departmentId);

    /** Khóa dòng đăng ký khi chấm điểm / gửi yêu cầu chấm lại để hai request đồng thời không chồng nhau. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from TopicRegistration r where r.id = :id")
    Optional<TopicRegistration> findByIdForUpdate(@Param("id") Long id);

    List<TopicRegistration> findByTopic_Id(Long topicId);

    Optional<TopicRegistration> findTopByStudent_IdAndTopic_IdOrderByRegisteredAtDesc(Long studentId, Long topicId);

    List<TopicRegistration> findByStudent_IdOrderByRegisteredAtDesc(Long studentId);

    boolean existsByTopic_IdAndApprovedTrue(Long topicId);
    boolean existsByStudent_IdAndTopic_IdAndApprovedTrue(Long studentId, Long topicId);
    /** Sinh viên đã đăng ký (bất kể trạng thái) một đề tài do giảng viên này hướng dẫn. */
    boolean existsByTopic_Lecturer_IdAndStudent_Id(Long lecturerId, Long studentId);
    boolean existsByStudent_IdAndTopic_Workspace_IdAndApprovedIsNull(Long studentId, Long workspaceId);
    boolean existsByStudent_IdAndTopic_Workspace_IdAndApprovedTrue(Long studentId, Long workspaceId);

    long countByTopic_Id(Long topicId);

    long countByTopic_IdAndApprovedIsNull(Long topicId);
    long countByTopic_IdAndApprovedTrue(Long topicId);
    long countByTopic_IdAndApprovedTrueAndScoreIsNull(Long topicId);
    long countByTopic_IdAndApprovedFalse(Long topicId);

}
