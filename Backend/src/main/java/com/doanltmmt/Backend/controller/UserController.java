package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.dto.UserDTO;
import com.doanltmmt.Backend.dto.request.ChangePasswordRequest;
import com.doanltmmt.Backend.dto.request.ProfileUpdateRequest;
import com.doanltmmt.Backend.dto.request.UserCreateRequest;
import com.doanltmmt.Backend.dto.response.PageResponse;
import com.doanltmmt.Backend.entity.Role;
import com.doanltmmt.Backend.service.UserService;
import com.doanltmmt.Backend.util.Pages;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @PutMapping("/{id}/profile")
    @PreAuthorize("isAuthenticated()")
    public UserDTO updateProfile(@PathVariable Long id, @Valid @RequestBody ProfileUpdateRequest body) {
        return service.updateProfile(id, body);
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("isAuthenticated()")
    public Map<String, String> changePassword(@PathVariable Long id, @Valid @RequestBody ChangePasswordRequest body) {
        return service.changePassword(id, body);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public UserDTO getUser(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<UserDTO> getAll(@RequestParam(required = false) String role,
                                        @RequestParam(required = false) String q,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = Pages.DEFAULT_SIZE) int size) {
        return service.listAll(role, q, page, size);
    }

    @PostMapping({ "", "/create" })
    @PreAuthorize("hasRole('ADMIN')")
    public UserDTO create(@Valid @RequestBody UserCreateRequest body) {
        return service.create(body);
    }

    /** Gửi lại mã kích hoạt cho tài khoản chưa kích hoạt (cách nhau tối thiểu 60 giây). */
    @PostMapping("/{id}/resend-activation")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> resendActivation(@PathVariable Long id) {
        return service.resendActivation(id);
    }

    @GetMapping("/by-department")
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_ADMIN')")
    public PageResponse<UserDTO> listByDepartment(@RequestParam(required = false) Long departmentId,
                                                  @RequestParam(required = false) String role,
                                                  @RequestParam(required = false) String q,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = Pages.DEFAULT_SIZE) int size) {
        return service.listByDepartment(departmentId, role, q, page, size);
    }

    @GetMapping("/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Role> getAllRoles() {
        return service.listRoles();
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDTO updateRole(@PathVariable Long id, @RequestParam Long roleId) {
        return service.updateRole(id, roleId);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDTO updateStatus(@PathVariable Long id, @RequestParam Boolean active) {
        return service.updateStatus(id, active);
    }
}
