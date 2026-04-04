package com.doanltmmt.Backend.config;

import com.doanltmmt.Backend.entity.Privilege;
import com.doanltmmt.Backend.entity.Role;
import com.doanltmmt.Backend.repository.PrivilegeRepository;
import com.doanltmmt.Backend.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.HashSet;

@Configuration
public class RolePrivilegeInitializer {

    @Bean
    CommandLineRunner initRolesAndPrivileges(RoleRepository roleRepo, PrivilegeRepository privilegeRepo) {
        return args -> {
            // 1. Khởi tạo Privileges
            Privilege userManage = createPrivilegeIfNotFound(privilegeRepo, "USER_MANAGE", "Quản lý người dùng (Xem, Thêm, Sửa, Xóa)");
            Privilege roleManage = createPrivilegeIfNotFound(privilegeRepo, "ROLE_MANAGE", "Quản lý vai trò và quyền hạn");
            Privilege deptManage = createPrivilegeIfNotFound(privilegeRepo, "DEPT_MANAGE", "Quản lý khoa và lớp học");
            Privilege topicManage = createPrivilegeIfNotFound(privilegeRepo, "TOPIC_MANAGE", "Quản lý đề tài (Phê duyệt, Đóng/Mở)");
            Privilege regManage = createPrivilegeIfNotFound(privilegeRepo, "REGISTRATION_MANAGE", "Duyệt đăng ký đề tài");
            Privilege reportView = createPrivilegeIfNotFound(privilegeRepo, "SYSTEM_REPORT", "Xem báo cáo và thống kê hệ thống");

            // 2. Cấu hình vai trò ADMIN
            Role adminRole = roleRepo.findByName("ADMIN").orElse(new Role("ADMIN"));
            adminRole.setDescription("Quản trị viên toàn hệ thống");
            adminRole.setPrivileges(new HashSet<>(Arrays.asList(userManage, roleManage, deptManage, topicManage, regManage, reportView)));
            roleRepo.save(adminRole);

            // 3. Cấu hình vai trò DEPARTMENT_ADMIN
            Role deptAdminRole = roleRepo.findByName("DEPARTMENT_ADMIN").orElse(new Role("DEPARTMENT_ADMIN"));
            deptAdminRole.setDescription("Quản trị viên cấp khoa");
            deptAdminRole.setPrivileges(new HashSet<>(Arrays.asList(deptManage, topicManage, regManage, reportView)));
            roleRepo.save(deptAdminRole);

            // 4. Cấu hình vai trò LECTURER
            Role lecturerRole = roleRepo.findByName("LECTURER").orElse(new Role("LECTURER"));
            lecturerRole.setDescription("Giảng viên hướng dẫn");
            lecturerRole.setPrivileges(new HashSet<>(Arrays.asList(regManage))); // Giảng viên chỉ có quyền duyệt đăng ký của mình (logic chi tiết ở Service)
            roleRepo.save(lecturerRole);
            
            // 5. Cấu hình vai trò STUDENT
            Role studentRole = roleRepo.findByName("STUDENT").orElse(new Role("STUDENT"));
            studentRole.setDescription("Sinh viên thực hiện đề tài");
            studentRole.setPrivileges(new HashSet<>()); 
            roleRepo.save(studentRole);
        };
    }

    private Privilege createPrivilegeIfNotFound(PrivilegeRepository repo, String name, String description) {
        return repo.findByName(name).orElseGet(() -> {
            Privilege p = new Privilege(name, description);
            return repo.save(p);
        });
    }
}
