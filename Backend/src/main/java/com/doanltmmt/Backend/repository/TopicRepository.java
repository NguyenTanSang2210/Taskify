package com.doanltmmt.Backend.repository;

import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.enums.TopicStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TopicRepository extends JpaRepository<Topic, Long>, JpaSpecificationExecutor<Topic> {

    /** Khóa dòng đề tài khi duyệt để 2 lượt duyệt đồng thời không vượt capacity. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Topic t where t.id = :id")
    Optional<Topic> findByIdForUpdate(@Param("id") Long id);

    /** Trang danh sách đề tài: nạp giảng viên + đợt cùng một câu (tránh N+1 khi dựng DTO). */
    @Override
    @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = {"lecturer", "lecturer.user", "workspace"})
    Page<Topic> findAll(Specification<Topic> spec, Pageable pageable);

    List<Topic> findByStatus(TopicStatus status);
    long countByLecturer_User_Id(Long userId);
    long countByStatus(TopicStatus status);
    long countByWorkspace_Id(Long workspaceId);
    long countByWorkspace_Department_Id(Long departmentId);
    long countByWorkspace_Department_IdAndStatus(Long departmentId, TopicStatus status);

    /** Tổng số chỗ của các đề tài trong khoa (đề tài cũ không đặt capacity tính là 1 chỗ — như Topic.effectiveCapacity). */
    @Query(
            "select coalesce(sum(case when t.capacity is null or t.capacity < 1 then 1 else t.capacity end), 0) "
            + "from Topic t where t.workspace.department.id = :departmentId")
    long sumCapacityByDepartment(@Param("departmentId") Long departmentId);
}
