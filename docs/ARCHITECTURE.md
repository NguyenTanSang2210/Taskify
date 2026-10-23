# Kiến trúc Taskify

Tài liệu mô tả cấu trúc hệ thống ở thời điểm kết thúc Giai đoạn 3 (v0.4.0-hardening). Các quyết định nền tảng được ghi ở [`docs/adr/`](adr/).

## 1. Sơ đồ thành phần

```mermaid
flowchart TB
  subgraph Client["Trình duyệt"]
    SPA["React 19 SPA<br/>pages · components · hooks<br/>api/* (axios) · ws/stomp.js"]
  end

  subgraph FE["taskify-frontend (nginx)"]
    STATIC["File tĩnh dist/<br/>(mỗi trang một chunk lazy)"]
    PROXY["Reverse proxy /api, /ws<br/>gắn X-Request-Id"]
  end

  subgraph BE["taskify-backend (Spring Boot 3.5, Java 21)"]
    FILTERS["RequestIdFilter → rate limit →<br/>JwtAuthenticationFilter"]
    CTRL["23 controller (< 120 dòng,<br/>không truy cập repository)"]
    SVC["Service @Transactional<br/>SecurityScopeService · TopicAccessService"]
    REPO["Spring Data JPA repository<br/>EntityGraph · truy vấn gom nhóm"]
    WS["STOMP broker (/ws)<br/>/user/queue/* · /topic/*"]
    EVT["EventPublisher<br/>(gửi sau khi commit)"]
    JOBS["Scheduler: nhắc hạn, hết hạn chấm lại,<br/>dọn refresh token / file / login throttle"]
    ACT["Actuator health · info"]
  end

  DB[("MySQL 8.4<br/>Flyway V1–V8")]
  MAIL["SMTP (email bất đồng bộ)"]

  SPA -->|HTTPS| STATIC
  SPA -->|REST + cookie refresh| PROXY --> FILTERS --> CTRL --> SVC --> REPO --> DB
  SPA <-->|STOMP/SockJS| PROXY
  PROXY <--> WS
  SVC --> EVT --> WS
  SVC --> MAIL
  JOBS --> SVC
```

### Phân tầng backend

| Tầng | Gói | Quy tắc |
|---|---|---|
| Web | `controller`, `dto/request`, `dto/response` | Nhận record request đã `@Valid`, gọi service, trả DTO. Không inject repository; tối đa 120 dòng (`LayeringTest` kiểm tra tự động). |
| Nghiệp vụ | `service` | Mọi thao tác ghi nằm trong `@Transactional`; GET dùng `readOnly`. Kiểm tra phạm vi dữ liệu qua `SecurityScopeService` (khoa) và `TopicAccessService` (đề tài). |
| Dữ liệu | `repository`, `entity`, `entity/enums` | Trạng thái là enum (`@Enumerated(STRING)`); quy tắc chuyển trạng thái nằm trong enum. |
| Hạ tầng | `config`, `security`, `exception`, `scheduler` | Định dạng lỗi chung (`GlobalExceptionHandler`, `ApiException`, `ErrorCodes`), JWT + refresh cookie, CORS, log có request id. |

`spring.jpa.open-in-view=false`: dữ liệu lazy phải được nạp trong service (EntityGraph hoặc truy vấn gom nhóm). `QueryCountIT` giữ ngân sách số truy vấn của các endpoint danh sách để chặn N+1.

### Frontend

| Thư mục | Vai trò |
|---|---|
| `routes/AppRoutes.jsx` | Khai báo route; mọi trang dùng `React.lazy` + `Suspense` |
| `layouts/AppLayout.jsx`, `components/Sidebar.jsx`, `config/navigation.js` | Khung giao diện, menu theo vai trò (dữ liệu, không lặp JSX) |
| `pages/<Trang>.jsx` + `pages/<trang>/` | Trang lớn tách thành hook dữ liệu (`useXxx.js`) và component từng tab/khối |
| `api/*.js` | Mọi lời gọi HTTP (không trang nào gọi axios trực tiếp) |
| `auth/session.js`, `context/AuthContext.jsx` | Access token trong bộ nhớ, làm mới phiên qua cookie, đồng bộ đăng xuất giữa các tab |
| `ws/stomp.js` | Thư viện STOMP tải lười; lấy token mới nhất mỗi lần kết nối |
| `hooks/useLoadEffect.js` | Tải dữ liệu sau render (không setState đồng bộ trong effect) |

## 2. Mô hình dữ liệu (ERD rút gọn)

