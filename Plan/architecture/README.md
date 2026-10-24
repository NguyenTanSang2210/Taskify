# Bộ sơ đồ kiến trúc & luồng nghiệp vụ Taskify

> Lập ngày 24/10/2026 từ mã nguồn hiện tại (nhánh `develop`, sau commit `b6bf889` — đóng Giai đoạn 3).
> Sơ đồ viết bằng **Mermaid**: xem trực tiếp trên GitHub, hoặc trong VS Code bằng Markdown Preview (cần extension *Markdown Preview Mermaid Support*), hoặc mở file [`so-do-kien-truc.html`](so-do-kien-truc.html) bằng trình duyệt (gộp toàn bộ tài liệu, tự vẽ sơ đồ).

## Mục lục

| File | Nội dung |
|---|---|
| [01-tong-quan-he-thong.md](01-tong-quan-he-thong.md) | Ngữ cảnh hệ thống, triển khai Docker, chế độ dev, đường đi của một request, vòng đời trọn một đợt đồ án (luồng lớn nhất) |
| [02-kien-truc-backend.md](02-kien-truc-backend.md) | Phân tầng, bản đồ controller → service → repository, chuỗi filter bảo mật, 3 lớp phân quyền, realtime, email, job định kỳ, xử lý lỗi, quan sát, mô hình dữ liệu, mọi máy trạng thái |
| [03-kien-truc-frontend.md](03-kien-truc-frontend.md) | Khởi động ứng dụng, quản lý phiên, interceptor axios, WebSocket client, route theo vai trò, bản đồ trang → component → API, các API chưa có giao diện |
| [04-luong-xac-thuc.md](04-luong-xac-thuc.md) | A — Đăng nhập, OTP, đổi mật khẩu lần đầu, kích hoạt, quên mật khẩu, làm mới phiên, đăng xuất, tạo Admin đầu tiên |
| [05-luong-quan-tri.md](05-luong-quan-tri.md) | B — Tài khoản, nhập Excel, khoa, lớp, vai trò & quyền, hồ sơ, cài đặt email |
| [06-luong-dot-de-tai-dang-ky.md](06-luong-dot-de-tai-dang-ky.md) | C — Đợt đồ án · D — Đề tài · E — Đăng ký đề tài |
| [07-luong-tien-do-moc.md](07-luong-tien-do-moc.md) | F — Tải file, báo cáo tiến độ, Kanban, nhận xét, kiểm tra trùng lặp · G — Mốc, nộp theo mốc, chấm điểm mốc |
| [08-luong-cham-diem.md](08-luong-cham-diem.md) | H — Chấm điểm tổng kết, khóa điểm, chấm lại, lịch sử điểm, xuất Excel |
| [09-luong-giao-tiep-thong-bao.md](09-luong-giao-tiep-thong-bao.md) | I — Tin nhắn, thông báo trong app, email (ngay / tổng hợp), thảo luận, thông báo chung, lịch |
| [10-luong-bao-cao-van-hanh.md](10-luong-bao-cao-van-hanh.md) | J — Dashboard & thống kê · K — Nhật ký kiểm toán, nhắc hạn tự động, job dọn dẹp, giới hạn tần suất, quan sát, CI/CD |

## Số liệu dự án (đếm từ mã nguồn)

| Hạng mục | Số lượng |
|---|---|
| Controller REST | 23 (113 endpoint) |
| Service | 45 (+ 3 lớp `importer`) |
| Entity / bảng nghiệp vụ | 31 entity, 9 enum trạng thái |
| Repository | 31 |
| Migration Flyway | V1 → V10 |
| Job định kỳ (`@Scheduled`) | 9 |
| Kênh realtime STOMP | 4 |
| Trang React (route) | 17 trang + 6 bước đăng nhập |
| Module API phía frontend | 20 file `src/api/*` |
| Test backend | 66 file (325 test) |

## Danh mục chức năng (từ nhỏ tới lớn)

Ký hiệu cột **UI**: ✅ có giao diện · ⚠️ chỉ có API (frontend có/không có hàm gọi nhưng chưa trang nào dùng).

