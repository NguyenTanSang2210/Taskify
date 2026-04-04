package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.entity.Privilege;
import com.doanltmmt.Backend.entity.Role;
import com.doanltmmt.Backend.repository.PrivilegeRepository;
import com.doanltmmt.Backend.repository.RoleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoleService {

    private final RoleRepository roleRepo;
    private final PrivilegeRepository privilegeRepo;
    private final AuditLogService auditLogService;

    public RoleService(RoleRepository roleRepo, PrivilegeRepository privilegeRepo, AuditLogService auditLogService) {
        this.roleRepo = roleRepo;
        this.privilegeRepo = privilegeRepo;
        this.auditLogService = auditLogService;
    }

    public List<Role> getAllRoles() {
        return roleRepo.findAll();
    }

    public List<Privilege> getAllPrivileges() {
        return privilegeRepo.findAll();
    }

    @Transactional
    public Role createRole(String name, String description) {
        if (roleRepo.findByName(name).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vai trò này đã tồn tại trong hệ thống.");
        }
        Role role = new Role(name, description);
        Role saved = roleRepo.save(role);
        auditLogService.log("CREATE_ROLE", "Role", saved.getId().toString(), "Đã tạo vai trò mới: " + name);
        return saved;
    }

    @Transactional
    public Role updateRole(Long id, String description, Set<Long> privilegeIds) {
        Role role = roleRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy vai trò yêu cầu."));
        
        role.setDescription(description);
        
        if (privilegeIds != null) {
            Set<Privilege> privileges = privilegeRepo.findAllById(privilegeIds).stream().collect(Collectors.toSet());
            role.setPrivileges(privileges);
        }
        
        Role saved = roleRepo.save(role);
        auditLogService.log("UPDATE_ROLE", "Role", saved.getId().toString(), "Đã cập nhật thông tin và quyền hạn cho vai trò: " + role.getName());
        return saved;
    }

    @Transactional
    public void deleteRole(Long id) {
        Role role = roleRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy vai trò để xóa."));
        
        // Cần kiểm tra xem có người dùng nào đang gán vai trò này không trước khi xóa (giả định có kiểm tra ở tầng Repository hoặc DB)
        roleRepo.delete(role);
        auditLogService.log("DELETE_ROLE", "Role", id.toString(), "Đã xóa vai trò: " + role.getName());
    }

    @Transactional
    public Privilege createPrivilege(String name, String description) {
        if (privilegeRepo.findByName(name).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quyền hạn này đã tồn tại.");
        }
        Privilege privilege = new Privilege(name, description);
        Privilege saved = privilegeRepo.save(privilege);
        auditLogService.log("CREATE_PRIVILEGE", "Privilege", saved.getId().toString(), "Đã tạo quyền hạn mới: " + name);
        return saved;
    }
}
