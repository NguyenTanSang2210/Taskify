-- P3-07: index cho các truy vấn hay dùng (danh sách đề tài/đăng ký, thông báo, tiến độ, tin nhắn, nhật ký).
-- Có kiểm tra tồn tại nên chạy lại an toàn (DB cũ có thể đã có index cùng tên do tạo tay).

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'topic_registration' AND index_name = 'idx_registration_topic_approved') = 0, 'ALTER TABLE `topic_registration` ADD INDEX `idx_registration_topic_approved` (`topic_id`,`approved`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'notifications' AND index_name = 'idx_notifications_user_read') = 0, 'ALTER TABLE `notifications` ADD INDEX `idx_notifications_user_read` (`user_id`,`read_at`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'notifications' AND index_name = 'idx_notifications_user_created') = 0, 'ALTER TABLE `notifications` ADD INDEX `idx_notifications_user_created` (`user_id`,`created_at`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'progress_report' AND index_name = 'idx_progress_topic_created') = 0, 'ALTER TABLE `progress_report` ADD INDEX `idx_progress_topic_created` (`topic_id`,`created_at`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'progress_report' AND index_name = 'idx_progress_student_topic') = 0, 'ALTER TABLE `progress_report` ADD INDEX `idx_progress_student_topic` (`student_id`,`topic_id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'messages' AND index_name = 'idx_messages_sender_recipient_created') = 0, 'ALTER TABLE `messages` ADD INDEX `idx_messages_sender_recipient_created` (`sender_id`,`recipient_id`,`created_at`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'audit_logs' AND index_name = 'idx_audit_timestamp') = 0, 'ALTER TABLE `audit_logs` ADD INDEX `idx_audit_timestamp` (`timestamp`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'audit_logs' AND index_name = 'idx_audit_actor') = 0, 'ALTER TABLE `audit_logs` ADD INDEX `idx_audit_actor` (`actor_username`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
