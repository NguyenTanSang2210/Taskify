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
}
