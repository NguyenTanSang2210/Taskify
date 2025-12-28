package com.doanltmmt.Backend.repository;

import com.doanltmmt.Backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    long countByDepartment_Id(Long departmentId);
    List<User> findByDepartment_IdOrderByIdDesc(Long departmentId);
    List<User> findByDepartment_IdAndRole_NameOrderByIdDesc(Long departmentId, String roleName);
}
