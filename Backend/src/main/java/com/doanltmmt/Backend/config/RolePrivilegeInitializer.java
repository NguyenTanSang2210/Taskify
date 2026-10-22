package com.doanltmmt.Backend.config;

import com.doanltmmt.Backend.entity.Privilege;
import com.doanltmmt.Backend.entity.Role;
import com.doanltmmt.Backend.repository.PrivilegeRepository;
import com.doanltmmt.Backend.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tạo quyền (privilege) và 4 vai trò hệ thống.
 *
 * <p>Quyền mặc định chỉ được gán khi vai trò được tạo LẦN ĐẦU. Vai trò đã tồn tại giữ nguyên quyền do quản trị viên
 * chỉnh trên trang Vai trò (trước đây bị ghi đè mỗi lần khởi động). Bổ sung quyền cho dữ liệu đã có thì làm bằng
 * một bước trong {@link DataMigrationRunner} (chạy đúng một lần).
 */
@Configuration
public class RolePrivilegeInitializer {

    private static final Logger log = LoggerFactory.getLogger(RolePrivilegeInitializer.class);

    /** Tên quyền → mô tả. */
    private static final Map<String, String> PRIVILEGES = new LinkedHashMap<>();
    /** Vai trò → mô tả + quyền mặc định. */
    private static final Map<String, Map.Entry<String, List<String>>> ROLES = new LinkedHashMap<>();

    static {
        PRIVILEGES.put("USER_MANAGE", "Quản lý người dùng (Xem, Thêm, Sửa, Xóa)");
        PRIVILEGES.put("ROLE_MANAGE", "Quản lý vai trò và quyền hạn");
        PRIVILEGES.put("DEPT_MANAGE", "Quản lý khoa và lớp học");
        PRIVILEGES.put("TOPIC_MANAGE", "Quản lý đề tài (Tạo, Sửa, Đóng/Mở)");
        PRIVILEGES.put("REGISTRATION_MANAGE", "Duyệt đăng ký đề tài");
        PRIVILEGES.put("SYSTEM_REPORT", "Xem báo cáo và thống kê hệ thống");

        ROLES.put("ADMIN", Map.entry("Quản trị viên toàn hệ thống",
                List.of("USER_MANAGE", "ROLE_MANAGE", "DEPT_MANAGE", "TOPIC_MANAGE", "REGISTRATION_MANAGE", "SYSTEM_REPORT")));
        ROLES.put("DEPARTMENT_ADMIN", Map.entry("Quản trị viên cấp khoa",
                List.of("DEPT_MANAGE", "TOPIC_MANAGE", "REGISTRATION_MANAGE", "SYSTEM_REPORT")));
        // Giảng viên chỉ thao tác trên đề tài/đăng ký của mình — phạm vi kiểm tra ở tầng service
        ROLES.put("LECTURER", Map.entry("Giảng viên hướng dẫn", List.of("TOPIC_MANAGE", "REGISTRATION_MANAGE")));
        ROLES.put("STUDENT", Map.entry("Sinh viên thực hiện đề tài", List.of()));
    }

    /** Chạy TRƯỚC DataSeeder để vai trò được tạo kèm quyền mặc định (DataSeeder chỉ tạo vai trò trống nếu thiếu). */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    CommandLineRunner initRolesAndPrivileges(RoleRepository roleRepo, PrivilegeRepository privilegeRepo,
                                             TransactionTemplate tx) {
        return args -> tx.executeWithoutResult(status -> {
            Map<String, Privilege> privileges = new LinkedHashMap<>();
            PRIVILEGES.forEach((name, description) -> privileges.put(name, privilegeRepo.findByName(name)
                    .orElseGet(() -> privilegeRepo.save(new Privilege(name, description)))));

            ROLES.forEach((name, def) -> {
                if (roleRepo.findByName(name).isEmpty()) {
                    Role role = new Role(name, def.getKey());
                    role.setPrivileges(collect(privileges, def.getValue()));
                    roleRepo.save(role);
                    log.info("Đã tạo vai trò {} với quyền mặc định {}", name, def.getValue());
                }
            });
        });
    }

    private static Set<Privilege> collect(Map<String, Privilege> privileges, List<String> names) {
        Set<Privilege> result = new HashSet<>();
        names.forEach(n -> result.add(privileges.get(n)));
        return result;
    }
}
