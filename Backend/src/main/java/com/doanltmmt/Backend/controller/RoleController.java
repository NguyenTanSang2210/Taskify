package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.entity.Privilege;
import com.doanltmmt.Backend.entity.Role;
import com.doanltmmt.Backend.service.RoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    public ResponseEntity<Role> createRole(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String description = body.get("description");
        return ResponseEntity.ok(roleService.createRole(name, description));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Role> updateRole(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String description = (String) body.get("description");
        @SuppressWarnings("unchecked")
        List<Number> privs = (List<Number>) body.get("privilegeIds");
        Set<Long> privilegeIds = privs != null ? privs.stream().map(Number::longValue).collect(Collectors.toSet()) : null;
        return ResponseEntity.ok(roleService.updateRole(id, description, privilegeIds));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/privileges")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Privilege> createPrivilege(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String description = body.get("description");
        return ResponseEntity.ok(roleService.createPrivilege(name, description));
    }
}
