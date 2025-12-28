package com.doanltmmt.Backend.repository;

import com.doanltmmt.Backend.entity.WorkspaceClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkspaceClassRepository extends JpaRepository<WorkspaceClass, Long> {
    List<WorkspaceClass> findByWorkspace_IdOrderByIdDesc(Long workspaceId);
    Optional<WorkspaceClass> findByWorkspace_IdAndAcademicClass_Id(Long workspaceId, Long classId);
    boolean existsByWorkspace_IdAndAcademicClass_IdAndActiveTrue(Long workspaceId, Long classId);
    long countByWorkspace_IdAndActiveTrue(Long workspaceId);
}
