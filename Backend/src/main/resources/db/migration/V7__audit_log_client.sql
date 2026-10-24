-- P4-02: nhật ký hệ thống ghi thêm IP + trình duyệt (user-agent) của người thực hiện; index cho bộ lọc hành động / đối tượng.
-- Viết dạng "chỉ thêm khi còn thiếu" giống V5 để chạy lại an toàn.
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'audit_logs' AND column_name = 'ip_address') = 0, 'ALTER TABLE `audit_logs` ADD COLUMN `ip_address` varchar(64) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'audit_logs' AND column_name = 'user_agent') = 0, 'ALTER TABLE `audit_logs` ADD COLUMN `user_agent` varchar(255) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'audit_logs' AND index_name = 'idx_audit_action') = 0, 'ALTER TABLE `audit_logs` ADD INDEX `idx_audit_action` (`action`, `timestamp`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'audit_logs' AND index_name = 'idx_audit_entity') = 0, 'ALTER TABLE `audit_logs` ADD INDEX `idx_audit_entity` (`entity_name`, `entity_id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