### A. Xác thực & phiên đăng nhập — [04](04-luong-xac-thuc.md)
| Mã | Chức năng | Vai trò | Endpoint chính | UI |
|---|---|---|---|---|
| A1 | Kiểm tra chính sách mật khẩu (8–64 ký tự, ≤72 byte, có chữ + số, không chứa username) | mọi | dùng nội bộ + `PasswordChecklist` | ✅ |
| A2 | Đăng nhập bằng mật khẩu (giới hạn IP + khóa cặp username/IP) | mọi | `POST /api/auth/login` | ✅ |
| A3 | Xác thực 2 bước OTP email (ADMIN, DEPARTMENT_ADMIN, LECTURER) | 3 vai trò quản lý | `POST /api/auth/otp/verify`, `/otp/resend` | ✅ |
| A4 | Đổi mật khẩu bắt buộc khi dùng mật khẩu tạm | mọi | `POST /api/auth/password/initial` | ✅ |
| A5 | Kích hoạt tài khoản bằng mã email (72 giờ) | mọi | `POST /api/auth/activate` | ✅ |
| A6 | Quên mật khẩu → đặt lại bằng mã (10 phút) | mọi | `POST /api/auth/password/forgot`, `/reset` | ✅ |
| A7 | Làm mới phiên (refresh token xoay vòng, phát hiện dùng lại) | mọi | `POST /api/auth/refresh` | ✅ (tự động) |
| A8 | Khôi phục phiên khi tải lại trang, làm mới chủ động, đồng bộ nhiều tab | mọi | `/auth/refresh` | ✅ (tự động) |
| A9 | Đăng xuất phiên hiện tại | mọi | `POST /api/auth/logout` | ✅ |
| A10 | Đăng xuất mọi thiết bị | mọi | `POST /api/auth/logout-all` | ⚠️ |
| A11 | Xác thực từng request bằng JWT + `tokenVersion` | mọi | filter | ✅ |
| A12 | Tạo ADMIN đầu tiên cho bản cài mới | hệ thống | biến môi trường | — |

### B. Quản trị người dùng & tổ chức — [05](05-luong-quan-tri.md)
| Mã | Chức năng | Vai trò | Endpoint chính | UI |
|---|---|---|---|---|
| B1 | Tạo tài khoản (mật khẩu tạm **hoặc** mã kích hoạt) + hồ sơ SV/GV | ADMIN | `POST /api/users` | ✅ |
| B2 | Danh bạ, tìm kiếm, lọc vai trò, phân trang | ADMIN, DEPT_ADMIN | `GET /api/users`, `/users/by-department` | ✅ |
| B3 | Khóa / mở tài khoản (thu hồi token ngay) | ADMIN | `PUT /api/users/{id}/status` | ✅ |
| B4 | Đổi vai trò (thu hồi token mang vai trò cũ) | ADMIN | `PUT /api/users/{id}/role` | ✅ |
| B5 | Gửi lại mã kích hoạt (chờ 60 giây) | ADMIN | `POST /api/users/{id}/resend-activation` | ✅ |
| B6 | Nhập SV/GV từ Excel: tải mẫu → xem trước → nhập | ADMIN, DEPT_ADMIN | `/api/users/import/*` | ✅ |
| B7 | Quản lý khoa (tạo, sửa, bật/tắt) | ADMIN | `/api/departments` | ✅ tạo · ⚠️ sửa/bật-tắt |
| B8 | Quản lý lớp sinh hoạt | ADMIN, DEPT_ADMIN | `/api/classes` | ✅ tạo · ⚠️ sửa |
| B9 | Vai trò & quyền hạn (tạo, gán quyền, xóa vai trò không phải hệ thống) | ADMIN | `/api/roles` | ✅ · ⚠️ tạo quyền |
| B10 | Hồ sơ cá nhân: xem, sửa, đổi mật khẩu | mọi | `/api/users/{id}`, `/profile`, `/password` | ✅ |
| B11 | Cài đặt email thông báo (bật/tắt nhóm, gửi ngay / tổng hợp) | mọi | `/api/notification-settings` | ✅ |

### C. Đợt đồ án (workspace) — [06](06-luong-dot-de-tai-dang-ky.md)
| Mã | Chức năng | Vai trò | Endpoint chính | UI |
|---|---|---|---|---|
| C1 | Tạo đợt | ADMIN, DEPT_ADMIN | `POST /api/workspaces` | ✅ |
| C2 | Sửa đợt (trường quan trọng chỉ khi DRAFT) | ADMIN, DEPT_ADMIN | `PUT /api/workspaces/{id}` | ⚠️ |
| C3 | Chuyển trạng thái đợt (6 trạng thái, có điều kiện tiên quyết) | ADMIN, DEPT_ADMIN | `POST /api/workspaces/{id}/transition` | ✅ |
| C4 | Gán / gỡ lớp khỏi đợt | ADMIN, DEPT_ADMIN | `/api/workspace-classes/assign`, `/unassign` | ✅ |
| C5 | Phân công giảng viên + quota, thu hồi phân công | ADMIN, DEPT_ADMIN | `/api/assignments/assign`, `/revoke` | ✅ |
| C6 | Danh sách đợt đang mở ra đề tài (cho GV) | GV, khoa | `GET /api/workspaces/open-for-topics` | ✅ |

