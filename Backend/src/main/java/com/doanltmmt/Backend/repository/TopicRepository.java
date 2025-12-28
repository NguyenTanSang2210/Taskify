package com.doanltmmt.Backend.repository;

import com.doanltmmt.Backend.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TopicRepository extends JpaRepository<Topic, Long>, JpaSpecificationExecutor<Topic> {
    long countByLecturer_User_Id(Long userId);
    long countByStatus(String status);
    long countByWorkspace_Id(Long workspaceId);
    long countByWorkspace_Department_Id(Long departmentId);
    long countByWorkspace_Department_IdAndStatus(Long departmentId, String status);
}
