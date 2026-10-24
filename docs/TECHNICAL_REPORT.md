# BÁO CÁO KỸ THUẬT — Taskify

Phiên bản tài liệu: kết thúc Giai đoạn 3 (v0.4.0-hardening), 23/10/2026.

## 1. Tổng quan hệ thống

Taskify quản lý đề tài và tiến độ đồ án trong môi trường đại học.

Mục tiêu:
- Chuẩn hóa vòng đời đề tài theo **đợt đồ án** (workspace) của từng khoa.
- Minh bạch việc đăng ký – duyệt – chấm điểm (điểm khóa khi đợt kết thúc, có lịch sử và quy trình chấm lại).
- Hỗ trợ trao đổi giữa giảng viên và sinh viên theo thời gian thực.

## 2. Phạm vi và đối tượng sử dụng

| Vai trò | Phạm vi |
|---|---|
| `ADMIN` | Toàn hệ thống: tài khoản, vai trò & quyền, khoa, thông báo chung, nhật ký kiểm toán, duyệt chấm lại |
| `DEPARTMENT_ADMIN` | Khoa của mình: đợt đồ án, lớp, phân công giảng viên + quota, duyệt đề tài, báo cáo số liệu, duyệt chấm lại |
| `LECTURER` | Đề tài mình hướng dẫn: ra đề tài, duyệt đăng ký, milestone, nhận xét tiến độ, chấm điểm |
| `STUDENT` | Bản thân: đăng ký đề tài trong đợt của lớp mình, nộp tiến độ, xem kết quả |

Quy trình nghiệp vụ chi tiết: [`documentation_vận_hành.md`](../documentation_vận_hành.md).

## 3. Kiến trúc kỹ thuật

Sơ đồ thành phần, ERD và sơ đồ trạng thái: [`ARCHITECTURE.md`](ARCHITECTURE.md).

### 3.1. Phân tầng

1. **Giao diện** — React SPA: trang tải lười theo route, hook dữ liệu + component theo tab.
2. **Truy cập API** — `src/api/*` (axios): access token trong bộ nhớ, tự làm mới phiên khi gặp 401, lỗi chuẩn hóa (`code`, `message`, `fieldErrors`).
3. **Web** — Spring MVC controller: nhận record request đã validate, trả DTO.
4. **Nghiệp vụ** — service `@Transactional`; kiểm tra phạm vi dữ liệu (`SecurityScopeService`, `TopicAccessService`).
5. **Dữ liệu** — Spring Data JPA + MySQL; schema do Flyway quản lý.
6. **Realtime** — STOMP/SockJS qua `/ws`; sự kiện phát sau khi transaction commit.

### 3.2. Công nghệ

| Lớp | Công nghệ |
|---|---|
| Backend | Java 21, Spring Boot 3.5 (Web, Security, Data JPA, Validation, WebSocket, Mail, Actuator), jjwt 0.12, Flyway, Apache POI (Excel), springdoc-openapi |
| CSDL | MySQL 8.4 |
| Frontend | React 19, React Router 7, Vite (rolldown), Tailwind CSS, axios, @stomp/stompjs + sockjs-client |
| Kiểm thử | JUnit 5, Mockito, Spring MockMvc, Testcontainers (MySQL), JaCoCo; Vitest + Testing Library + jsdom; Playwright (kịch bản giao diện) |
| Hạ tầng | Docker Compose (MySQL, backend, nginx), GitHub Actions, CodeQL, Dependabot |

## 4. Phân hệ nghiệp vụ chính

### 4.1. Vòng đời đợt đồ án

`DRAFT → OPEN_TOPIC → OPEN_REGISTRATION → LOCK_REGISTRATION → IN_PROGRESS → CLOSED` (đóng sớm được từ mọi giai đoạn sau `DRAFT`). Mỗi giai đoạn quyết định thao tác nào được phép (cấu hình, ra đề tài, đăng ký, duyệt, nộp tiến độ, chấm điểm); quy tắc nằm trong enum `WorkspaceStatus`, vi phạm trả mã `WORKSPACE_PHASE` / `INVALID_TRANSITION`.

### 4.2. Đề tài và đăng ký

- Một đề tài nhận nhiều sinh viên độc lập tới `capacity`; mỗi sinh viên được duyệt và chấm điểm riêng ([ADR-0001](adr/0001-nhieu-sinh-vien-moi-de-tai.md)).
- Sinh viên chỉ có một đăng ký đang chờ/đã duyệt trong mỗi đợt; khóa hàng khi duyệt để không vượt `capacity` dưới tải đồng thời.
- Phát hiện tên đề tài trùng/gần trùng có chuẩn hóa dấu tiếng Việt.

