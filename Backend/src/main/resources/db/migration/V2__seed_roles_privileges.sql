-- =====================================================================================================
-- V2 — Quyền và 4 vai trò hệ thống (thay RolePrivilegeInitializer).
-- Quyền mặc định CHỈ gán cho vai trò được tạo mới ở đây; vai trò đã có giữ nguyên quyền quản trị viên đã chỉnh.
-- Quyền mới sau này: thêm bằng migration mới (V4, V5…), không sửa file này.
-- =====================================================================================================

INSERT INTO `privileges` (`name`, `description`)
SELECT v.name, v.description
FROM (SELECT 'USER_MANAGE' AS name, 'Quản lý người dùng (Xem, Thêm, Sửa, Xóa)' AS description
      UNION ALL SELECT 'ROLE_MANAGE', 'Quản lý vai trò và quyền hạn'
      UNION ALL SELECT 'DEPT_MANAGE', 'Quản lý khoa và lớp học'
      UNION ALL SELECT 'TOPIC_MANAGE', 'Quản lý đề tài (Tạo, Sửa, Đóng/Mở)'
      UNION ALL SELECT 'REGISTRATION_MANAGE', 'Duyệt đăng ký đề tài'
      UNION ALL SELECT 'SYSTEM_REPORT', 'Xem báo cáo và thống kê hệ thống') v
WHERE NOT EXISTS (SELECT 1 FROM `privileges` p WHERE p.name = v.name);

-- Vai trò chưa có trong DB — chỉ những vai trò này nhận quyền mặc định
CREATE TEMPORARY TABLE `tmp_new_roles` (`name` varchar(50) PRIMARY KEY, `description` varchar(255));
INSERT INTO `tmp_new_roles` (`name`, `description`)
SELECT v.name, v.description
FROM (SELECT 'ADMIN' AS name, 'Quản trị viên toàn hệ thống' AS description
      UNION ALL SELECT 'DEPARTMENT_ADMIN', 'Quản trị viên cấp khoa'
      UNION ALL SELECT 'LECTURER', 'Giảng viên hướng dẫn'
      UNION ALL SELECT 'STUDENT', 'Sinh viên thực hiện đề tài') v
WHERE NOT EXISTS (SELECT 1 FROM `roles` r WHERE r.name = v.name);

INSERT INTO `roles` (`name`, `description`) SELECT `name`, `description` FROM `tmp_new_roles`;

-- Giảng viên chỉ thao tác trên đề tài/đăng ký của mình — phạm vi kiểm tra ở tầng service
INSERT INTO `roles_privileges` (`role_id`, `privilege_id`)
SELECT r.id, p.id
FROM `tmp_new_roles` n
JOIN `roles` r ON r.name = n.name
JOIN (SELECT 'ADMIN' AS role_name, 'USER_MANAGE' AS privilege_name
      UNION ALL SELECT 'ADMIN', 'ROLE_MANAGE'
      UNION ALL SELECT 'ADMIN', 'DEPT_MANAGE'
      UNION ALL SELECT 'ADMIN', 'TOPIC_MANAGE'
      UNION ALL SELECT 'ADMIN', 'REGISTRATION_MANAGE'
      UNION ALL SELECT 'ADMIN', 'SYSTEM_REPORT'
      UNION ALL SELECT 'DEPARTMENT_ADMIN', 'DEPT_MANAGE'
      UNION ALL SELECT 'DEPARTMENT_ADMIN', 'TOPIC_MANAGE'
      UNION ALL SELECT 'DEPARTMENT_ADMIN', 'REGISTRATION_MANAGE'
      UNION ALL SELECT 'DEPARTMENT_ADMIN', 'SYSTEM_REPORT'
      UNION ALL SELECT 'LECTURER', 'TOPIC_MANAGE'
      UNION ALL SELECT 'LECTURER', 'REGISTRATION_MANAGE') d ON d.role_name = n.name
JOIN `privileges` p ON p.name = d.privilege_name;

DROP TEMPORARY TABLE `tmp_new_roles`;
