package com.doanltmmt.Backend.repository;

import com.doanltmmt.Backend.entity.LecturerAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LecturerAssignmentRepository extends JpaRepository<LecturerAssignment, Long> {
    List<LecturerAssignment> findByWorkspace_IdOrderByIdDesc(Long workspaceId);
    Optional<LecturerAssignment> findByWorkspace_IdAndLecturer_IdAndType(Long workspaceId, Long lecturerId, String type);

    /** Giảng viên của khoa chưa được phân công vào đợt nào (không có phân công đang hiệu lực). */
    @Query("""
            select count(u) from User u
            where u.department.id = :departmentId and u.role.name = 'LECTURER'
              and not exists (select a.id from LecturerAssignment a where a.lecturer.id = u.id and a.active = true)
            """)
    long countLecturersWithoutAssignment(@Param("departmentId") Long departmentId);
}

