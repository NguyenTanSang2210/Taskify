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
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated");
        }
        String username = auth.getName();
        return userRepo.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    public boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        String expected = "ROLE_" + role;
        return auth.getAuthorities().stream().anyMatch(a -> expected.equals(a.getAuthority()) || role.equals(a.getAuthority()));
    }

    public void requireDepartmentAccess(Long departmentId) {
        if (departmentId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "departmentId is required");
        }
        if (hasRole("ADMIN")) return;
        if (!hasRole("DEPARTMENT_ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        User current = requireCurrentUser();
        Long currentDeptId = current.getDepartment() != null ? current.getDepartment().getId() : null;
        if (currentDeptId == null || !currentDeptId.equals(departmentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Out of scope");
        }
    }
}

