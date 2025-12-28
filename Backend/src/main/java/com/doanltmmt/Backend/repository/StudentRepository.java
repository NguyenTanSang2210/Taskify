package com.doanltmmt.Backend.repository;

import com.doanltmmt.Backend.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {
    long countByUser_Department_Id(Long departmentId);
}