### 4.3. Tiến độ, điểm và chấm lại

- Milestone theo đề tài (hạn nằm trong thời gian đợt); báo cáo tiến độ có file đính kèm, Kanban `TODO / IN_PROGRESS / DONE`, nhận xét của giảng viên.
- Điểm 0–10 chấm khi đợt `IN_PROGRESS`; khóa khi `CLOSED`; sửa qua yêu cầu chấm lại được khoa duyệt, mở khóa một lần trong thời hạn ([ADR-0008](adr/0008-khoa-diem-va-cham-lai.md)). Mọi thay đổi ghi `grade_history`.

### 4.4. Cộng tác realtime và thông báo

- Thông báo cá nhân, tin nhắn, cập nhật đăng ký/tiến độ theo đề tài qua WebSocket; quyền đăng ký kênh kiểm tra như REST.
- Email bất đồng bộ (template tiếng Việt) cho OTP, kích hoạt, kết quả duyệt, chấm điểm, nhắc hạn.
- Scheduler: nhắc mốc đến hạn (08:00), nhắc chưa nộp tiến độ 7 ngày (09:00), hết hạn yêu cầu chấm lại, dọn refresh token / file mồ côi / bản ghi chống dò mật khẩu.

## 5. Mô hình dữ liệu

32 bảng nghiệp vụ (không tính `flyway_schema_history`), ERD ở [`ARCHITECTURE.md`](ARCHITECTURE.md#2-mô-hình-dữ-liệu-erd-rút-gọn). Nhóm chính:

- Tài khoản: `users`, `roles`, `privileges`, `roles_privileges`, `departments`, `lecturers`, `students`, `academic_classes`, `otp_tokens`, `refresh_tokens`, `login_throttle`.
- Đợt và đề tài: `workspaces`, `workspace_classes`, `lecturer_assignments`, `topics`, `topic_registration`, `grade_history`, `grade_revision_requests`.
- Tiến độ và cộng tác: `milestones`, `milestone_grades`, `progress_report`, `stored_files`, `discussion_threads`, `discussion_posts`, `messages`, `announcements`, `calendar_events`, `notifications`, `notification_settings`, `email_digest_items`.
- Vận hành: `audit_logs`, `data_migrations` (đánh dấu bước sửa dữ liệu đã chạy).

Thời gian lưu UTC, hiển thị giờ Việt Nam. Migration: V1 baseline → V6 refresh token.

## 6. API

Tài liệu tương tác: Swagger UI ở `/swagger-ui.html` (chỉ mở ở profile dev). Nhóm endpoint:

| Tiền tố | Chức năng |
|---|---|
| `/api/auth` | đăng nhập, OTP, refresh, đăng xuất (một / mọi thiết bị), kích hoạt, quên/đặt lại mật khẩu |
| `/api/users`, `/api/roles` | người dùng (phân trang, tìm kiếm), vai trò & quyền |
| `/api/departments`, `/api/classes` | khoa, lớp |
| `/api/workspaces`, `/api/workspace-classes`, `/api/assignments` | đợt đồ án + chuyển giai đoạn, gán lớp, phân công giảng viên |
| `/api/topics`, `/api/registration` | đề tài, đăng ký – duyệt – chấm điểm |
| `/api/registration/{id}/grade-revisions`, `/api/grade-revisions` | yêu cầu chấm lại, lịch sử điểm |
| `/api/progress`, `/api/milestones`, `/api/calendar` | tiến độ, mốc, lịch |
| `/api/discuss`, `/api/messages`, `/api/announcements`, `/api/notifications` | thảo luận, tin nhắn, thông báo |
| `/api/upload`, `/api/secure/files` | tải lên / tải xuống có kiểm tra quyền |
| `/api/export`, `/api/dashboard`, `/api/audit` | xuất Excel, số liệu tổng quan, nhật ký kiểm toán |

Quy ước chung: body là record có Bean Validation; danh sách trả `{items, page, size, totalElements, totalPages}` (size ≤ 100); lỗi theo định dạng thống nhất ở [`error-codes.md`](error-codes.md).

## 7. Bảo mật và phân quyền

Tóm tắt (chi tiết và ma trận phân quyền: [`SECURITY.md`](SECURITY.md)):

- OTP 2 bước cho vai trò quản trị/giảng viên; access token chỉ cấp sau OTP.
- Access token 15 phút trong bộ nhớ + refresh token xoay vòng trong cookie HttpOnly, phát hiện dùng lại; `tokenVersion` thu hồi ngay khi khóa tài khoản, đổi vai trò, đổi mật khẩu.
- Phân quyền hai lớp: vai trò/quyền + phạm vi dữ liệu (khoa, đề tài) — chặn IDOR ở mọi truy cập theo id và ở WebSocket.
- Chống dò mật khẩu bền vững, giới hạn tần suất các endpoint nhạy cảm.
- Upload kiểm tra đuôi + magic bytes, lưu tên UUID, tải qua API có kiểm tra quyền.
- Header bảo mật/CSP ở nginx; secret chỉ từ biến môi trường, profile `prod` từ chối secret mẫu.

## 8. Kiểm thử và chất lượng

| Chỉ số | Giá trị (kết thúc GĐ3) |
|---|---|
| Test backend | 325 (unit + tích hợp trên MySQL thật qua Testcontainers) |
| Độ phủ (JaCoCo) | toàn bộ 87,6%, service 87,0% (theo dòng) — CI chặn dưới 75% / 80% |
| Test frontend | 28 (Vitest) |
| Lint frontend | 0 lỗi, 0 cảnh báo (CI chặn cảnh báo mới) |
| Smoke test nghiệp vụ | 29 kịch bản, 0 lỗi (`scripts/smoke-test.mjs`) |

Các nhóm test đáng chú ý:
- Phân quyền và IDOR theo ma trận (`*AccessIT`, `SecurityEndpointsIT`), luồng OTP, kích hoạt, refresh token (`RefreshTokenIT`).
- Vòng đời đợt/đề tài/điểm/chấm lại (test nghiệp vụ toàn luồng của P2-12).
- Kiến trúc: `LayeringTest` (controller không dùng repository, < 120 dòng); `QueryCountIT` (ngân sách số truy vấn — chặn N+1); `FlywayMigrationIT` (DB trống và DB cũ).
- Định dạng lỗi (`ErrorFormatIT`), phân trang (`PaginationIT`), quan sát (`ObservabilityIT`), múi giờ (`TimeZoneIT`).

CI (GitHub Actions) chạy trên mỗi push/PR vào `develop` và `main`: backend `verify`, frontend lint/test/build, dựng cả stack Docker và gọi thử health/đăng nhập/trang chủ.

## 9. Yêu cầu phi chức năng

| Khía cạnh | Hiện trạng |
|---|---|
| Hiệu năng | Phân trang mọi danh sách; truy vấn gom nhóm + EntityGraph + index V5 (danh sách đề tài 55 → 7 truy vấn); bundle giao diện ban đầu 110 KB gzip |
| Khả dụng | Healthcheck Docker trên readiness (chỉ phụ thuộc DB); khởi động theo thứ tự db → backend → frontend |
| Quan sát | `X-Request-Id` xuyên suốt nginx → log → body lỗi; log JSON ở prod; `/actuator/health`, `/actuator/info` |
| Bảo trì | Migration có phiên bản; tài liệu kiến trúc, ADR, quy trình đóng góp |
| Mở rộng | Chạy nhiều instance sau nginx (P4-10): realtime qua RabbitMQ, job định kỳ khóa bằng ShedLock, giới hạn tần suất dùng chung qua CSDL — đã thử 2 instance, kể cả khi RabbitMQ khởi động lại |

## 10. Giới hạn và hướng phát triển

Giới hạn hiện tại:
- Chưa có CD và quy trình triển khai production tự động (HTTPS, sao lưu định kỳ) — chuẩn bị ở [`DEPLOYMENT.md`](DEPLOYMENT.md), hoàn thiện ở P4-09.
- Chưa có metrics/dashboard giám sát (P4-08); chưa có import người dùng từ Excel (P4-03).
- Một số quan hệ JPA còn `EAGER` (đã kiểm soát số truy vấn bằng EntityGraph + `QueryCountIT`).
- Kịch bản Playwright chạy tay trên stack Docker, chưa nằm trong CI.

Hướng phát triển (chi tiết: [`Plan/plan1.md`](../Plan/plan1.md), Giai đoạn 4): dashboard thống kê nâng cao, giao diện nhật ký kiểm toán, import Excel, nộp bài theo milestone, cài đặt thông báo cá nhân, observability, triển khai production. Để dành sau v1.0: quản lý nhóm (Team), học phí.

## 11. Gợi ý sử dụng trong báo cáo đồ án

- Chương phân tích: mục 1, 2, 4.
- Chương thiết kế: mục 3, 5, 6 và [`ARCHITECTURE.md`](ARCHITECTURE.md), [`adr/`](adr/).
- Chương cài đặt và kiểm thử: mục 7, 8 và [`SECURITY.md`](SECURITY.md).
- Chương kết luận: mục 9, 10.