```mermaid
erDiagram
  DEPARTMENTS ||--o{ USERS : "thuộc khoa"
  ROLES ||--o{ USERS : "vai trò"
  ROLES }o--o{ PRIVILEGES : "roles_privileges"
  USERS ||--o| LECTURERS : "hồ sơ GV"
  USERS ||--o| STUDENTS : "hồ sơ SV"
  DEPARTMENTS ||--o{ ACADEMIC_CLASSES : ""
  ACADEMIC_CLASSES ||--o{ STUDENTS : "lớp"
  DEPARTMENTS ||--o{ WORKSPACES : "đợt đồ án"
  WORKSPACES ||--o{ WORKSPACE_CLASSES : "lớp tham gia"
  ACADEMIC_CLASSES ||--o{ WORKSPACE_CLASSES : ""
  WORKSPACES ||--o{ LECTURER_ASSIGNMENTS : "phân công + quota"
  LECTURERS ||--o{ LECTURER_ASSIGNMENTS : ""
  WORKSPACES ||--o{ TOPICS : ""
  LECTURERS ||--o{ TOPICS : "hướng dẫn"
  TOPICS ||--o{ TOPIC_REGISTRATION : "tối đa capacity SV được duyệt"
  STUDENTS ||--o{ TOPIC_REGISTRATION : ""
  TOPIC_REGISTRATION ||--o{ GRADE_HISTORY : "lịch sử điểm"
  TOPIC_REGISTRATION ||--o{ GRADE_REVISION_REQUESTS : "yêu cầu chấm lại"
  TOPICS ||--o{ MILESTONES : ""
  TOPICS ||--o{ PROGRESS_REPORT : ""
  STUDENTS ||--o{ PROGRESS_REPORT : ""
  MILESTONES |o--o{ PROGRESS_REPORT : "tùy chọn"
  TOPICS ||--o{ DISCUSSION_THREADS : ""
  DISCUSSION_THREADS ||--o{ DISCUSSION_POSTS : ""
  TOPICS ||--o{ MESSAGES : "ngữ cảnh"
  USERS ||--o{ MESSAGES : "gửi / nhận"
  TOPICS |o--o{ ANNOUNCEMENTS : "null = toàn hệ thống"
  TOPICS ||--o{ CALENDAR_EVENTS : ""
  USERS ||--o{ NOTIFICATIONS : ""
  USERS ||--o{ REFRESH_TOKENS : "phiên đăng nhập"
  USERS ||--o{ OTP_TOKENS : ""
```

Bảng không có khóa ngoại: `audit_logs` (ghi tên người thực hiện), `login_throttle` (chống dò mật khẩu theo tài khoản/IP), `stored_files` (file đã tải lên, xóa mồ côi định kỳ), `data_migrations`, `flyway_schema_history`.

Ghi chú:
- `topic_registration.approved`: `NULL` = chờ duyệt, `1` = đã duyệt, `0` = từ chối. Một đề tài nhận nhiều sinh viên độc lập theo `capacity`, mỗi sinh viên chấm điểm riêng ([ADR-0001](adr/0001-nhieu-sinh-vien-moi-de-tai.md)).
- Thời gian lưu **UTC** trong CSDL, ứng dụng chạy giờ Việt Nam (`Asia/Ho_Chi_Minh`) và trả về giao diện theo giờ Việt Nam.
- Schema chỉ thay đổi qua Flyway (`Backend/src/main/resources/db/migration`): V1 baseline, V2 vai trò/quyền mặc định, V3 sửa dữ liệu GĐ2, V4 enum trạng thái, V5 index truy vấn, V6 refresh token, V7 IP/trình duyệt trong nhật ký hệ thống, V8 thời điểm nhận xét tiến độ.

## 3. Vòng đời trạng thái

### Đợt đồ án (workspace) — `WorkspaceStatus.canTransitionTo`

```mermaid
stateDiagram-v2
  [*] --> DRAFT
  DRAFT --> OPEN_TOPIC: mở ra đề tài
  OPEN_TOPIC --> OPEN_REGISTRATION: mở đăng ký
  OPEN_REGISTRATION --> LOCK_REGISTRATION: khóa đăng ký
  LOCK_REGISTRATION --> IN_PROGRESS: bắt đầu thực hiện
  IN_PROGRESS --> CLOSED: kết thúc
  OPEN_TOPIC --> CLOSED: đóng sớm
  OPEN_REGISTRATION --> CLOSED: đóng sớm
  LOCK_REGISTRATION --> CLOSED: đóng sớm
  CLOSED --> [*]
```

