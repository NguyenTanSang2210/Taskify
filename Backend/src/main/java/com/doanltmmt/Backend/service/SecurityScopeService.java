package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SecurityScopeService {

    private final UserRepository userRepo;

    public SecurityScopeService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public User requireCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại.");
        }
        String username = auth.getName();
        return userRepo.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng."));
    }

    public boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        String expected = "ROLE_" + role;
        return auth.getAuthorities().stream().anyMatch(a -> expected.equals(a.getAuthority()) || role.equals(a.getAuthority()));
    }

    public void requireDepartmentAccess(Long departmentId) {
        if (departmentId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn khoa (departmentId).");
        }
        if (hasRole("ADMIN")) return;
        if (!hasRole("DEPARTMENT_ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này.");
        }
        User current = requireCurrentUser();
        Long currentDeptId = current.getDepartment() != null ? current.getDepartment().getId() : null;
        if (currentDeptId == null || !currentDeptId.equals(departmentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập dữ liệu này.");
        }
    }
}

