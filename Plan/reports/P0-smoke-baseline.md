# Kết quả smoke test — 2026-09-25T07:55:13.631Z

API: `http://localhost:8081/api` · WEB: `http://localhost:5175` · OK=17  BUG(đã biết)=12  FIXED=0  FAIL=0  — tổng 29

| # | Kịch bản | Kết quả | Task | Chi tiết |
|---|---|:---:|---|---|
| 1a | Token ADMIN dùng được TRƯỚC khi xác thực OTP | BUG | P1-01 | GET /users bằng token chưa qua OTP → HTTP 200 |
| 1 | Admin đăng nhập + OTP | OK |  | role=ADMIN, otpRequired=true |
| 2 | Admin tạo khoa và tạo giảng viên mới | OK |  | khoa T896348, giảng viên lect896348 |
| 3 | DeptAdmin tạo workspace → OPEN_TOPIC | OK |  | workspace #4 status=OPEN_TOPIC |
| 4 | DeptAdmin gán lớp CNTT-K15 + phân công giảng viên | OK |  | lớp #1, GV #3 |
| 5 | Giảng viên tự tạo đề tài | BUG | P2-01 | HTTP 403 — thiếu quyền TOPIC_MANAGE (frontend sẽ đăng xuất GV) |
| 5b | DeptAdmin tạo đề tài thay giảng viên (đường vòng) | OK |  | đề tài #3, capacity=3 |
| 6 | DeptAdmin chuyển workspace → OPEN_REGISTRATION | OK |  | status=OPEN_REGISTRATION |
| 7 | Student đăng ký đề tài | OK |  | đăng ký #5, approved=null (chờ duyệt) |
| 8 | Student2 đăng ký cùng đề tài (capacity=3) | OK |  | đăng ký #6 |
| 9a | Giảng viên duyệt Student | OK |  | đăng ký #5 approved=true |
| 9b | Giảng viên duyệt Student2 (đề tài capacity=3) | BUG | P2-04 | HTTP 400 Topic already has an approved registration |
| 10 | DeptAdmin → LOCK_REGISTRATION → IN_PROGRESS | OK |  | status=IN_PROGRESS |
| 11 | Nộp báo cáo kèm file từ trang Không gian dự án | BUG | P2-03 | gửi fileUrl dạng object như ProjectSpacePage.jsx:126 → HTTP 400 |
| 12 | Upload file + nộp báo cáo (trang Chi tiết tiến độ) | OK |  | báo cáo #3, file /api/secure/files/abd22052-d5a4-4007-93a0-e127afd469a2_smoke-896348.txt |
| 13a | Tải file bằng link <a href> (không có header Authorization) | BUG | P2-03 | HTTP 403 |
| 13b | Giảng viên tải file có kèm token | OK |  | 23 bytes |
| 14 | Giảng viên nhận xét → sinh viên nhận thông báo | OK |  | thông báo #14 |
| 15 | Sinh viên đổi trạng thái Kanban | OK |  | status=IN_PROGRESS |
| 16 | Chat giảng viên → sinh viên | OK |  | sinh viên nhận được tin nhắn |
| 17 | Giảng viên chấm điểm | OK |  | score=8.5 |
| 18 | Giảng viên xuất Excel | OK |  | 3572 bytes xlsx |
| 19 | Admin khóa tài khoản → token cũ của sinh viên còn dùng được? | BUG | P1-02 | sau khi khóa: HTTP 200 (đã mở khóa lại tài khoản) |
| 20 | Chức năng Quên mật khẩu | BUG | P2-05 | POST /auth/password/forgot → HTTP 404 |
| 21 | Upload file 2MB qua nginx (cổng frontend) | BUG | P2-03 | HTTP 413 |
| S1 | Sinh viên đọc toàn bộ audit log | BUG | P1-05 #12 | HTTP 200, 64 bản ghi |
| S2 | Giảng viên khác tạo milestone trên đề tài không phải của mình | BUG | P1-05 #1 | HTTP 200 |
| S3 | Sinh viên xem email/SĐT của người khác qua /users/{id} | BUG | P1-05 #13 | HTTP 200, email=lecturer@example.com |
| S4 | Tự đăng ký tài khoản công khai /auth/register | BUG | P1-09 | HTTP 200 — tạo được tài khoản với mật khẩu '1' |
