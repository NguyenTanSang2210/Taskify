-- P3-14: refresh token (cookie HttpOnly), chỉ lưu SHA-256 của token; xoay vòng mỗi lần dùng.
-- family_id: chuỗi token sinh từ cùng một lần đăng nhập; replaced_by_id: token kế tiếp sau khi xoay vòng
-- (token đã bị thay mà còn bị dùng lại = dấu hiệu bị đánh cắp → thu hồi toàn bộ phiên của người dùng).
CREATE TABLE IF NOT EXISTS `refresh_tokens` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `token_hash` varchar(64) NOT NULL,
  `family_id` varchar(36) NOT NULL,
  `token_version` int NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `revoked_at` datetime(6) DEFAULT NULL,
  `replaced_by_id` bigint DEFAULT NULL,
  `user_agent` varchar(255) DEFAULT NULL,
  `ip` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refresh_token_hash` (`token_hash`),
  KEY `idx_refresh_user` (`user_id`),
  KEY `idx_refresh_family` (`family_id`),
  KEY `idx_refresh_expires` (`expires_at`),
  CONSTRAINT `fk_refresh_token_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