### D. Đề tài
| Mã | Chức năng | Vai trò | Endpoint chính | UI |
|---|---|---|---|---|
| D1 | Tạo đề tài trong đợt `OPEN_TOPIC` | GV (quyền `TOPIC_MANAGE`), ADMIN | `POST /api/topics/create` | ✅ |
| D2 | Sửa / xóa đề tài (capacity ≥ số SV đã duyệt) | GV chủ đề tài, khoa, ADMIN | `PUT`, `DELETE /api/topics/{id}` | ✅ |
| D3 | Đóng / mở lại đề tài | GV, khoa, ADMIN | `POST /api/topics/{id}/open`, `/close` | ✅ |
| D4 | Danh sách, tìm kiếm, lọc, kèm số đăng ký & trạng thái đăng ký của SV | mọi | `GET /api/topics` | ✅ |

### E. Đăng ký đề tài
| Mã | Chức năng | Vai trò | Endpoint chính | UI |
|---|---|---|---|---|
| E1 | SV đăng ký (8 điều kiện kiểm tra) | SV | `POST /api/registration/register` | ✅ |
| E2 | SV hủy đăng ký đang chờ | SV | `POST /api/registration/cancel/{id}` | ✅ |
| E3 | GV duyệt (khóa dòng đề tài chống vượt capacity) | GV | `POST /api/registration/approve/{id}` | ✅ |
| E4 | GV từ chối kèm lý do | GV | `POST /api/registration/reject/{id}` | ✅ |
| E5 | Xem đăng ký của tôi / của đề tài | SV / GV, khoa | `GET /api/registration/mine`, `/topic/{id}` | ✅ |
| E6 | Đẩy realtime thay đổi đăng ký | GV, khoa | `/topic/registration/{topicId}` | ✅ |

### F. Tiến độ — [07](07-luong-tien-do-moc.md)
| Mã | Chức năng | Vai trò | Endpoint chính | UI |
|---|---|---|---|---|
| F1 | Tải file lên (13 định dạng, kiểm tra magic bytes, ≤20 MB) | mọi | `POST /api/upload` | ✅ |
| F2 | Tải file xuống có kiểm quyền | chủ file, GV đề tài, ADMIN | `GET /api/secure/files/{name}` | ✅ |
| F3 | Kiểm tra trùng lặp nội dung (cosine, ngưỡng 80 %) | hệ thống | trong F4 | ✅ (hiện điểm cho GV) |
| F4 | SV nộp báo cáo tiến độ (kèm file, gắn mốc) | SV | `POST /api/progress/create` | ✅ |
| F5 | SV kéo thẻ Kanban đổi trạng thái | SV | `PATCH /api/progress/{id}/status` | ✅ |
| F6 | GV nhận xét / đổi trạng thái / hạn | GV, ADMIN | `PUT /api/progress/{id}/comment` | ✅ |
| F7 | Xem tiến độ theo SV / theo đề tài | SV, GV, khoa | `GET /api/progress/student/{id}`, `/topic/{id}` | ✅ |
| F8 | Đẩy realtime tiến độ | thành viên đề tài | `/topic/progress/{topicId}` | ✅ |

### G. Mốc tiến độ (milestone)
| Mã | Chức năng | Vai trò | Endpoint chính | UI |
|---|---|---|---|---|
| G1 | Tạo / sửa / xóa mốc (trọng số, hạn trong khoảng đợt) | GV chủ đề tài, ADMIN | `/api/milestones` | ✅ |
| G2 | SV nộp bài theo mốc | SV | `POST /api/progress/create` + `milestoneId` | ✅ |
| G3 | Bảng điểm mốc: nộp đúng/trễ hạn, điểm gợi ý theo trọng số | thành viên đề tài | `GET /api/milestones/board` | ✅ |
| G4 | Chấm điểm từng mốc | GV chủ đề tài | `PUT /api/milestones/{id}/grades/{studentId}` | ✅ |

### H. Chấm điểm & chấm lại — [08](08-luong-cham-diem.md)
| Mã | Chức năng | Vai trò | Endpoint chính | UI |
|---|---|---|---|---|
| H1 | Tính trạng thái chấm (`OPEN/LOCKED/UNLOCKED/NOT_GRADING`) | hệ thống | trong DTO | ✅ |
| H2 | Chấm / sửa điểm tổng kết (thang 10, bước 0,25) | GV hướng dẫn | `POST /api/registration/grade/{id}` | ✅ |
| H3 | Đề tài tự chuyển `COMPLETED` khi mọi SV có điểm | hệ thống | trong H2 | ✅ |
| H4 | GV gửi yêu cầu chấm lại (đợt đã đóng) | GV | `POST /api/registration/{id}/grade-revisions` | ✅ |
| H5 | Khoa/ADMIN duyệt (mở khóa N giờ) / từ chối | DEPT_ADMIN, ADMIN | `/api/grade-revisions/{id}/approve`, `/reject` | ✅ |
| H6 | Tự khóa lại khi quá hạn mở khóa | scheduler | mỗi giờ | — |
| H7 | Lịch sử điểm | GV, khoa, ADMIN | `GET /api/registration/{id}/grade-history` | ✅ |
| H8 | Xuất Excel danh sách đăng ký + điểm | GV, khoa, ADMIN | `GET /api/export/excel` | ✅ |

