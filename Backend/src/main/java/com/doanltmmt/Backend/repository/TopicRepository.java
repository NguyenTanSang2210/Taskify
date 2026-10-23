package com.doanltmmt.Backend.repository;

import com.doanltmmt.Backend.entity.Topic;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TopicRepository extends JpaRepository<Topic, Long>, JpaSpecificationExecutor<Topic> {

    /** Khóa dòng đề tài khi duyệt để 2 lượt duyệt đồng thời không vượt capacity. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Topic t where t.id = :id")
    Optional<Topic> findByIdForUpdate(@Param("id") Long id);

    List<Topic> findByStatus(String status);
    long countByLecturer_User_Id(Long userId);
    long countByStatus(String status);
    long countByWorkspace_Id(Long workspaceId);
    long countByWorkspace_Department_Id(Long departmentId);
    long countByWorkspace_Department_IdAndStatus(Long departmentId, String status);

    /** Tổng số chỗ của các đề tài trong khoa (đề tài cũ không đặt capacity tính là 1 chỗ — như Topic.effectiveCapacity). */
    @Query(
            "select coalesce(sum(case when t.capacity is null or t.capacity < 1 then 1 else t.capacity end), 0) "
            + "from Topic t where t.workspace.department.id = :departmentId")
    long sumCapacityByDepartment(@Param("departmentId") Long departmentId);
}
