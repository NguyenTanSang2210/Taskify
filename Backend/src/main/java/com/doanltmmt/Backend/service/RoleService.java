package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.entity.Privilege;
import com.doanltmmt.Backend.entity.Role;
import com.doanltmmt.Backend.repository.PrivilegeRepository;
import com.doanltmmt.Backend.repository.RoleRepository;
import com.doanltmmt.Backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class RoleService {

    /** Vai trò mà mã nguồn dựa vào (phân quyền, luồng nghiệp vụ) — không được xóa. */
    public static final Set<String> SYSTEM_ROLES = Set.of("ADMIN", "DEPARTMENT_ADMIN", "LECTURER", "STUDENT");

    private static final Pattern ROLE_NAME = Pattern.compile("[A-Z][A-Z0-9_]{1,49}");

    private final RoleRepository roleRepo;
    private final PrivilegeRepository privilegeRepo;
    private final UserRepository userRepo;
    private final AuditLogService auditLogService;

    public RoleService(RoleRepository roleRepo, PrivilegeRepository privilegeRepo, UserRepository userRepo,
                       AuditLogService auditLogService) {
        this.roleRepo = roleRepo;
        this.privilegeRepo = privilegeRepo;
        this.userRepo = userRepo;
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
        name = name == null ? "" : name.trim().toUpperCase(Locale.ROOT);
        if (!ROLE_NAME.matcher(name).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tên vai trò chỉ gồm chữ in hoa không dấu, số và dấu gạch dưới (2–50 ký tự), ví dụ: THU_KY");
        }
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
        if (SYSTEM_ROLES.contains(role.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể xóa vai trò hệ thống " + role.getName() + ".");
        }
        long users = userRepo.countByRole_Id(id);
        if (users > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Vai trò đang được gán cho " + users + " người dùng — hãy chuyển họ sang vai trò khác trước khi xóa.");
        }
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
