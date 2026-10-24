package com.doanltmmt.Backend.repository;

import com.doanltmmt.Backend.entity.TopicRegistration;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
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

    @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = {"student", "student.user", "topic", "topic.lecturer", "topic.lecturer.user", "topic.workspace", "reviewer", "reviewer.user"})
    List<TopicRegistration> findByTopic_Id(Long topicId);

    Optional<TopicRegistration> findTopByStudent_IdAndTopic_IdOrderByRegisteredAtDesc(Long studentId, Long topicId);

    @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = {"student", "student.user", "topic", "topic.lecturer", "topic.lecturer.user", "topic.workspace", "reviewer", "reviewer.user"})
    List<TopicRegistration> findByStudent_IdOrderByRegisteredAtDesc(Long studentId);

    boolean existsByTopic_IdAndApprovedTrue(Long topicId);
    boolean existsByStudent_IdAndTopic_IdAndApprovedTrue(Long studentId, Long topicId);
    /** Sinh viên đã đăng ký (bất kể trạng thái) một đề tài do giảng viên này hướng dẫn. */
    boolean existsByTopic_Lecturer_IdAndStudent_Id(Long lecturerId, Long studentId);
    boolean existsByStudent_IdAndTopic_Workspace_IdAndApprovedIsNull(Long studentId, Long workspaceId);
    boolean existsByStudent_IdAndTopic_Workspace_IdAndApprovedTrue(Long studentId, Long workspaceId);

    long countByTopic_IdAndApprovedTrue(Long topicId);
    long countByTopic_IdAndApprovedTrueAndScoreIsNull(Long topicId);

    /** Số đăng ký theo trạng thái của nhiều đề tài trong MỘT câu (trang danh sách đề tài). */
    interface TopicRegistrationCounts {
        Long getTopicId();
        Long getTotal();
        Long getPending();
        Long getApproved();
        Long getRejected();
    }

    @Query("""
            select r.topic.id as topicId, count(r) as total,
                   sum(case when r.approved is null then 1 else 0 end) as pending,
                   sum(case when r.approved = true then 1 else 0 end) as approved,
                   sum(case when r.approved = false then 1 else 0 end) as rejected
            from TopicRegistration r where r.topic.id in :topicIds group by r.topic.id
            """)
    List<TopicRegistrationCounts> countByTopicIds(@Param("topicIds") Collection<Long> topicIds);

    /** Trạng thái đăng ký của MỘT sinh viên với nhiều đề tài; bản ghi mới nhất của mỗi đề tài đứng trước. */
    interface StudentRegistrationStatus {
        Long getTopicId();
        Boolean getApproved();
    }

    @Query("""
            select r.topic.id as topicId, r.approved as approved from TopicRegistration r
            where r.student.id = :studentId and r.topic.id in :topicIds
            order by r.registeredAt desc, r.id desc
            """)
    List<StudentRegistrationStatus> findStatusesOfStudent(@Param("studentId") Long studentId,
                                                          @Param("topicIds") Collection<Long> topicIds);
}
