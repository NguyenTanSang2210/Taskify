package com.doanltmmt.Backend.repository;

import com.doanltmmt.Backend.entity.Milestone;
import com.doanltmmt.Backend.entity.TopicRegistration;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Truy vấn tổng hợp cho dashboard (P4-01) — mỗi số liệu là MỘT câu GROUP BY/COUNT, không nạp entity.
 * Phạm vi: {@code deptId} / {@code wsId} null = không lọc theo khoa / đợt.
 */
public interface DashboardQueryRepository extends Repository<TopicRegistration, Long> {

    String TOPIC_SCOPE = "(:deptId is null or t.workspace.department.id = :deptId) and (:wsId is null or t.workspace.id = :wsId)";

    // ------------------------------------------------------------------ tổng quan (ADMIN / quản trị khoa)

    @Query("select t.status, count(t) from Topic t where " + TOPIC_SCOPE + " group by t.status")
    List<Object[]> countTopicsByStatus(@Param("deptId") Long deptId, @Param("wsId") Long wsId);

    /** [approved (null/true/false), số đăng ký]. */
    @Query("select r.approved, count(r) from TopicRegistration r join r.topic t where " + TOPIC_SCOPE + " group by r.approved")
    List<Object[]> countRegistrationsByApproval(@Param("deptId") Long deptId, @Param("wsId") Long wsId);

    @Query("select count(distinct r.student.id) from TopicRegistration r join r.topic t where r.approved = true and " + TOPIC_SCOPE)
    long countStudentsWithApprovedTopic(@Param("deptId") Long deptId, @Param("wsId") Long wsId);

    /** [điểm làm tròn xuống (0..10), số SV]. */
    @Query("select floor(r.score), count(r) from TopicRegistration r join r.topic t where r.approved = true and r.score is not null and "
            + TOPIC_SCOPE + " group by floor(r.score)")
    List<Object[]> scoreHistogram(@Param("deptId") Long deptId, @Param("wsId") Long wsId);

    @Query("select avg(r.score) from TopicRegistration r join r.topic t where r.approved = true and r.score is not null and " + TOPIC_SCOPE)
    Double averageScore(@Param("deptId") Long deptId, @Param("wsId") Long wsId);

    /** [tên giảng viên, số đề tài, số SV được duyệt] — nhiều SV nhất trước. */
    @Query("select u.fullName, count(distinct t.id), count(distinct case when r.approved = true then r.id end) "
            + "from Topic t join t.lecturer l join l.user u left join TopicRegistration r on r.topic = t where " + TOPIC_SCOPE
            + " group by l.id, u.fullName order by count(distinct case when r.approved = true then r.id end) desc, u.fullName")
    List<Object[]> lecturerLoad(@Param("deptId") Long deptId, @Param("wsId") Long wsId, Pageable limit);

    @Query("select count(s) from Student s where s.academicClass.id in "
            + "(select wc.academicClass.id from WorkspaceClass wc where wc.workspace.id = :wsId and wc.active = true)")
    long countStudentsInWorkspace(@Param("wsId") Long wsId);

    /** [nộp lúc, nhận xét lần đầu lúc] của các báo cáo đã có thời điểm nhận xét. */
    @Query("select p.createdAt, p.commentedAt from ProgressReport p join p.topic t where p.commentedAt is not null and " + TOPIC_SCOPE)
    List<Object[]> feedbackTimes(@Param("deptId") Long deptId, @Param("wsId") Long wsId);

    @Query("select count(p) from ProgressReport p join p.topic t where (p.lecturerComment is null or trim(p.lecturerComment) = '') and "
            + TOPIC_SCOPE)
    long countUncommentedReports(@Param("deptId") Long deptId, @Param("wsId") Long wsId);

    // ------------------------------------------------------------------ giảng viên

    @Query("select count(r) from TopicRegistration r where r.topic.lecturer.user.id = :userId and r.approved = true")
    long countApprovedForLecturer(@Param("userId") Long userId);

    @Query("select count(r) from TopicRegistration r where r.topic.lecturer.user.id = :userId and r.approved is null")
    long countPendingForLecturer(@Param("userId") Long userId);

    @Query("select count(p) from ProgressReport p where p.topic.lecturer.user.id = :userId "
            + "and (p.lecturerComment is null or trim(p.lecturerComment) = '')")
    long countUncommentedForLecturer(@Param("userId") Long userId);

    /** Sinh viên được duyệt (đợt đang thực hiện) không nộp báo cáo nào từ {@code since}. */
    @Query("""
            select r from TopicRegistration r join fetch r.student s join fetch s.user join fetch r.topic t
            where r.approved = true and t.lecturer.user.id = :userId
              and t.workspace.status = com.doanltmmt.Backend.entity.enums.WorkspaceStatus.IN_PROGRESS
              and not exists (select p.id from ProgressReport p where p.student = r.student and p.topic = r.topic and p.createdAt >= :since)
            order by t.title, s.studentCode
            """)
    List<TopicRegistration> silentStudentsOfLecturer(@Param("userId") Long userId, @Param("since") LocalDateTime since, Pageable limit);

    /** [mã SV (= id user), mã đề tài, lần nộp gần nhất]. */
    @Query("select p.student.id, p.topic.id, max(p.createdAt) from ProgressReport p where p.topic.lecturer.user.id = :userId "
            + "group by p.student.id, p.topic.id")
    List<Object[]> lastReportPerStudent(@Param("userId") Long userId);

    @Query("select m from Milestone m join fetch m.topic t where t.lecturer.user.id = :userId and m.deadline >= :from and m.deadline < :to "
            + "order by m.deadline")
    List<Milestone> upcomingMilestonesOfLecturer(@Param("userId") Long userId, @Param("from") LocalDateTime from,
                                                 @Param("to") LocalDateTime to, Pageable limit);

    // ------------------------------------------------------------------ sinh viên

    @Query("""
            select r from TopicRegistration r join fetch r.topic t left join fetch t.lecturer l left join fetch l.user
              left join fetch t.workspace
            where r.student.id = :studentId order by r.registeredAt desc
            """)
    List<TopicRegistration> registrationsOfStudent(@Param("studentId") Long studentId);

    @Query("select p.status, count(p) from ProgressReport p where p.student.id = :studentId and p.topic.id = :topicId group by p.status")
    List<Object[]> progressByStatus(@Param("studentId") Long studentId, @Param("topicId") Long topicId);

    @Query("select m from Milestone m where m.topic.id = :topicId and m.deadline >= :from order by m.deadline")
    List<Milestone> upcomingMilestonesOfTopic(@Param("topicId") Long topicId, @Param("from") LocalDateTime from, Pageable limit);
}
