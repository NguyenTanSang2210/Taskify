package com.doanltmmt.Backend.repository;

import com.doanltmmt.Backend.entity.AcademicClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AcademicClassRepository extends JpaRepository<AcademicClass, Long> {
    List<AcademicClass> findByDepartment_IdOrderByIdDesc(Long departmentId);
}

