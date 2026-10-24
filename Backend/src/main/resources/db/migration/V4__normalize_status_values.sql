-- =====================================================================================================
-- V4 — P3-04: trạng thái chuyển sang enum (cột vẫn là varchar). Chuẩn hóa dữ liệu cũ để Hibernate đọc được:
-- bỏ khoảng trắng / chữ thường, đổi tên cũ, giá trị rỗng hoặc lạ về giá trị an toàn.
-- =====================================================================================================

-- Đợt: "OPEN" là tên cũ của OPEN_TOPIC; giá trị lạ → DRAFT (đợt phải được quản trị khoa mở lại)
UPDATE `workspaces` SET `status` = UPPER(TRIM(`status`)) WHERE `status` IS NOT NULL;
UPDATE `workspaces` SET `status` = 'OPEN_TOPIC' WHERE `status` = 'OPEN';
UPDATE `workspaces` SET `status` = 'DRAFT'
WHERE `status` IS NULL
   OR `status` NOT IN ('DRAFT', 'OPEN_TOPIC', 'OPEN_REGISTRATION', 'LOCK_REGISTRATION', 'IN_PROGRESS', 'CLOSED');

-- Đề tài: giá trị lạ (vd. REGISTERED còn sót) → tính lại OPEN/FULL theo số SV đã duyệt
UPDATE `topics` SET `status` = UPPER(TRIM(`status`)) WHERE `status` IS NOT NULL;
UPDATE `topics` t
SET t.status = CASE
        WHEN (SELECT COUNT(*) FROM `topic_registration` r WHERE r.topic_id = t.id AND r.approved = 1)
             >= GREATEST(COALESCE(t.capacity, 1), 1) THEN 'FULL'
        ELSE 'OPEN' END
WHERE t.status IS NULL OR t.status NOT IN ('OPEN', 'FULL', 'CLOSED', 'COMPLETED');

UPDATE `progress_report` SET `status` = UPPER(TRIM(`status`));
UPDATE `progress_report` SET `status` = 'TODO' WHERE `status` NOT IN ('TODO', 'IN_PROGRESS', 'DONE');

UPDATE `notifications` SET `type` = UPPER(TRIM(`type`));
UPDATE `notifications` SET `type` = 'INFO' WHERE `type` NOT IN ('INFO', 'REMINDER', 'SYSTEM');
UPDATE `notifications` SET `severity` = 'INFO'
WHERE `severity` IS NULL OR UPPER(TRIM(`severity`)) NOT IN ('INFO', 'WARNING', 'CRITICAL');
UPDATE `notifications` SET `severity` = UPPER(TRIM(`severity`));

UPDATE `calendar_events` SET `type` = 'MEETING'
WHERE `type` IS NULL OR UPPER(TRIM(`type`)) NOT IN ('MEETING', 'MILESTONE', 'DEADLINE');
UPDATE `calendar_events` SET `type` = UPPER(TRIM(`type`));
