package com.doanltmmt.Backend.repository;

import com.doanltmmt.Backend.entity.AcademicClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AcademicClassRepository extends JpaRepository<AcademicClass, Long> {
    List<AcademicClass> findByDepartment_IdOrderByIdDesc(Long departmentId);
    Optional<AcademicClass> findByDepartment_IdAndCode(Long departmentId, String code);
}

