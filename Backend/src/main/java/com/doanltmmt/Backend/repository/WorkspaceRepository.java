package com.doanltmmt.Backend.repository;

import com.doanltmmt.Backend.entity.enums.WorkspaceStatus;
import com.doanltmmt.Backend.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
    List<Workspace> findByDepartment_IdOrderByIdDesc(Long departmentId);
    List<Workspace> findByStatusOrderByIdDesc(WorkspaceStatus status);
    List<Workspace> findByDepartment_IdAndStatusOrderByIdDesc(Long departmentId, WorkspaceStatus status);
    long countByDepartment_Id(Long departmentId);
}
