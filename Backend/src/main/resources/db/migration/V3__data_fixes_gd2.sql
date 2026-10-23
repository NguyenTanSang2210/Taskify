-- =====================================================================================================
-- V3 — Chỉnh dữ liệu của Giai đoạn 2 (thay DataMigrationRunner). Mỗi bước có mốc trong bảng data_migrations:
-- DB đã chạy bước đó bằng DataMigrationRunner trước đây thì bỏ qua (không chạy lại lần hai).
-- =====================================================================================================

-- P2-01: giảng viên trên DB cũ thiếu TOPIC_MANAGE nên không tự tạo được đề tài
INSERT INTO `roles_privileges` (`role_id`, `privilege_id`)
SELECT r.id, p.id
FROM `roles` r JOIN `privileges` p ON p.name = 'TOPIC_MANAGE'
WHERE r.name = 'LECTURER'
  AND NOT EXISTS (SELECT 1 FROM `data_migrations` m WHERE m.id = 'P2-01_lecturer_topic_manage')
  AND NOT EXISTS (SELECT 1 FROM `roles_privileges` rp WHERE rp.role_id = r.id AND rp.privilege_id = p.id);

-- P2-04: mô hình cũ 1 SV/đề tài đặt "REGISTERED" khi duyệt 1 SV → tính lại OPEN/FULL theo capacity
UPDATE `topics` t
SET t.status = CASE
        WHEN (SELECT COUNT(*) FROM `topic_registration` r WHERE r.topic_id = t.id AND r.approved = 1)
             >= GREATEST(COALESCE(t.capacity, 1), 1) THEN 'FULL'
        ELSE 'OPEN' END
WHERE t.status = 'REGISTERED'
  AND NOT EXISTS (SELECT 1 FROM `data_migrations` m WHERE m.id = 'P2-04_topic_status_by_capacity');

-- P2-11 (D-07): bỏ tính năng ProjectPermission — xóa bảng nếu rỗng; còn dữ liệu thì giữ lại để quản trị xuất trước
SET @pp_rows = NULL;
SET @pp_exists = (SELECT COUNT(*) FROM information_schema.tables
                  WHERE table_schema = DATABASE() AND table_name = 'project_permissions');
SET @ddl = IF(@pp_exists = 0, 'DO 0',
              'SELECT COUNT(*) INTO @pp_rows FROM `project_permissions`');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF(@pp_exists = 1 AND COALESCE(@pp_rows, 0) = 0, 'DROP TABLE `project_permissions`', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT IGNORE INTO `data_migrations` (`id`, `applied_at`) VALUES
    ('P2-01_lecturer_topic_manage', UTC_TIMESTAMP(6)),
    ('P2-04_topic_status_by_capacity', UTC_TIMESTAMP(6));
INSERT IGNORE INTO `data_migrations` (`id`, `applied_at`)
SELECT 'P2-11_drop_project_permissions', UTC_TIMESTAMP(6)
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM information_schema.tables
                  WHERE table_schema = DATABASE() AND table_name = 'project_permissions');
