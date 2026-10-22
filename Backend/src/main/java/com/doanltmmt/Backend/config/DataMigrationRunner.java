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
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

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
    private final TransactionTemplate tx;

    public DataMigrationRunner(DataMigrationRepository migrationRepo, RoleRepository roleRepo,
                               PrivilegeRepository privilegeRepo, TopicRepository topicRepo,
                               TopicRegistrationRepository regRepo, TransactionTemplate tx) {
        this.migrationRepo = migrationRepo;
        this.roleRepo = roleRepo;
        this.privilegeRepo = privilegeRepo;
        this.topicRepo = topicRepo;
        this.regRepo = regRepo;
        this.tx = tx;
    }

    @Override
    public void run(String... args) {
        Map<String, Runnable> steps = new LinkedHashMap<>();
        steps.put("P2-01_lecturer_topic_manage", this::lecturerTopicManage);
        steps.put("P2-04_topic_status_by_capacity", this::topicStatusByCapacity);
        steps.forEach((id, step) -> tx.executeWithoutResult(status -> {
            if (migrationRepo.existsById(id)) return;
            step.run();
            migrationRepo.save(new DataMigration(id));
            log.info("Đã chạy bước dữ liệu {}", id);
        }));
    }

    /** Giảng viên trên DB cũ thiếu TOPIC_MANAGE nên không tự tạo được đề tài. */
    private void lecturerTopicManage() {
        Privilege topicManage = privilegeRepo.findByName("TOPIC_MANAGE").orElseThrow();
        roleRepo.findByName("LECTURER").ifPresent(lecturer -> {
            lecturer.getPrivileges().add(topicManage);
            roleRepo.save(lecturer);
        });
    }

    /** Mô hình cũ 1 SV/đề tài đặt "REGISTERED" khi duyệt 1 SV → tính lại OPEN/FULL theo capacity. */
    private void topicStatusByCapacity() {
        for (Topic topic : topicRepo.findByStatus("REGISTERED")) {
            topic.setStatus(Topic.OPEN);
            topic.refreshCapacityStatus(regRepo.countByTopic_IdAndApprovedTrue(topic.getId()));
            topicRepo.save(topic);
        }
    }
}
