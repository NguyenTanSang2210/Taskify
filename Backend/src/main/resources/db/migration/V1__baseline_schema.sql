-- =====================================================================================================
-- V1 — Schema nền (P3-01). Sinh tự động từ schema Hibernate của mã nguồn GĐ2 (22/10/2026), sau đó chỉ sửa qua
-- migration mới (V2, V3…) — KHÔNG sửa file này.
--
-- Chạy được cho cả hai trường hợp:
--   * DB trống        → tạo đủ bảng.
--   * DB đã có dữ liệu (trước đây do ddl-auto=update tạo) → Flyway baseline ở version 0 rồi chạy file này:
--     CREATE TABLE IF NOT EXISTS bỏ qua bảng đã có; phần 2 bổ sung cột / chỉ mục / khóa ngoại còn thiếu
--     (DB tạo từ mã cũ hơn). Cột bổ sung vào bảng đã có dữ liệu được phép NULL để không lỗi với dòng cũ.
-- =====================================================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------------- 1. Bảng
CREATE TABLE IF NOT EXISTS `academic_classes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `code` varchar(50) NOT NULL,
  `name` varchar(255) NOT NULL,
  `department_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKtpbla2txfyucgv7a0tbsyarm0` (`department_id`,`code`),
  CONSTRAINT `FKr6fncacuiempnms4qo75ovf1d` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `announcements` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` text NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `title` varchar(255) NOT NULL,
  `author_id` bigint NOT NULL,
  `topic_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK1pgl63iwbqvmngumhvr3xopg3` (`author_id`),
  KEY `FKs4fe698qsc3f6utgex90gwf1q` (`topic_id`),
  CONSTRAINT `FK1pgl63iwbqvmngumhvr3xopg3` FOREIGN KEY (`author_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKs4fe698qsc3f6utgex90gwf1q` FOREIGN KEY (`topic_id`) REFERENCES `topics` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `audit_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `action` varchar(255) DEFAULT NULL,
  `actor_username` varchar(255) DEFAULT NULL,
  `details` text,
  `entity_id` varchar(255) DEFAULT NULL,
  `entity_name` varchar(255) DEFAULT NULL,
  `timestamp` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `calendar_events` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `description` text,
  `end_time` datetime(6) DEFAULT NULL,
  `start_time` datetime(6) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `type` varchar(20) DEFAULT NULL,
  `topic_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK2uebmbi4goljyd4bea8m5vnab` (`topic_id`),
  CONSTRAINT `FK2uebmbi4goljyd4bea8m5vnab` FOREIGN KEY (`topic_id`) REFERENCES `topics` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `data_migrations` (
  `id` varchar(100) NOT NULL,
  `applied_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `departments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `code` varchar(50) NOT NULL,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKl7tivi5261wxdnvo6cct9gg6t` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `discussion_posts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` text NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `author_id` bigint NOT NULL,
  `thread_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKm4m5leisx6to9ts8nuoekw7fn` (`author_id`),
  KEY `FKtcr686u3k5o3sama235eae1ff` (`thread_id`),
  CONSTRAINT `FKm4m5leisx6to9ts8nuoekw7fn` FOREIGN KEY (`author_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKtcr686u3k5o3sama235eae1ff` FOREIGN KEY (`thread_id`) REFERENCES `discussion_threads` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `discussion_threads` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `title` varchar(255) NOT NULL,
  `created_by` bigint NOT NULL,
  `topic_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKpvk346hab63rihnrudqu8tpn0` (`created_by`),
  KEY `FK4rf5rba76krjf8s7yx4v16l93` (`topic_id`),
  CONSTRAINT `FK4rf5rba76krjf8s7yx4v16l93` FOREIGN KEY (`topic_id`) REFERENCES `topics` (`id`),
  CONSTRAINT `FKpvk346hab63rihnrudqu8tpn0` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `grade_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `changed_at` datetime(6) NOT NULL,
  `new_feedback` text,
  `new_score` double NOT NULL,
  `old_feedback` text,
  `old_score` double DEFAULT NULL,
  `reason` text,
  `changed_by` bigint NOT NULL,
  `registration_id` bigint NOT NULL,
  `revision_request_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_grade_history_reg` (`registration_id`),
  KEY `FK2cdt7rpftl2u8p9bypq7lrw85` (`changed_by`),
  KEY `FK3xpduiqx6lvpfl5tpvjy3yu47` (`revision_request_id`),
  CONSTRAINT `FK2cdt7rpftl2u8p9bypq7lrw85` FOREIGN KEY (`changed_by`) REFERENCES `users` (`id`),
  CONSTRAINT `FK3xpduiqx6lvpfl5tpvjy3yu47` FOREIGN KEY (`revision_request_id`) REFERENCES `grade_revision_requests` (`id`),
  CONSTRAINT `FK92353p6c96bn8kb0f20td7aen` FOREIGN KEY (`registration_id`) REFERENCES `topic_registration` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `grade_revision_requests` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `reason` text NOT NULL,
  `review_note` varchar(1000) DEFAULT NULL,
  `reviewed_at` datetime(6) DEFAULT NULL,
  `status` varchar(20) NOT NULL,
  `unlock_until` datetime(6) DEFAULT NULL,
  `registration_id` bigint NOT NULL,
  `requested_by` bigint NOT NULL,
  `reviewed_by` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_grade_revision_reg_status` (`registration_id`,`status`),
  KEY `FK8ogvt8j2d5um0fwb81a1ae0t9` (`requested_by`),
  KEY `FK6bn8xcliy6w83l9wku5ioldsy` (`reviewed_by`),
  CONSTRAINT `FK6bn8xcliy6w83l9wku5ioldsy` FOREIGN KEY (`reviewed_by`) REFERENCES `users` (`id`),
  CONSTRAINT `FK8ogvt8j2d5um0fwb81a1ae0t9` FOREIGN KEY (`requested_by`) REFERENCES `users` (`id`),
  CONSTRAINT `FKojkju9eopojixvcb3vn1u5rpr` FOREIGN KEY (`registration_id`) REFERENCES `topic_registration` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `lecturer_assignments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `quota_max_groups` int NOT NULL,
  `reason` text,
  `type` varchar(20) NOT NULL,
  `lecturer_id` bigint NOT NULL,
  `workspace_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK16a8pxyubeqt9e2vf9yt2t09i` (`workspace_id`,`lecturer_id`,`type`),
  KEY `FKlvfgc661txvu3jncj3baafgh0` (`lecturer_id`),
  CONSTRAINT `FKlvfgc661txvu3jncj3baafgh0` FOREIGN KEY (`lecturer_id`) REFERENCES `lecturers` (`id`),
  CONSTRAINT `FKnq9fgadg0uios2y0b1y1yq60w` FOREIGN KEY (`workspace_id`) REFERENCES `workspaces` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `lecturers` (
  `id` bigint NOT NULL,
  `degree` varchar(255) DEFAULT NULL,
  `speciality` varchar(255) DEFAULT NULL,
  `department_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKpowy6g9rglsdytsdu6c9fymox` (`department_id`),
  CONSTRAINT `FK7d4qvhqq0sr5qryod4h5tcvy5` FOREIGN KEY (`id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKpowy6g9rglsdytsdu6c9fymox` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `login_throttle` (
  `throttle_key` varchar(64) NOT NULL,
  `failures` int NOT NULL,
  `locked_until` datetime(6) DEFAULT NULL,
  `window_start` datetime(6) NOT NULL,
  PRIMARY KEY (`throttle_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `messages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` text NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `recipient_id` bigint NOT NULL,
  `sender_id` bigint NOT NULL,
  `topic_id` bigint DEFAULT NULL,
  `workspace_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKhdkwfnspwb3s60j27vpg0rpg6` (`recipient_id`),
  KEY `FK4ui4nnwntodh6wjvck53dbk9m` (`sender_id`),
  KEY `FKkoe6xlcl0wxc7d5ckn58y1x0c` (`topic_id`),
  KEY `FK31v0eu0kks5tp34l1p11290ox` (`workspace_id`),
  CONSTRAINT `FK31v0eu0kks5tp34l1p11290ox` FOREIGN KEY (`workspace_id`) REFERENCES `workspaces` (`id`),
  CONSTRAINT `FK4ui4nnwntodh6wjvck53dbk9m` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKhdkwfnspwb3s60j27vpg0rpg6` FOREIGN KEY (`recipient_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKkoe6xlcl0wxc7d5ckn58y1x0c` FOREIGN KEY (`topic_id`) REFERENCES `topics` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `milestones` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `deadline` datetime(6) DEFAULT NULL,
  `description` text,
  `start_date` datetime(6) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `topic_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKnggvk4i34f17w7qy8jyj7r4jt` (`topic_id`),
  CONSTRAINT `FKnggvk4i34f17w7qy8jyj7r4jt` FOREIGN KEY (`topic_id`) REFERENCES `topics` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` text,
  `created_at` datetime(6) NOT NULL,
  `due_at` datetime(6) DEFAULT NULL,
  `link_path` varchar(255) DEFAULT NULL,
  `read_at` datetime(6) DEFAULT NULL,
  `ref_id` bigint DEFAULT NULL,
  `ref_type` varchar(50) DEFAULT NULL,
  `severity` varchar(30) DEFAULT NULL,
  `title` varchar(200) NOT NULL,
  `type` varchar(50) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK9y21adhxn0ayjhfocscqox7bh` (`user_id`),
  CONSTRAINT `FK9y21adhxn0ayjhfocscqox7bh` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `otp_tokens` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `attempts` int NOT NULL DEFAULT '0',
  `code` varchar(10) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `expires_at` datetime(6) NOT NULL,
  `purpose` enum('ACTIVATION','LOGIN','RESET_PASSWORD') NOT NULL DEFAULT 'LOGIN',
  `used` bit(1) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKjyk2kyj8ul2bjqjpentlrpyi1` (`user_id`),
  CONSTRAINT `FKjyk2kyj8ul2bjqjpentlrpyi1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `privileges` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `description` varchar(255) DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKm2tnonbcaquofx1ccy060ejyc` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `progress_report` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` text NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `deadline` datetime(6) DEFAULT NULL,
  `file_url` varchar(500) DEFAULT NULL,
  `lecturer_comment` text,
  `similar_to_report_id` bigint DEFAULT NULL,
  `similarity_score` double DEFAULT NULL,
  `status` varchar(20) NOT NULL,
  `title` varchar(200) NOT NULL,
  `milestone_id` bigint DEFAULT NULL,
  `student_id` bigint NOT NULL,
  `topic_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKq7cpn58kdpb93a1hxu6hjvo25` (`milestone_id`),
  KEY `FKo1e7ufhbx0pt1wpv1hapvuhto` (`student_id`),
  KEY `FKb8w6aglicr9nk6gly2qb53h6f` (`topic_id`),
  CONSTRAINT `FKb8w6aglicr9nk6gly2qb53h6f` FOREIGN KEY (`topic_id`) REFERENCES `topics` (`id`),
  CONSTRAINT `FKo1e7ufhbx0pt1wpv1hapvuhto` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`),
  CONSTRAINT `FKq7cpn58kdpb93a1hxu6hjvo25` FOREIGN KEY (`milestone_id`) REFERENCES `milestones` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `roles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `description` varchar(255) DEFAULT NULL,
  `name` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKofx66keruapi6vyqpv6f2or37` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `roles_privileges` (
  `role_id` bigint NOT NULL,
  `privilege_id` bigint NOT NULL,
  PRIMARY KEY (`role_id`,`privilege_id`),
  KEY `FK5duhoc7rwt8h06avv41o41cfy` (`privilege_id`),
  CONSTRAINT `FK5duhoc7rwt8h06avv41o41cfy` FOREIGN KEY (`privilege_id`) REFERENCES `privileges` (`id`),
  CONSTRAINT `FK629oqwrudgp5u7tewl07ayugj` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `stored_files` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content_type` varchar(100) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `original_name` varchar(255) NOT NULL,
  `owner_id` bigint NOT NULL,
  `ref_id` bigint DEFAULT NULL,
  `ref_type` varchar(30) DEFAULT NULL,
  `size_bytes` bigint NOT NULL,
  `stored_name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKjjd7hf5amerajq056e2nd5y31` (`stored_name`),
  KEY `idx_stored_files_owner` (`owner_id`),
  KEY `idx_stored_files_ref` (`ref_type`,`ref_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `students` (
  `id` bigint NOT NULL,
  `class_name` varchar(255) DEFAULT NULL,
  `student_code` varchar(255) DEFAULT NULL,
  `class_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKpe06fepkeagn2w6pppj10xf68` (`class_id`),
  CONSTRAINT `FK7xqmtv7r2eb5axni3jm0a80su` FOREIGN KEY (`id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKpe06fepkeagn2w6pppj10xf68` FOREIGN KEY (`class_id`) REFERENCES `academic_classes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `topic_registration` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `approved` tinyint(1) DEFAULT NULL,
  `feedback` text,
  `registered_at` datetime(6) NOT NULL,
  `reject_reason` varchar(500) DEFAULT NULL,
  `reviewed_at` datetime(6) DEFAULT NULL,
  `score` double DEFAULT NULL,
  `reviewer_id` bigint DEFAULT NULL,
  `student_id` bigint NOT NULL,
  `topic_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_registration_student_topic` (`student_id`,`topic_id`),
  KEY `FKgagrhg6cx8bjxs0551kc7a9vs` (`reviewer_id`),
  KEY `FK10j7yem7erfd8odad81rtsnw7` (`topic_id`),
  CONSTRAINT `FK10j7yem7erfd8odad81rtsnw7` FOREIGN KEY (`topic_id`) REFERENCES `topics` (`id`),
  CONSTRAINT `FK787dgaa34uitlx5231w5kyqom` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`),
  CONSTRAINT `FKgagrhg6cx8bjxs0551kc7a9vs` FOREIGN KEY (`reviewer_id`) REFERENCES `lecturers` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `topics` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `capacity` int DEFAULT NULL,
  `description` text,
  `status` varchar(50) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `lecturer_id` bigint DEFAULT NULL,
  `workspace_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK2rrx0i1r6ecobj5uuu4t83p6m` (`lecturer_id`),
  KEY `FK20bsyno5jeliiync88nsj7v0d` (`workspace_id`),
  CONSTRAINT `FK20bsyno5jeliiync88nsj7v0d` FOREIGN KEY (`workspace_id`) REFERENCES `workspaces` (`id`),
  CONSTRAINT `FK2rrx0i1r6ecobj5uuu4t83p6m` FOREIGN KEY (`lecturer_id`) REFERENCES `lecturers` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `activated` bit(1) NOT NULL DEFAULT b'1',
  `active` bit(1) NOT NULL,
  `email` varchar(100) DEFAULT NULL,
  `full_name` varchar(100) NOT NULL,
  `must_change_password` bit(1) NOT NULL DEFAULT b'0',
  `password` varchar(255) NOT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `token_version` int NOT NULL DEFAULT '0',
  `username` varchar(50) NOT NULL,
  `department_id` bigint DEFAULT NULL,
  `role_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKr43af9ap4edm43mmtq01oddj6` (`username`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`),
  KEY `FKsbg59w8q63i0oo53rlgvlcnjq` (`department_id`),
  KEY `FKp56c1712k691lhsyewcssf40f` (`role_id`),
  CONSTRAINT `FKp56c1712k691lhsyewcssf40f` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`),
  CONSTRAINT `FKsbg59w8q63i0oo53rlgvlcnjq` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `workspace_classes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `class_id` bigint NOT NULL,
  `workspace_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK9rb4ruifhhpprs2s8dh6q93jj` (`workspace_id`,`class_id`),
  KEY `FKpcjqw35ttovnlvvh1n2pou4ul` (`class_id`),
  CONSTRAINT `FKpcjqw35ttovnlvvh1n2pou4ul` FOREIGN KEY (`class_id`) REFERENCES `academic_classes` (`id`),
  CONSTRAINT `FKrywthrafxc1ya467dodhrybn6` FOREIGN KEY (`workspace_id`) REFERENCES `workspaces` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS `workspaces` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `end_at` datetime(6) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `semester` varchar(50) DEFAULT NULL,
  `start_at` datetime(6) DEFAULT NULL,
  `status` varchar(30) NOT NULL,
  `type` varchar(50) DEFAULT NULL,
  `department_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKmy77je4f9pwmh064o2bqu07s2` (`department_id`),
  CONSTRAINT `FKmy77je4f9pwmh064o2bqu07s2` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------------- 2. Cột còn thiếu (DB cũ)
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'academic_classes' AND column_name = 'active') = 0, 'ALTER TABLE `academic_classes` ADD COLUMN `active` bit(1) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'academic_classes' AND column_name = 'code') = 0, 'ALTER TABLE `academic_classes` ADD COLUMN `code` varchar(50) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'academic_classes' AND column_name = 'name') = 0, 'ALTER TABLE `academic_classes` ADD COLUMN `name` varchar(255) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'academic_classes' AND column_name = 'department_id') = 0, 'ALTER TABLE `academic_classes` ADD COLUMN `department_id` bigint NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'announcements' AND column_name = 'content') = 0, 'ALTER TABLE `announcements` ADD COLUMN `content` text NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'announcements' AND column_name = 'created_at') = 0, 'ALTER TABLE `announcements` ADD COLUMN `created_at` datetime(6) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'announcements' AND column_name = 'title') = 0, 'ALTER TABLE `announcements` ADD COLUMN `title` varchar(255) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'announcements' AND column_name = 'author_id') = 0, 'ALTER TABLE `announcements` ADD COLUMN `author_id` bigint NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'announcements' AND column_name = 'topic_id') = 0, 'ALTER TABLE `announcements` ADD COLUMN `topic_id` bigint NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'audit_logs' AND column_name = 'action') = 0, 'ALTER TABLE `audit_logs` ADD COLUMN `action` varchar(255) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'audit_logs' AND column_name = 'actor_username') = 0, 'ALTER TABLE `audit_logs` ADD COLUMN `actor_username` varchar(255) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'audit_logs' AND column_name = 'details') = 0, 'ALTER TABLE `audit_logs` ADD COLUMN `details` text NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'audit_logs' AND column_name = 'entity_id') = 0, 'ALTER TABLE `audit_logs` ADD COLUMN `entity_id` varchar(255) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'audit_logs' AND column_name = 'entity_name') = 0, 'ALTER TABLE `audit_logs` ADD COLUMN `entity_name` varchar(255) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'audit_logs' AND column_name = 'timestamp') = 0, 'ALTER TABLE `audit_logs` ADD COLUMN `timestamp` datetime(6) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'calendar_events' AND column_name = 'description') = 0, 'ALTER TABLE `calendar_events` ADD COLUMN `description` text NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'calendar_events' AND column_name = 'end_time') = 0, 'ALTER TABLE `calendar_events` ADD COLUMN `end_time` datetime(6) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'calendar_events' AND column_name = 'start_time') = 0, 'ALTER TABLE `calendar_events` ADD COLUMN `start_time` datetime(6) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'calendar_events' AND column_name = 'title') = 0, 'ALTER TABLE `calendar_events` ADD COLUMN `title` varchar(255) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'calendar_events' AND column_name = 'type') = 0, 'ALTER TABLE `calendar_events` ADD COLUMN `type` varchar(20) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'calendar_events' AND column_name = 'topic_id') = 0, 'ALTER TABLE `calendar_events` ADD COLUMN `topic_id` bigint NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'data_migrations' AND column_name = 'applied_at') = 0, 'ALTER TABLE `data_migrations` ADD COLUMN `applied_at` datetime(6) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'departments' AND column_name = 'active') = 0, 'ALTER TABLE `departments` ADD COLUMN `active` bit(1) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'departments' AND column_name = 'code') = 0, 'ALTER TABLE `departments` ADD COLUMN `code` varchar(50) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'departments' AND column_name = 'name') = 0, 'ALTER TABLE `departments` ADD COLUMN `name` varchar(255) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'discussion_posts' AND column_name = 'content') = 0, 'ALTER TABLE `discussion_posts` ADD COLUMN `content` text NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'discussion_posts' AND column_name = 'created_at') = 0, 'ALTER TABLE `discussion_posts` ADD COLUMN `created_at` datetime(6) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'discussion_posts' AND column_name = 'author_id') = 0, 'ALTER TABLE `discussion_posts` ADD COLUMN `author_id` bigint NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'discussion_posts' AND column_name = 'thread_id') = 0, 'ALTER TABLE `discussion_posts` ADD COLUMN `thread_id` bigint NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'discussion_threads' AND column_name = 'created_at') = 0, 'ALTER TABLE `discussion_threads` ADD COLUMN `created_at` datetime(6) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'discussion_threads' AND column_name = 'title') = 0, 'ALTER TABLE `discussion_threads` ADD COLUMN `title` varchar(255) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'discussion_threads' AND column_name = 'created_by') = 0, 'ALTER TABLE `discussion_threads` ADD COLUMN `created_by` bigint NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'discussion_threads' AND column_name = 'topic_id') = 0, 'ALTER TABLE `discussion_threads` ADD COLUMN `topic_id` bigint NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'grade_history' AND column_name = 'changed_at') = 0, 'ALTER TABLE `grade_history` ADD COLUMN `changed_at` datetime(6) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'grade_history' AND column_name = 'new_feedback') = 0, 'ALTER TABLE `grade_history` ADD COLUMN `new_feedback` text NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'grade_history' AND column_name = 'new_score') = 0, 'ALTER TABLE `grade_history` ADD COLUMN `new_score` double NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'grade_history' AND column_name = 'old_feedback') = 0, 'ALTER TABLE `grade_history` ADD COLUMN `old_feedback` text NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'grade_history' AND column_name = 'old_score') = 0, 'ALTER TABLE `grade_history` ADD COLUMN `old_score` double NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'grade_history' AND column_name = 'reason') = 0, 'ALTER TABLE `grade_history` ADD COLUMN `reason` text NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'grade_history' AND column_name = 'changed_by') = 0, 'ALTER TABLE `grade_history` ADD COLUMN `changed_by` bigint NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'grade_history' AND column_name = 'registration_id') = 0, 'ALTER TABLE `grade_history` ADD COLUMN `registration_id` bigint NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'grade_history' AND column_name = 'revision_request_id') = 0, 'ALTER TABLE `grade_history` ADD COLUMN `revision_request_id` bigint NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'grade_revision_requests' AND column_name = 'created_at') = 0, 'ALTER TABLE `grade_revision_requests` ADD COLUMN `created_at` datetime(6) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'grade_revision_requests' AND column_name = 'reason') = 0, 'ALTER TABLE `grade_revision_requests` ADD COLUMN `reason` text NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'grade_revision_requests' AND column_name = 'review_note') = 0, 'ALTER TABLE `grade_revision_requests` ADD COLUMN `review_note` varchar(1000) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'grade_revision_requests' AND column_name = 'reviewed_at') = 0, 'ALTER TABLE `grade_revision_requests` ADD COLUMN `reviewed_at` datetime(6) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'grade_revision_requests' AND column_name = 'status') = 0, 'ALTER TABLE `grade_revision_requests` ADD COLUMN `status` varchar(20) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'grade_revision_requests' AND column_name = 'unlock_until') = 0, 'ALTER TABLE `grade_revision_requests` ADD COLUMN `unlock_until` datetime(6) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'grade_revision_requests' AND column_name = 'registration_id') = 0, 'ALTER TABLE `grade_revision_requests` ADD COLUMN `registration_id` bigint NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'grade_revision_requests' AND column_name = 'requested_by') = 0, 'ALTER TABLE `grade_revision_requests` ADD COLUMN `requested_by` bigint NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'grade_revision_requests' AND column_name = 'reviewed_by') = 0, 'ALTER TABLE `grade_revision_requests` ADD COLUMN `reviewed_by` bigint NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'lecturer_assignments' AND column_name = 'active') = 0, 'ALTER TABLE `lecturer_assignments` ADD COLUMN `active` bit(1) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'lecturer_assignments' AND column_name = 'created_at') = 0, 'ALTER TABLE `lecturer_assignments` ADD COLUMN `created_at` datetime(6) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'lecturer_assignments' AND column_name = 'quota_max_groups') = 0, 'ALTER TABLE `lecturer_assignments` ADD COLUMN `quota_max_groups` int NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'lecturer_assignments' AND column_name = 'reason') = 0, 'ALTER TABLE `lecturer_assignments` ADD COLUMN `reason` text NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'lecturer_assignments' AND column_name = 'type') = 0, 'ALTER TABLE `lecturer_assignments` ADD COLUMN `type` varchar(20) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'lecturer_assignments' AND column_name = 'lecturer_id') = 0, 'ALTER TABLE `lecturer_assignments` ADD COLUMN `lecturer_id` bigint NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'lecturer_assignments' AND column_name = 'workspace_id') = 0, 'ALTER TABLE `lecturer_assignments` ADD COLUMN `workspace_id` bigint NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'lecturers' AND column_name = 'degree') = 0, 'ALTER TABLE `lecturers` ADD COLUMN `degree` varchar(255) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'lecturers' AND column_name = 'speciality') = 0, 'ALTER TABLE `lecturers` ADD COLUMN `speciality` varchar(255) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'lecturers' AND column_name = 'department_id') = 0, 'ALTER TABLE `lecturers` ADD COLUMN `department_id` bigint NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'login_throttle' AND column_name = 'failures') = 0, 'ALTER TABLE `login_throttle` ADD COLUMN `failures` int NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'login_throttle' AND column_name = 'locked_until') = 0, 'ALTER TABLE `login_throttle` ADD COLUMN `locked_until` datetime(6) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'login_throttle' AND column_name = 'window_start') = 0, 'ALTER TABLE `login_throttle` ADD COLUMN `window_start` datetime(6) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'messages' AND column_name = 'content') = 0, 'ALTER TABLE `messages` ADD COLUMN `content` text NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'messages' AND column_name = 'created_at') = 0, 'ALTER TABLE `messages` ADD COLUMN `created_at` datetime(6) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'messages' AND column_name = 'recipient_id') = 0, 'ALTER TABLE `messages` ADD COLUMN `recipient_id` bigint NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'messages' AND column_name = 'sender_id') = 0, 'ALTER TABLE `messages` ADD COLUMN `sender_id` bigint NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'messages' AND column_name = 'topic_id') = 0, 'ALTER TABLE `messages` ADD COLUMN `topic_id` bigint NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'messages' AND column_name = 'workspace_id') = 0, 'ALTER TABLE `messages` ADD COLUMN `workspace_id` bigint NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'milestones' AND column_name = 'created_at') = 0, 'ALTER TABLE `milestones` ADD COLUMN `created_at` datetime(6) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'milestones' AND column_name = 'deadline') = 0, 'ALTER TABLE `milestones` ADD COLUMN `deadline` datetime(6) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'milestones' AND column_name = 'description') = 0, 'ALTER TABLE `milestones` ADD COLUMN `description` text NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'milestones' AND column_name = 'start_date') = 0, 'ALTER TABLE `milestones` ADD COLUMN `start_date` datetime(6) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'milestones' AND column_name = 'title') = 0, 'ALTER TABLE `milestones` ADD COLUMN `title` varchar(255) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'milestones' AND column_name = 'topic_id') = 0, 'ALTER TABLE `milestones` ADD COLUMN `topic_id` bigint NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'notifications' AND column_name = 'content') = 0, 'ALTER TABLE `notifications` ADD COLUMN `content` text NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'notifications' AND column_name = 'created_at') = 0, 'ALTER TABLE `notifications` ADD COLUMN `created_at` datetime(6) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'notifications' AND column_name = 'due_at') = 0, 'ALTER TABLE `notifications` ADD COLUMN `due_at` datetime(6) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'notifications' AND column_name = 'link_path') = 0, 'ALTER TABLE `notifications` ADD COLUMN `link_path` varchar(255) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'notifications' AND column_name = 'read_at') = 0, 'ALTER TABLE `notifications` ADD COLUMN `read_at` datetime(6) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'notifications' AND column_name = 'ref_id') = 0, 'ALTER TABLE `notifications` ADD COLUMN `ref_id` bigint NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'notifications' AND column_name = 'ref_type') = 0, 'ALTER TABLE `notifications` ADD COLUMN `ref_type` varchar(50) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'notifications' AND column_name = 'severity') = 0, 'ALTER TABLE `notifications` ADD COLUMN `severity` varchar(30) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'notifications' AND column_name = 'title') = 0, 'ALTER TABLE `notifications` ADD COLUMN `title` varchar(200) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'notifications' AND column_name = 'type') = 0, 'ALTER TABLE `notifications` ADD COLUMN `type` varchar(50) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'notifications' AND column_name = 'user_id') = 0, 'ALTER TABLE `notifications` ADD COLUMN `user_id` bigint NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'otp_tokens' AND column_name = 'attempts') = 0, 'ALTER TABLE `otp_tokens` ADD COLUMN `attempts` int NOT NULL DEFAULT 0', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'otp_tokens' AND column_name = 'code') = 0, 'ALTER TABLE `otp_tokens` ADD COLUMN `code` varchar(10) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'otp_tokens' AND column_name = 'created_at') = 0, 'ALTER TABLE `otp_tokens` ADD COLUMN `created_at` datetime(6) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'otp_tokens' AND column_name = 'expires_at') = 0, 'ALTER TABLE `otp_tokens` ADD COLUMN `expires_at` datetime(6) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'otp_tokens' AND column_name = 'purpose') = 0, 'ALTER TABLE `otp_tokens` ADD COLUMN `purpose` enum(''ACTIVATION'',''LOGIN'',''RESET_PASSWORD'') NOT NULL DEFAULT ''LOGIN''', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'otp_tokens' AND column_name = 'used') = 0, 'ALTER TABLE `otp_tokens` ADD COLUMN `used` bit(1) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'otp_tokens' AND column_name = 'user_id') = 0, 'ALTER TABLE `otp_tokens` ADD COLUMN `user_id` bigint NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'privileges' AND column_name = 'description') = 0, 'ALTER TABLE `privileges` ADD COLUMN `description` varchar(255) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'privileges' AND column_name = 'name') = 0, 'ALTER TABLE `privileges` ADD COLUMN `name` varchar(100) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'progress_report' AND column_name = 'content') = 0, 'ALTER TABLE `progress_report` ADD COLUMN `content` text NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'progress_report' AND column_name = 'created_at') = 0, 'ALTER TABLE `progress_report` ADD COLUMN `created_at` datetime(6) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'progress_report' AND column_name = 'deadline') = 0, 'ALTER TABLE `progress_report` ADD COLUMN `deadline` datetime(6) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'progress_report' AND column_name = 'file_url') = 0, 'ALTER TABLE `progress_report` ADD COLUMN `file_url` varchar(500) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'progress_report' AND column_name = 'lecturer_comment') = 0, 'ALTER TABLE `progress_report` ADD COLUMN `lecturer_comment` text NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'progress_report' AND column_name = 'similar_to_report_id') = 0, 'ALTER TABLE `progress_report` ADD COLUMN `similar_to_report_id` bigint NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'progress_report' AND column_name = 'similarity_score') = 0, 'ALTER TABLE `progress_report` ADD COLUMN `similarity_score` double NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'progress_report' AND column_name = 'status') = 0, 'ALTER TABLE `progress_report` ADD COLUMN `status` varchar(20) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'progress_report' AND column_name = 'title') = 0, 'ALTER TABLE `progress_report` ADD COLUMN `title` varchar(200) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'progress_report' AND column_name = 'milestone_id') = 0, 'ALTER TABLE `progress_report` ADD COLUMN `milestone_id` bigint NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'progress_report' AND column_name = 'student_id') = 0, 'ALTER TABLE `progress_report` ADD COLUMN `student_id` bigint NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'progress_report' AND column_name = 'topic_id') = 0, 'ALTER TABLE `progress_report` ADD COLUMN `topic_id` bigint NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'roles' AND column_name = 'description') = 0, 'ALTER TABLE `roles` ADD COLUMN `description` varchar(255) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'roles' AND column_name = 'name') = 0, 'ALTER TABLE `roles` ADD COLUMN `name` varchar(50) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'stored_files' AND column_name = 'content_type') = 0, 'ALTER TABLE `stored_files` ADD COLUMN `content_type` varchar(100) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'stored_files' AND column_name = 'created_at') = 0, 'ALTER TABLE `stored_files` ADD COLUMN `created_at` datetime(6) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'stored_files' AND column_name = 'original_name') = 0, 'ALTER TABLE `stored_files` ADD COLUMN `original_name` varchar(255) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'stored_files' AND column_name = 'owner_id') = 0, 'ALTER TABLE `stored_files` ADD COLUMN `owner_id` bigint NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'stored_files' AND column_name = 'ref_id') = 0, 'ALTER TABLE `stored_files` ADD COLUMN `ref_id` bigint NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'stored_files' AND column_name = 'ref_type') = 0, 'ALTER TABLE `stored_files` ADD COLUMN `ref_type` varchar(30) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'stored_files' AND column_name = 'size_bytes') = 0, 'ALTER TABLE `stored_files` ADD COLUMN `size_bytes` bigint NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'stored_files' AND column_name = 'stored_name') = 0, 'ALTER TABLE `stored_files` ADD COLUMN `stored_name` varchar(255) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'students' AND column_name = 'class_name') = 0, 'ALTER TABLE `students` ADD COLUMN `class_name` varchar(255) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'students' AND column_name = 'student_code') = 0, 'ALTER TABLE `students` ADD COLUMN `student_code` varchar(255) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'students' AND column_name = 'class_id') = 0, 'ALTER TABLE `students` ADD COLUMN `class_id` bigint NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'topic_registration' AND column_name = 'approved') = 0, 'ALTER TABLE `topic_registration` ADD COLUMN `approved` tinyint(1) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'topic_registration' AND column_name = 'feedback') = 0, 'ALTER TABLE `topic_registration` ADD COLUMN `feedback` text NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'topic_registration' AND column_name = 'registered_at') = 0, 'ALTER TABLE `topic_registration` ADD COLUMN `registered_at` datetime(6) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'topic_registration' AND column_name = 'reject_reason') = 0, 'ALTER TABLE `topic_registration` ADD COLUMN `reject_reason` varchar(500) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'topic_registration' AND column_name = 'reviewed_at') = 0, 'ALTER TABLE `topic_registration` ADD COLUMN `reviewed_at` datetime(6) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'topic_registration' AND column_name = 'score') = 0, 'ALTER TABLE `topic_registration` ADD COLUMN `score` double NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'topic_registration' AND column_name = 'reviewer_id') = 0, 'ALTER TABLE `topic_registration` ADD COLUMN `reviewer_id` bigint NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'topic_registration' AND column_name = 'student_id') = 0, 'ALTER TABLE `topic_registration` ADD COLUMN `student_id` bigint NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'topic_registration' AND column_name = 'topic_id') = 0, 'ALTER TABLE `topic_registration` ADD COLUMN `topic_id` bigint NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'topics' AND column_name = 'capacity') = 0, 'ALTER TABLE `topics` ADD COLUMN `capacity` int NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'topics' AND column_name = 'description') = 0, 'ALTER TABLE `topics` ADD COLUMN `description` text NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'topics' AND column_name = 'status') = 0, 'ALTER TABLE `topics` ADD COLUMN `status` varchar(50) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'topics' AND column_name = 'title') = 0, 'ALTER TABLE `topics` ADD COLUMN `title` varchar(255) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'topics' AND column_name = 'lecturer_id') = 0, 'ALTER TABLE `topics` ADD COLUMN `lecturer_id` bigint NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'topics' AND column_name = 'workspace_id') = 0, 'ALTER TABLE `topics` ADD COLUMN `workspace_id` bigint NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'activated') = 0, 'ALTER TABLE `users` ADD COLUMN `activated` bit(1) NOT NULL DEFAULT b''1''', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'active') = 0, 'ALTER TABLE `users` ADD COLUMN `active` bit(1) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'email') = 0, 'ALTER TABLE `users` ADD COLUMN `email` varchar(100) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'full_name') = 0, 'ALTER TABLE `users` ADD COLUMN `full_name` varchar(100) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'must_change_password') = 0, 'ALTER TABLE `users` ADD COLUMN `must_change_password` bit(1) NOT NULL DEFAULT b''0''', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'password') = 0, 'ALTER TABLE `users` ADD COLUMN `password` varchar(255) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'phone') = 0, 'ALTER TABLE `users` ADD COLUMN `phone` varchar(20) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'token_version') = 0, 'ALTER TABLE `users` ADD COLUMN `token_version` int NOT NULL DEFAULT 0', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'username') = 0, 'ALTER TABLE `users` ADD COLUMN `username` varchar(50) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'department_id') = 0, 'ALTER TABLE `users` ADD COLUMN `department_id` bigint NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'role_id') = 0, 'ALTER TABLE `users` ADD COLUMN `role_id` bigint NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'workspace_classes' AND column_name = 'active') = 0, 'ALTER TABLE `workspace_classes` ADD COLUMN `active` bit(1) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'workspace_classes' AND column_name = 'class_id') = 0, 'ALTER TABLE `workspace_classes` ADD COLUMN `class_id` bigint NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'workspace_classes' AND column_name = 'workspace_id') = 0, 'ALTER TABLE `workspace_classes` ADD COLUMN `workspace_id` bigint NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'workspaces' AND column_name = 'active') = 0, 'ALTER TABLE `workspaces` ADD COLUMN `active` bit(1) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'workspaces' AND column_name = 'end_at') = 0, 'ALTER TABLE `workspaces` ADD COLUMN `end_at` datetime(6) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'workspaces' AND column_name = 'name') = 0, 'ALTER TABLE `workspaces` ADD COLUMN `name` varchar(255) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'workspaces' AND column_name = 'semester') = 0, 'ALTER TABLE `workspaces` ADD COLUMN `semester` varchar(50) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'workspaces' AND column_name = 'start_at') = 0, 'ALTER TABLE `workspaces` ADD COLUMN `start_at` datetime(6) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'workspaces' AND column_name = 'status') = 0, 'ALTER TABLE `workspaces` ADD COLUMN `status` varchar(30) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'workspaces' AND column_name = 'type') = 0, 'ALTER TABLE `workspaces` ADD COLUMN `type` varchar(50) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'workspaces' AND column_name = 'department_id') = 0, 'ALTER TABLE `workspaces` ADD COLUMN `department_id` bigint NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------- 3. Chỉ mục / ràng buộc duy nhất còn thiếu
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'academic_classes' AND index_name = 'UKtpbla2txfyucgv7a0tbsyarm0') = 0, 'ALTER TABLE `academic_classes` ADD UNIQUE INDEX `UKtpbla2txfyucgv7a0tbsyarm0` (`department_id`,`code`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'departments' AND index_name = 'UKl7tivi5261wxdnvo6cct9gg6t') = 0, 'ALTER TABLE `departments` ADD UNIQUE INDEX `UKl7tivi5261wxdnvo6cct9gg6t` (`code`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'grade_history' AND index_name = 'idx_grade_history_reg') = 0, 'ALTER TABLE `grade_history` ADD INDEX `idx_grade_history_reg` (`registration_id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'grade_revision_requests' AND index_name = 'idx_grade_revision_reg_status') = 0, 'ALTER TABLE `grade_revision_requests` ADD INDEX `idx_grade_revision_reg_status` (`registration_id`,`status`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'lecturer_assignments' AND index_name = 'UK16a8pxyubeqt9e2vf9yt2t09i') = 0, 'ALTER TABLE `lecturer_assignments` ADD UNIQUE INDEX `UK16a8pxyubeqt9e2vf9yt2t09i` (`workspace_id`,`lecturer_id`,`type`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'privileges' AND index_name = 'UKm2tnonbcaquofx1ccy060ejyc') = 0, 'ALTER TABLE `privileges` ADD UNIQUE INDEX `UKm2tnonbcaquofx1ccy060ejyc` (`name`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'roles' AND index_name = 'UKofx66keruapi6vyqpv6f2or37') = 0, 'ALTER TABLE `roles` ADD UNIQUE INDEX `UKofx66keruapi6vyqpv6f2or37` (`name`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'stored_files' AND index_name = 'idx_stored_files_owner') = 0, 'ALTER TABLE `stored_files` ADD INDEX `idx_stored_files_owner` (`owner_id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'stored_files' AND index_name = 'idx_stored_files_ref') = 0, 'ALTER TABLE `stored_files` ADD INDEX `idx_stored_files_ref` (`ref_type`,`ref_id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'stored_files' AND index_name = 'UKjjd7hf5amerajq056e2nd5y31') = 0, 'ALTER TABLE `stored_files` ADD UNIQUE INDEX `UKjjd7hf5amerajq056e2nd5y31` (`stored_name`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'topic_registration' AND index_name = 'uk_registration_student_topic') = 0, 'ALTER TABLE `topic_registration` ADD UNIQUE INDEX `uk_registration_student_topic` (`student_id`,`topic_id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'users' AND index_name = 'UK6dotkott2kjsp8vw4d0m25fb7') = 0, 'ALTER TABLE `users` ADD UNIQUE INDEX `UK6dotkott2kjsp8vw4d0m25fb7` (`email`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'users' AND index_name = 'UKr43af9ap4edm43mmtq01oddj6') = 0, 'ALTER TABLE `users` ADD UNIQUE INDEX `UKr43af9ap4edm43mmtq01oddj6` (`username`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'workspace_classes' AND index_name = 'UK9rb4ruifhhpprs2s8dh6q93jj') = 0, 'ALTER TABLE `workspace_classes` ADD UNIQUE INDEX `UK9rb4ruifhhpprs2s8dh6q93jj` (`workspace_id`,`class_id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------- 4. Khóa ngoại còn thiếu
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'academic_classes' AND constraint_name = 'FKr6fncacuiempnms4qo75ovf1d' AND constraint_type = 'FOREIGN KEY') = 0, 'ALTER TABLE `academic_classes` ADD CONSTRAINT `FKr6fncacuiempnms4qo75ovf1d` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'announcements' AND constraint_name = 'FK1pgl63iwbqvmngumhvr3xopg3' AND constraint_type = 'FOREIGN KEY') = 0, 'ALTER TABLE `announcements` ADD CONSTRAINT `FK1pgl63iwbqvmngumhvr3xopg3` FOREIGN KEY (`author_id`) REFERENCES `users` (`id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'announcements' AND constraint_name = 'FKs4fe698qsc3f6utgex90gwf1q' AND constraint_type = 'FOREIGN KEY') = 0, 'ALTER TABLE `announcements` ADD CONSTRAINT `FKs4fe698qsc3f6utgex90gwf1q` FOREIGN KEY (`topic_id`) REFERENCES `topics` (`id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'calendar_events' AND constraint_name = 'FK2uebmbi4goljyd4bea8m5vnab' AND constraint_type = 'FOREIGN KEY') = 0, 'ALTER TABLE `calendar_events` ADD CONSTRAINT `FK2uebmbi4goljyd4bea8m5vnab` FOREIGN KEY (`topic_id`) REFERENCES `topics` (`id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'discussion_posts' AND constraint_name = 'FKm4m5leisx6to9ts8nuoekw7fn' AND constraint_type = 'FOREIGN KEY') = 0, 'ALTER TABLE `discussion_posts` ADD CONSTRAINT `FKm4m5leisx6to9ts8nuoekw7fn` FOREIGN KEY (`author_id`) REFERENCES `users` (`id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'discussion_posts' AND constraint_name = 'FKtcr686u3k5o3sama235eae1ff' AND constraint_type = 'FOREIGN KEY') = 0, 'ALTER TABLE `discussion_posts` ADD CONSTRAINT `FKtcr686u3k5o3sama235eae1ff` FOREIGN KEY (`thread_id`) REFERENCES `discussion_threads` (`id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'discussion_threads' AND constraint_name = 'FK4rf5rba76krjf8s7yx4v16l93' AND constraint_type = 'FOREIGN KEY') = 0, 'ALTER TABLE `discussion_threads` ADD CONSTRAINT `FK4rf5rba76krjf8s7yx4v16l93` FOREIGN KEY (`topic_id`) REFERENCES `topics` (`id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'discussion_threads' AND constraint_name = 'FKpvk346hab63rihnrudqu8tpn0' AND constraint_type = 'FOREIGN KEY') = 0, 'ALTER TABLE `discussion_threads` ADD CONSTRAINT `FKpvk346hab63rihnrudqu8tpn0` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'grade_history' AND constraint_name = 'FK2cdt7rpftl2u8p9bypq7lrw85' AND constraint_type = 'FOREIGN KEY') = 0, 'ALTER TABLE `grade_history` ADD CONSTRAINT `FK2cdt7rpftl2u8p9bypq7lrw85` FOREIGN KEY (`changed_by`) REFERENCES `users` (`id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'grade_history' AND constraint_name = 'FK3xpduiqx6lvpfl5tpvjy3yu47' AND constraint_type = 'FOREIGN KEY') = 0, 'ALTER TABLE `grade_history` ADD CONSTRAINT `FK3xpduiqx6lvpfl5tpvjy3yu47` FOREIGN KEY (`revision_request_id`) REFERENCES `grade_revision_requests` (`id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'grade_history' AND constraint_name = 'FK92353p6c96bn8kb0f20td7aen' AND constraint_type = 'FOREIGN KEY') = 0, 'ALTER TABLE `grade_history` ADD CONSTRAINT `FK92353p6c96bn8kb0f20td7aen` FOREIGN KEY (`registration_id`) REFERENCES `topic_registration` (`id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'grade_revision_requests' AND constraint_name = 'FK6bn8xcliy6w83l9wku5ioldsy' AND constraint_type = 'FOREIGN KEY') = 0, 'ALTER TABLE `grade_revision_requests` ADD CONSTRAINT `FK6bn8xcliy6w83l9wku5ioldsy` FOREIGN KEY (`reviewed_by`) REFERENCES `users` (`id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'grade_revision_requests' AND constraint_name = 'FK8ogvt8j2d5um0fwb81a1ae0t9' AND constraint_type = 'FOREIGN KEY') = 0, 'ALTER TABLE `grade_revision_requests` ADD CONSTRAINT `FK8ogvt8j2d5um0fwb81a1ae0t9` FOREIGN KEY (`requested_by`) REFERENCES `users` (`id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'grade_revision_requests' AND constraint_name = 'FKojkju9eopojixvcb3vn1u5rpr' AND constraint_type = 'FOREIGN KEY') = 0, 'ALTER TABLE `grade_revision_requests` ADD CONSTRAINT `FKojkju9eopojixvcb3vn1u5rpr` FOREIGN KEY (`registration_id`) REFERENCES `topic_registration` (`id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'lecturer_assignments' AND constraint_name = 'FKlvfgc661txvu3jncj3baafgh0' AND constraint_type = 'FOREIGN KEY') = 0, 'ALTER TABLE `lecturer_assignments` ADD CONSTRAINT `FKlvfgc661txvu3jncj3baafgh0` FOREIGN KEY (`lecturer_id`) REFERENCES `lecturers` (`id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'lecturer_assignments' AND constraint_name = 'FKnq9fgadg0uios2y0b1y1yq60w' AND constraint_type = 'FOREIGN KEY') = 0, 'ALTER TABLE `lecturer_assignments` ADD CONSTRAINT `FKnq9fgadg0uios2y0b1y1yq60w` FOREIGN KEY (`workspace_id`) REFERENCES `workspaces` (`id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'lecturers' AND constraint_name = 'FK7d4qvhqq0sr5qryod4h5tcvy5' AND constraint_type = 'FOREIGN KEY') = 0, 'ALTER TABLE `lecturers` ADD CONSTRAINT `FK7d4qvhqq0sr5qryod4h5tcvy5` FOREIGN KEY (`id`) REFERENCES `users` (`id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'lecturers' AND constraint_name = 'FKpowy6g9rglsdytsdu6c9fymox' AND constraint_type = 'FOREIGN KEY') = 0, 'ALTER TABLE `lecturers` ADD CONSTRAINT `FKpowy6g9rglsdytsdu6c9fymox` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'messages' AND constraint_name = 'FK31v0eu0kks5tp34l1p11290ox' AND constraint_type = 'FOREIGN KEY') = 0, 'ALTER TABLE `messages` ADD CONSTRAINT `FK31v0eu0kks5tp34l1p11290ox` FOREIGN KEY (`workspace_id`) REFERENCES `workspaces` (`id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'messages' AND constraint_name = 'FK4ui4nnwntodh6wjvck53dbk9m' AND constraint_type = 'FOREIGN KEY') = 0, 'ALTER TABLE `messages` ADD CONSTRAINT `FK4ui4nnwntodh6wjvck53dbk9m` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'messages' AND constraint_name = 'FKhdkwfnspwb3s60j27vpg0rpg6' AND constraint_type = 'FOREIGN KEY') = 0, 'ALTER TABLE `messages` ADD CONSTRAINT `FKhdkwfnspwb3s60j27vpg0rpg6` FOREIGN KEY (`recipient_id`) REFERENCES `users` (`id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'messages' AND constraint_name = 'FKkoe6xlcl0wxc7d5ckn58y1x0c' AND constraint_type = 'FOREIGN KEY') = 0, 'ALTER TABLE `messages` ADD CONSTRAINT `FKkoe6xlcl0wxc7d5ckn58y1x0c` FOREIGN KEY (`topic_id`) REFERENCES `topics` (`id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'milestones' AND constraint_name = 'FKnggvk4i34f17w7qy8jyj7r4jt' AND constraint_type = 'FOREIGN KEY') = 0, 'ALTER TABLE `milestones` ADD CONSTRAINT `FKnggvk4i34f17w7qy8jyj7r4jt` FOREIGN KEY (`topic_id`) REFERENCES `topics` (`id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'notifications' AND constraint_name = 'FK9y21adhxn0ayjhfocscqox7bh' AND constraint_type = 'FOREIGN KEY') = 0, 'ALTER TABLE `notifications` ADD CONSTRAINT `FK9y21adhxn0ayjhfocscqox7bh` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'otp_tokens' AND constraint_name = 'FKjyk2kyj8ul2bjqjpentlrpyi1' AND constraint_type = 'FOREIGN KEY') = 0, 'ALTER TABLE `otp_tokens` ADD CONSTRAINT `FKjyk2kyj8ul2bjqjpentlrpyi1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'progress_report' AND constraint_name = 'FKb8w6aglicr9nk6gly2qb53h6f' AND constraint_type = 'FOREIGN KEY') = 0, 'ALTER TABLE `progress_report` ADD CONSTRAINT `FKb8w6aglicr9nk6gly2qb53h6f` FOREIGN KEY (`topic_id`) REFERENCES `topics` (`id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'progress_report' AND constraint_name = 'FKo1e7ufhbx0pt1wpv1hapvuhto' AND constraint_type = 'FOREIGN KEY') = 0, 'ALTER TABLE `progress_report` ADD CONSTRAINT `FKo1e7ufhbx0pt1wpv1hapvuhto` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'progress_report' AND constraint_name = 'FKq7cpn58kdpb93a1hxu6hjvo25' AND constraint_type = 'FOREIGN KEY') = 0, 'ALTER TABLE `progress_report` ADD CONSTRAINT `FKq7cpn58kdpb93a1hxu6hjvo25` FOREIGN KEY (`milestone_id`) REFERENCES `milestones` (`id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'roles_privileges' AND constraint_name = 'FK5duhoc7rwt8h06avv41o41cfy' AND constraint_type = 'FOREIGN KEY') = 0, 'ALTER TABLE `roles_privileges` ADD CONSTRAINT `FK5duhoc7rwt8h06avv41o41cfy` FOREIGN KEY (`privilege_id`) REFERENCES `privileges` (`id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'roles_privileges' AND constraint_name = 'FK629oqwrudgp5u7tewl07ayugj' AND constraint_type = 'FOREIGN KEY') = 0, 'ALTER TABLE `roles_privileges` ADD CONSTRAINT `FK629oqwrudgp5u7tewl07ayugj` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'students' AND constraint_name = 'FK7xqmtv7r2eb5axni3jm0a80su' AND constraint_type = 'FOREIGN KEY') = 0, 'ALTER TABLE `students` ADD CONSTRAINT `FK7xqmtv7r2eb5axni3jm0a80su` FOREIGN KEY (`id`) REFERENCES `users` (`id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'students' AND constraint_name = 'FKpe06fepkeagn2w6pppj10xf68' AND constraint_type = 'FOREIGN KEY') = 0, 'ALTER TABLE `students` ADD CONSTRAINT `FKpe06fepkeagn2w6pppj10xf68` FOREIGN KEY (`class_id`) REFERENCES `academic_classes` (`id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'topic_registration' AND constraint_name = 'FK10j7yem7erfd8odad81rtsnw7' AND constraint_type = 'FOREIGN KEY') = 0, 'ALTER TABLE `topic_registration` ADD CONSTRAINT `FK10j7yem7erfd8odad81rtsnw7` FOREIGN KEY (`topic_id`) REFERENCES `topics` (`id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'topic_registration' AND constraint_name = 'FK787dgaa34uitlx5231w5kyqom' AND constraint_type = 'FOREIGN KEY') = 0, 'ALTER TABLE `topic_registration` ADD CONSTRAINT `FK787dgaa34uitlx5231w5kyqom` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'topic_registration' AND constraint_name = 'FKgagrhg6cx8bjxs0551kc7a9vs' AND constraint_type = 'FOREIGN KEY') = 0, 'ALTER TABLE `topic_registration` ADD CONSTRAINT `FKgagrhg6cx8bjxs0551kc7a9vs` FOREIGN KEY (`reviewer_id`) REFERENCES `lecturers` (`id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'topics' AND constraint_name = 'FK20bsyno5jeliiync88nsj7v0d' AND constraint_type = 'FOREIGN KEY') = 0, 'ALTER TABLE `topics` ADD CONSTRAINT `FK20bsyno5jeliiync88nsj7v0d` FOREIGN KEY (`workspace_id`) REFERENCES `workspaces` (`id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'topics' AND constraint_name = 'FK2rrx0i1r6ecobj5uuu4t83p6m' AND constraint_type = 'FOREIGN KEY') = 0, 'ALTER TABLE `topics` ADD CONSTRAINT `FK2rrx0i1r6ecobj5uuu4t83p6m` FOREIGN KEY (`lecturer_id`) REFERENCES `lecturers` (`id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'users' AND constraint_name = 'FKp56c1712k691lhsyewcssf40f' AND constraint_type = 'FOREIGN KEY') = 0, 'ALTER TABLE `users` ADD CONSTRAINT `FKp56c1712k691lhsyewcssf40f` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'users' AND constraint_name = 'FKsbg59w8q63i0oo53rlgvlcnjq' AND constraint_type = 'FOREIGN KEY') = 0, 'ALTER TABLE `users` ADD CONSTRAINT `FKsbg59w8q63i0oo53rlgvlcnjq` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'workspace_classes' AND constraint_name = 'FKpcjqw35ttovnlvvh1n2pou4ul' AND constraint_type = 'FOREIGN KEY') = 0, 'ALTER TABLE `workspace_classes` ADD CONSTRAINT `FKpcjqw35ttovnlvvh1n2pou4ul` FOREIGN KEY (`class_id`) REFERENCES `academic_classes` (`id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'workspace_classes' AND constraint_name = 'FKrywthrafxc1ya467dodhrybn6' AND constraint_type = 'FOREIGN KEY') = 0, 'ALTER TABLE `workspace_classes` ADD CONSTRAINT `FKrywthrafxc1ya467dodhrybn6` FOREIGN KEY (`workspace_id`) REFERENCES `workspaces` (`id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'workspaces' AND constraint_name = 'FKmy77je4f9pwmh064o2bqu07s2' AND constraint_type = 'FOREIGN KEY') = 0, 'ALTER TABLE `workspaces` ADD CONSTRAINT `FKmy77je4f9pwmh064o2bqu07s2` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`)', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------- 5. Cột ENUM: đủ giá trị hiện tại
ALTER TABLE `otp_tokens` MODIFY COLUMN `purpose` enum('ACTIVATION','LOGIN','RESET_PASSWORD') NOT NULL DEFAULT 'LOGIN';

-- Thay SchemaFixer: approved = NULL nghĩa là "chờ duyệt"
ALTER TABLE `topic_registration` MODIFY COLUMN `approved` tinyint(1) NULL;

SET FOREIGN_KEY_CHECKS = 1;
