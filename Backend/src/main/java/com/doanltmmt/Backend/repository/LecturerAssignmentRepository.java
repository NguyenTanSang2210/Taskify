package com.doanltmmt.Backend.repository;

import com.doanltmmt.Backend.entity.LecturerAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LecturerAssignmentRepository extends JpaRepository<LecturerAssignment, Long> {
    List<LecturerAssignment> findByWorkspace_IdOrderByIdDesc(Long workspaceId);
    Optional<LecturerAssignment> findByWorkspace_IdAndLecturer_IdAndType(Long workspaceId, Long lecturerId, String type);
}

