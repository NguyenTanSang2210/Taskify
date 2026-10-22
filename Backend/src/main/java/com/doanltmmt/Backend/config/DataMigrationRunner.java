package com.doanltmmt.Backend.config;

import com.doanltmmt.Backend.entity.DataMigration;
import com.doanltmmt.Backend.entity.Privilege;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.repository.DataMigrationRepository;
import com.doanltmmt.Backend.repository.PrivilegeRepository;
import com.doanltmmt.Backend.repository.RoleRepository;
import com.doanltmmt.Backend.repository.TopicRegistrationRepository;
import com.doanltmmt.Backend.repository.TopicRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * Các bước chỉnh dữ liệu cho DB đã có từ trước, mỗi bước chạy ĐÚNG MỘT LẦN (đánh dấu trong bảng
 * {@code data_migrations}). Chạy sau {@link RolePrivilegeInitializer}. Sẽ chuyển sang Flyway ở P3-01.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class DataMigrationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataMigrationRunner.class);

    private final DataMigrationRepository migrationRepo;
    private final RoleRepository roleRepo;
    private final PrivilegeRepository privilegeRepo;
    private final TopicRepository topicRepo;
    private final TopicRegistrationRepository regRepo;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;

    public DataMigrationRunner(DataMigrationRepository migrationRepo, RoleRepository roleRepo,
                               PrivilegeRepository privilegeRepo, TopicRepository topicRepo,
                               TopicRegistrationRepository regRepo, JdbcTemplate jdbc, TransactionTemplate tx) {
        this.migrationRepo = migrationRepo;
        this.roleRepo = roleRepo;
        this.privilegeRepo = privilegeRepo;
        this.topicRepo = topicRepo;
        this.regRepo = regRepo;
        this.jdbc = jdbc;
        this.tx = tx;
    }

    @Override
    public void run(String... args) {
        // Mỗi bước trả về true khi đã xong (được đánh dấu), false để thử lại ở lần khởi động sau
        Map<String, BooleanSupplier> steps = new LinkedHashMap<>();
        steps.put("P2-01_lecturer_topic_manage", this::lecturerTopicManage);
        steps.put("P2-04_topic_status_by_capacity", this::topicStatusByCapacity);
        steps.put("P2-11_drop_project_permissions", this::dropProjectPermissions);
        steps.forEach((id, step) -> tx.executeWithoutResult(status -> {
            if (migrationRepo.existsById(id)) return;
            if (!step.getAsBoolean()) return;
            migrationRepo.save(new DataMigration(id));
            log.info("Đã chạy bước dữ liệu {}", id);
        }));
    }

    /** Giảng viên trên DB cũ thiếu TOPIC_MANAGE nên không tự tạo được đề tài. */
    private boolean lecturerTopicManage() {
        Privilege topicManage = privilegeRepo.findByName("TOPIC_MANAGE").orElseThrow();
        roleRepo.findByName("LECTURER").ifPresent(lecturer -> {
            lecturer.getPrivileges().add(topicManage);
            roleRepo.save(lecturer);
        });
        return true;
    }

    /** Mô hình cũ 1 SV/đề tài đặt "REGISTERED" khi duyệt 1 SV → tính lại OPEN/FULL theo capacity. */
    private boolean topicStatusByCapacity() {
        for (Topic topic : topicRepo.findByStatus("REGISTERED")) {
            topic.setStatus(Topic.OPEN);
            topic.refreshCapacityStatus(regRepo.countByTopic_IdAndApprovedTrue(topic.getId()));
            topicRepo.save(topic);
        }
        return true;
    }

    /**
     * D-07: tính năng ProjectPermission đã bỏ — xóa bảng {@code project_permissions} (ddl-auto không tự xóa).
     * Bảng còn dữ liệu thì KHÔNG xóa, chỉ cảnh báo để quản trị xuất dữ liệu trước.
     */
    private boolean dropProjectPermissions() {
        Integer exists = jdbc.queryForObject("select count(*) from information_schema.tables "
                + "where table_schema = database() and table_name = 'project_permissions'", Integer.class);
        if (exists == null || exists == 0) return true;
        Integer rows = jdbc.queryForObject("select count(*) from project_permissions", Integer.class);
        if (rows != null && rows > 0) {
            log.warn("Bảng project_permissions còn {} dòng — hãy xuất dữ liệu rồi xóa bảng thủ công (D-07)", rows);
            return false;
        }
        jdbc.execute("drop table project_permissions");
        return true;
    }
}
