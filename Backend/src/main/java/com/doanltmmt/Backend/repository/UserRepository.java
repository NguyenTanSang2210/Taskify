package com.doanltmmt.Backend.repository;

import com.doanltmmt.Backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    long countByRole_Id(Long roleId);

    /** Mỗi request xác thực đều gọi: nạp khoa + vai trò + quyền trong MỘT câu thay vì 3. */
    @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = {"department", "role", "role.privileges"})
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    boolean existsByRole_Name(String roleName);

    /** Các tên đăng nhập trong danh sách đã có tài khoản (so khớp như cột username — không phân biệt hoa thường). */
    @Query("select u.username from User u where u.username in :usernames")
    List<String> findExistingUsernames(@Param("usernames") Collection<String> usernames);

    @Query("select u.email from User u where u.email in :emails")
    List<String> findExistingEmails(@Param("emails") Collection<String> emails);
    long countByDepartment_Id(Long departmentId);
    /** Quản trị khoa của một khoa (nhận thông báo yêu cầu chấm lại). */
    List<User> findByDepartment_IdAndRole_NameOrderByIdDesc(Long departmentId, String roleName);

    /** Danh sách người dùng (lọc + phân trang): nạp khoa và vai trò cùng một câu. */
    @Override
    @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = {"department", "role"})
    Page<User> findAll(Specification<User> spec, Pageable pageable);
}
