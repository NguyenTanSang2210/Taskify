-- P4-01: thời điểm giảng viên nhận xét báo cáo tiến độ LẦN ĐẦU — để đo thời gian phản hồi trung bình của giảng viên.
-- Báo cáo đã nhận xét trước bản này không có thời điểm (NULL) và không được tính vào trung bình.
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'progress_report' AND column_name = 'commented_at') = 0, 'ALTER TABLE `progress_report` ADD COLUMN `commented_at` datetime(6) NULL', 'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