### I. Giao tiếp & thông báo — [09](09-luong-giao-tiep-thong-bao.md)
| Mã | Chức năng | Vai trò | Endpoint chính | UI |
|---|---|---|---|---|
| I1 | Tin nhắn 1–1 giữa thành viên cùng đề tài (realtime) | SV, GV | `/api/messages/*`, `/user/queue/messages` | ✅ · ⚠️ hộp thư `inbox` |
| I2 | Thông báo trong app: chuông, trang thông báo, đánh dấu đã đọc (realtime) | mọi | `/api/notifications/*`, `/user/queue/notifications` | ✅ |
| I3 | Gửi thông báo thủ công | GV (cho SV của mình), ADMIN | `POST /api/notifications/create` | ⚠️ |
| I4 | Email thông báo theo cài đặt: gửi ngay hoặc xếp hàng tổng hợp | hệ thống | `NotificationMailer` | — |
| I5 | Thư tổng hợp mỗi sáng 07:30 | scheduler | — | — |
| I6 | Hạ tầng gửi email bất đồng bộ sau commit (thử lại, giãn nhịp) | hệ thống | `MailDispatcher` | — |
| I7 | Thảo luận nhóm theo chủ đề | thành viên đề tài | `/api/discuss/*` | ⚠️ |
| I8 | Thông báo chung (theo đề tài / toàn hệ thống) | GV, ADMIN | `/api/announcements` | ⚠️ |
| I9 | Lịch sự kiện đề tài (gộp cả mốc) | thành viên / GV | `/api/calendar` | ⚠️ |

### J. Dashboard & thống kê — [10](10-luong-bao-cao-van-hanh.md)
| Mã | Chức năng | Vai trò | Endpoint chính | UI |
|---|---|---|---|---|
| J1 | Dashboard GV: việc cần làm, SV im lặng 7 ngày, mốc sắp tới | GV | `GET /api/dashboard/lecturer` | ✅ |
| J2 | Dashboard SV: đăng ký hiện tại, tiến độ Kanban, mốc sắp tới | SV | `GET /api/dashboard/student` | ✅ |
| J3 | Thống kê tổng hợp theo khoa/đợt + xuất Excel | ADMIN, DEPT_ADMIN | `GET /api/dashboard/overview`, `/overview/export` | ✅ |
| J4 | Số liệu nhanh của khoa (tỉ lệ lấp đầy, GV chưa phân công) | DEPT_ADMIN | `GET /api/dashboard/department-admin` | ✅ |
| J5 | Số liệu nhanh toàn hệ thống | ADMIN | `GET /api/dashboard/admin` | ⚠️ |

### K. Kiểm toán & vận hành
| Mã | Chức năng | Vai trò | Endpoint / cơ chế | UI |
|---|---|---|---|---|
| K1 | Ghi nhật ký kiểm toán mọi thao tác ghi (hơn 60 mã hành động) | hệ thống | `AuditLogService` | — |
| K2 | Tra cứu, lọc theo nhóm, xuất CSV nhật ký | ADMIN | `/api/audit`, `/options`, `/export` | ✅ · ⚠️ `/recent` |
| K3 | Nhắc báo cáo tiến độ (09:00) và nhắc hạn mốc (08:00) | scheduler | `ReminderService` | — |
| K4 | Dọn dẹp định kỳ: refresh token, bộ đếm đăng nhập, file mồ côi, nhật ký cũ | scheduler | 03:15–03:45 | — |
| K5 | Giới hạn tần suất (rate limit) theo IP / người dùng | hệ thống | `RateLimiter` | — |
| K6 | Định dạng lỗi chung + mã lỗi nghiệp vụ | hệ thống | `GlobalExceptionHandler` | ✅ (toast, lỗi theo ô) |
| K7 | Request id, log JSON, health, Prometheus, Grafana | vận hành | Actuator, Micrometer | — |
| K8 | CI (verify, lint/test/build, dựng stack), CodeQL | vận hành | GitHub Actions | — |
