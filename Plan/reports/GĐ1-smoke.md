# Kết quả smoke test — 2026-10-01T03:26:48.396Z

API: `http://localhost:8081/api` · WEB: `http://localhost:5175` · OK=18  BUG(đã biết)=6  FIXED=6  SKIP=0  FAIL=0  — tổng 30

| # | Kịch bản | Kết quả | Task | Chi tiết |
|---|---|:---:|---|---|
| 1a | Đăng nhập ADMIN (chưa nhập OTP) có nhận được token dùng được không? | FIXED | P1-01 | token trong phản hồi: không; GET /users bằng token/preAuthToken → HTTP 401 |
| 1 | Admin đăng nhập + OTP | OK |  | role=ADMIN, otpRequired=true |
| 2 | Admin tạo khoa và tạo giảng viên mới | OK |  | khoa T188143, giảng viên lect188143 (đã kích hoạt qua mã) |
| 3 | DeptAdmin tạo workspace → OPEN_TOPIC | OK |  | workspace #14 status=OPEN_TOPIC |
| 4 | DeptAdmin gán lớp CNTT-K15 + phân công giảng viên | OK |  | lớp #1, GV #3 |
| 5 | Giảng viên tự tạo đề tài | BUG | P2-01 | HTTP 403 — thiếu quyền TOPIC_MANAGE |
| 5b | DeptAdmin tạo đề tài thay giảng viên (đường vòng) | OK |  | đề tài #13, capacity=3 |
| 6 | DeptAdmin chuyển workspace → OPEN_REGISTRATION | OK |  | status=OPEN_REGISTRATION |
| 7 | Student đăng ký đề tài | OK |  | đăng ký #25, approved=null (chờ duyệt) |
| 8 | Student2 đăng ký cùng đề tài (capacity=3) | OK |  | đăng ký #26 |
| 9a | Giảng viên duyệt Student | OK |  | đăng ký #25 approved=true |
| 9b | Giảng viên duyệt Student2 (đề tài capacity=3) | BUG | P2-04 | HTTP 400 Topic already has an approved registration |
| 10 | DeptAdmin → LOCK_REGISTRATION → IN_PROGRESS | OK |  | status=IN_PROGRESS |
| 11 | Frontend gán nguyên object trả về của /upload vào fileUrl khi nộp báo cáo | BUG | P2-03 | còn 2 chỗ: frontend/src/pages/ProjectSpacePage.jsx:126, frontend/src/pages/StudentProgressPage.jsx:344 — API upload trả object {fileUrl,...} nên body báo cáo bị 400 |
| 12 | Upload file + nộp báo cáo (trang Chi tiết tiến độ) | OK |  | báo cáo #17, file /api/secure/files/6cb77a4a-2e22-4358-8b4d-fc078c461291.txt |
| 13a | Frontend tải file bằng <a href> (trình duyệt không gửi header Authorization) | BUG | P2-03 | còn 5 chỗ: frontend/src/components/ProgressRow.jsx:49, frontend/src/pages/ProjectSpacePage.jsx:454, frontend/src/pages/StudentProgressDetailPage.jsx:276, frontend/src/pages/StudentProgressPage.jsx:135, frontend/src/pages/StudentProgressPage.jsx:631 |
| 13c | Backend từ chối tải file khi KHÔNG có token (401) | OK |  | HTTP 401 |
| 13b | Giảng viên tải file có kèm token | OK |  | 23 bytes |
| 14 | Giảng viên nhận xét → sinh viên nhận thông báo | OK |  | thông báo #72 |
| 15 | Sinh viên đổi trạng thái Kanban | OK |  | status=IN_PROGRESS |
| 16 | Chat giảng viên → sinh viên | OK |  | sinh viên nhận được tin nhắn |
| 17 | Giảng viên chấm điểm | OK |  | score=8.5 |
| 18 | Giảng viên xuất Excel | OK |  | 3574 bytes xlsx |
| 19 | Admin khóa tài khoản → token cũ của sinh viên còn dùng được? | FIXED | P1-02 | sau khi khóa: HTTP 401 (đã mở khóa lại tài khoản) |
| 20 | Chức năng Quên mật khẩu | BUG | P2-05 | POST /auth/password/forgot → HTTP 404 |
| 21 | Upload file 2MB qua nginx (cổng frontend) | BUG | P2-03 | HTTP 413 |
| S1 | Sinh viên đọc toàn bộ audit log | FIXED | P1-05 #12 | HTTP 403 |
| S2 | Giảng viên khác tạo milestone trên đề tài không phải của mình | FIXED | P1-05 #1 | HTTP 403 |
| S3 | Sinh viên xem email/SĐT của người khác qua /users/{id} | FIXED | P1-05 #13 | HTTP 200, email=— |
| S4 | Tự đăng ký tài khoản công khai /auth/register | FIXED | P1-09 | HTTP 401 |
