package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.dto.request.RoleRequest;
import com.doanltmmt.Backend.dto.request.RoleUpdateRequest;
import com.doanltmmt.Backend.entity.Privilege;
import com.doanltmmt.Backend.entity.Role;
import com.doanltmmt.Backend.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<Role> getAllRoles() {
        return roleService.getAllRoles();
    }

    @GetMapping("/privileges")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Privilege> getAllPrivileges() {
        return roleService.getAllPrivileges();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Role> createRole(@Valid @RequestBody RoleRequest body) {
        return ResponseEntity.ok(roleService.createRole(body.name(), body.description()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Role> updateRole(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest body) {
        return ResponseEntity.ok(roleService.updateRole(id, body.description(), body.privilegeIds()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/privileges")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Privilege> createPrivilege(@Valid @RequestBody RoleRequest body) {
        return ResponseEntity.ok(roleService.createPrivilege(body.name(), body.description()));
    }
}
