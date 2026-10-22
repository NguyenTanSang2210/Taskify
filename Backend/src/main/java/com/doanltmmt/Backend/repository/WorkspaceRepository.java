package com.doanltmmt.Backend.repository;

import com.doanltmmt.Backend.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
    List<Workspace> findByDepartment_IdOrderByIdDesc(Long departmentId);
    List<Workspace> findByStatusInOrderByIdDesc(List<String> statuses);
    List<Workspace> findByDepartment_IdAndStatusInOrderByIdDesc(Long departmentId, List<String> statuses);
    long countByDepartment_Id(Long departmentId);
}
