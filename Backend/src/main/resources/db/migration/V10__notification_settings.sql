-- P4-06: cài đặt email thông báo của từng người dùng + hàng chờ email tổng hợp hằng ngày.
-- Không có dòng cài đặt = mặc định (mọi nhóm bật, gửi ngay). Email bảo mật (OTP, kích hoạt, đặt lại mật khẩu) luôn gửi ngay.
CREATE TABLE IF NOT EXISTS `notification_settings` (
  `user_id` bigint NOT NULL,
  `email_digest` bit(1) NOT NULL DEFAULT b'0',
  `email_registration` bit(1) NOT NULL DEFAULT b'1',
  `email_progress` bit(1) NOT NULL DEFAULT b'1',
  `email_grade` bit(1) NOT NULL DEFAULT b'1',
  `email_reminder` bit(1) NOT NULL DEFAULT b'1',
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`user_id`),
  CONSTRAINT `fk_notification_settings_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `email_digest_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `category` varchar(20) NOT NULL,
  `subject` varchar(255) NOT NULL,
  `body` text NOT NULL,
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_digest_user` (`user_id`, `created_at`),
  CONSTRAINT `fk_digest_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
