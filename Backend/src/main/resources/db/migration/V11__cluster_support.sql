-- P4-10: hỗ trợ chạy nhiều instance backend.

-- ShedLock: mỗi job định kỳ (nhắc hạn, email tổng hợp, dọn dữ liệu...) chỉ chạy trên MỘT instance mỗi lượt.
CREATE TABLE IF NOT EXISTS `shedlock` (
  `name` varchar(64) NOT NULL,
  `lock_until` timestamp(3) NOT NULL,
  `locked_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `locked_by` varchar(255) NOT NULL,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Giới hạn tần suất dùng chung (app.rate-limit.store=jdbc): bộ đếm cửa sổ cố định theo (nhóm|khóa), vd. "login|1.2.3.4".
CREATE TABLE IF NOT EXISTS `rate_limit_windows` (
  `bucket_key` varchar(191) NOT NULL,
  `window_end_ms` bigint NOT NULL,
  `hits` int NOT NULL,
  PRIMARY KEY (`bucket_key`),
  KEY `idx_rate_limit_window_end` (`window_end_ms`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