| Thao tác | Cho phép ở trạng thái |
|---|---|
| Gán lớp, phân công giảng viên | `DRAFT`, `OPEN_TOPIC` |
| Tạo / sửa / xóa đề tài | `OPEN_TOPIC` |
| Đóng / mở lại đề tài | `OPEN_TOPIC`, `OPEN_REGISTRATION` |
| Sinh viên đăng ký | `OPEN_REGISTRATION` |
| Duyệt / từ chối đăng ký | `OPEN_REGISTRATION`, `LOCK_REGISTRATION` |
| Nộp báo cáo tiến độ, chấm điểm | `IN_PROGRESS` |
| Sửa điểm | `CLOSED` chỉ khi có yêu cầu chấm lại đã duyệt, còn hạn |

Vi phạm trả lỗi `WORKSPACE_PHASE` hoặc `INVALID_TRANSITION` ([`error-codes.md`](error-codes.md)).

### Đề tài — `TopicStatus`

```mermaid
stateDiagram-v2
  [*] --> OPEN
  OPEN --> FULL: số SV được duyệt = capacity
  FULL --> OPEN: hủy / tăng capacity
  OPEN --> CLOSED: GV đóng
  FULL --> CLOSED: GV đóng
  CLOSED --> OPEN: GV mở lại
  OPEN --> COMPLETED: mọi SV đã duyệt đều có điểm
  FULL --> COMPLETED: mọi SV đã duyệt đều có điểm
  CLOSED --> COMPLETED: mọi SV đã duyệt đều có điểm
```

`OPEN ↔ FULL` được tính lại sau mỗi lần duyệt/hủy/đổi `capacity` (`Topic.refreshCapacityStatus`); đề tài `CLOSED` hoặc `COMPLETED` giữ nguyên.

### Yêu cầu chấm lại — `GradeRevisionStatus`

```mermaid
stateDiagram-v2
  [*] --> PENDING: GV gửi yêu cầu (đợt đã CLOSED)
  PENDING --> APPROVED: khoa/admin duyệt (mở khóa tới unlockUntil)
  PENDING --> REJECTED
  APPROVED --> USED: GV sửa điểm
  APPROVED --> EXPIRED: quá unlockUntil (scheduler)
```

## 4. Luồng xác thực

```mermaid
sequenceDiagram
  participant B as Trình duyệt
  participant A as /api/auth
  B->>A: POST /login {username, password}
  alt STUDENT
    A-->>B: access token (15 phút) + Set-Cookie taskify_refresh (HttpOnly, SameSite=Strict)
  else ADMIN / DEPARTMENT_ADMIN / LECTURER
    A-->>B: preAuthToken (chưa phải access token) — OTP gửi qua email
    B->>A: POST /verify-otp {preAuthToken, otp}
    A-->>B: access token + cookie refresh
  end
  Note over B: access token chỉ nằm trong bộ nhớ (không localStorage)
  B->>A: POST /refresh (cookie) — khi tải lại trang, trước khi hết hạn, hoặc khi gặp 401
  A-->>B: access token mới + cookie mới (xoay vòng)
  B->>A: POST /logout — thu hồi phiên, xóa cookie
```

Chi tiết cơ chế bảo vệ: [`SECURITY.md`](SECURITY.md).

## 5. Realtime

| Kênh | Nội dung | Ai nhận |
|---|---|---|
| `/user/queue/notifications` | Thông báo cá nhân | chính người dùng |
| `/user/queue/messages` | Tin nhắn mới | người gửi và người nhận |
| `/topic/progress/{topicId}` | Báo cáo tiến độ thay đổi | GV hướng dẫn, SV của đề tài, khoa |
| `/topic/registration/{topicId}` | Đăng ký thay đổi | GV hướng dẫn, khoa |

Sự kiện được phát **sau khi transaction commit** (`EventPublisher` + `TransactionSynchronization`), nên client không nhận sự kiện của thao tác bị rollback. Quyền đăng ký kênh được kiểm tra khi `SUBSCRIBE` (cùng quy tắc với REST). Broker hiện là broker trong bộ nhớ (một instance backend); mở rộng nhiều instance: P4-10.

## 6. Quan sát hệ thống

- Mỗi request có `X-Request-Id` (nhận từ nginx hoặc tự sinh), có trong log, header phản hồi và body lỗi (`requestId`).
- Profile `prod`: log JSON (logstash) ra console + file xoay vòng trong volume `backend_logs`.
- `GET /actuator/health/readiness` (chỉ phụ thuộc DB) dùng cho healthcheck Docker; `GET /actuator/info` trả phiên bản build.
