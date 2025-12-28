package com.doanltmmt.Backend.repository;

import com.doanltmmt.Backend.entity.ProjectPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProjectPermissionRepository extends JpaRepository<ProjectPermission, Long> {
    List<ProjectPermission> findByTopic_Id(Long topicId);
    Optional<ProjectPermission> findByTopic_IdAndRoleNameAndToolName(Long topicId, String roleName, String toolName);
}
