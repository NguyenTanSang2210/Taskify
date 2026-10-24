# 10 — Luồng J · K: Dashboard, thống kê, kiểm toán, vận hành

[← Mục lục](README.md)

Thành phần chính: `DashboardPage` (`LecturerSection`, `StudentSection`, `OverviewSection`, `charts`) · `StatsTab` · `AuditLogPage` (`AuditFilters`, `AuditTable`) · `DashboardService` · `DashboardOverviewService` · `DashboardQueryRepository` · `AuditLogService` · `AuditLogQueryService` · `ReminderService` · `RateLimiter` · `RequestIdFilter` · `AppMetrics`.

---

# J. Dashboard & thống kê

## Trang Tổng quan chọn nội dung theo vai trò

```mermaid
flowchart LR
  D["DashboardPage"] --> R{"vai trò"}
  R -->|"LECTURER"| L["GET /api/dashboard/lecturer → LecturerSection"]
  R -->|"STUDENT"| S["GET /api/dashboard/student → StudentSection"]
  R -->|"ADMIN / DEPARTMENT_ADMIN"| O["GET /api/dashboard/overview → OverviewSection (+ xuất Excel)"]
  DA["DepartmentAdminPage → tab Báo cáo số liệu"] --> DS["GET /api/dashboard/department-admin → StatsTab"]
  AD["(chưa dùng) GET /api/dashboard/admin"] -.-> X["tổng user, đề tài, SV, đề tài OPEN"]
```

## J1. Dashboard giảng viên

```mermaid
flowchart TB
  Q["DashboardService.getLecturerStats (userId = mình)"] --> C1["số đề tài đang hướng dẫn"]
  Q --> C2["số SV đã duyệt"]
  Q --> C3["số đăng ký chờ duyệt"]
  Q --> C4["số báo cáo chưa nhận xét"]
  Q --> L1["≤ 10 SV 'im lặng' — 7 ngày chưa gửi báo cáo<br/>(kèm lần nộp gần nhất theo SV × đề tài)"]
  Q --> L2["≤ 10 mốc sắp đến hạn trong 14 ngày"]
  C1 & C2 & C3 & C4 & L1 & L2 --> UI["LecturerSection: thẻ số + danh sách việc cần làm"]
```

## J2. Dashboard sinh viên

```mermaid
flowchart TB
  Q["getStudentStats (studentId = userId)"] --> P["chọn đăng ký hiện tại:<br/>ĐÃ DUYỆT → CHỜ DUYỆT → mới nhất (bị từ chối)"]
  P --> N{"có?"}
  N -->|"không"| E["trống: gợi ý đi đăng ký"]
  N -->|"có"| I["đề tài, GV, đợt, trạng thái, điểm"]
  I --> A{"APPROVED?"}
  A -->|"không"| E2["không có tiến độ / mốc"]
  A -->|"có"| K["đếm báo cáo TODO / IN_PROGRESS / DONE → % hoàn thành"]
  K --> M["≤ 5 mốc sắp tới của đề tài"]
  M --> UI["StudentSection"]
```

## J3. Thống kê tổng hợp (ADMIN / quản trị khoa)

```mermaid
sequenceDiagram
  autonumber
  actor Q as ADMIN / DEPT_ADMIN
  participant FE as OverviewSection
  participant S as DashboardOverviewService
  participant R as DashboardQueryRepository
  Q->>FE: chọn khoa (chỉ ADMIN), đợt
  FE->>S: GET /api/dashboard/overview?departmentId&workspaceId
  S->>S: ADMIN: khoa tùy chọn (trống = toàn hệ thống) · DEPT_ADMIN: luôn khoa mình (khác → 403)
  S->>S: đợt phải thuộc khoa đang xem (403)
  S->>R: đề tài theo trạng thái (GROUP BY)
  S->>R: đăng ký chờ / duyệt / từ chối
  S->>R: histogram điểm 10 khoảng [0,1) … [9,10] + điểm trung bình
  S->>R: tổng SV (theo đợt / khoa / toàn hệ thống) và SV đã có đề tài
  S->>R: tải GV: số đề tài, số SV (top N)
  S->>R: thời gian phản hồi = commentedAt − createdAt (giờ, trung bình) + số báo cáo chưa nhận xét
  S-->>FE: Overview {topics, students{total, withTopic, %}, registrations{..., graded, % đã chấm},<br/>scores{avg, histogram}, lecturerLoad, feedback, danh sách khoa, đợt}
  FE->>FE: charts.jsx vẽ biểu đồ
  Q->>FE: Xuất Excel
  FE->>S: GET /api/dashboard/overview/export (cùng tham số)
  S-->>FE: .xlsx 3 sheet: Tổng quan · Phân bố điểm · Giảng viên
```
Mọi số liệu là truy vấn `COUNT` / `GROUP BY` (dựa vào index V5), không cache.

## J4. Số liệu nhanh của khoa

```mermaid
flowchart LR
  T["StatsTab"] -->|"GET /api/dashboard/department-admin"| S["getDepartmentAdminStats"]
  S --> V["số đợt · số user · số SV · số đề tài · đề tài OPEN<br/>tổng capacity · tổng SV đã duyệt<br/>tỉ lệ lấp đầy = duyệt / capacity (%)<br/>số GV chưa được phân công đợt nào"]
  S -. "tài khoản chưa thuộc khoa" .-> Z["mọi số = 0, fillRate = null (không lỗi 500)"]
```

---

# K. Kiểm toán & vận hành

## K1. Ghi nhật ký kiểm toán

```mermaid
flowchart LR
  BIZ["Mọi service ghi dữ liệu"] -->|"log(action, entity, entityId, details)"| A["AuditLogService"]
  AUTH["Luồng đăng nhập / OTP / kích hoạt<br/>(chưa có SecurityContext)"] -->|"log(..., actor)"| A
  A --> ROW[("audit_logs<br/>timestamp · actorUsername (hoặc SYSTEM) · action · entityName · entityId · details<br/>· IP · trình duyệt (V7)")]
```

Các nhóm mã hành động:

| Nhóm (bộ lọc) | Ví dụ mã |
|---|---|
| `AUTH` | `LOGIN_SUCCESS`, `LOGIN_FAILED`, `LOGIN_LOCKED`, `OTP_FAILED`, `OTP_LOCKED`, `LOGOUT_ALL`, `PASSWORD_INITIAL_SET`, `PASSWORD_RESET_REQUESTED/DONE/FAILED`, `ACTIVATE_ACCOUNT`, `ACTIVATION_FAILED`, `REFRESH_TOKEN_REUSE`, `CHANGE_PASSWORD` |
| `ACCOUNT` | `CREATE_USER`, `UPDATE_USER_ROLE`, `LOCK_USER`, `UNLOCK_USER`, `UPDATE_PROFILE`, `RESEND_ACTIVATION`, `IMPORT_USERS`, `BOOTSTRAP_ADMIN`, `CREATE_ROLE`, `UPDATE_ROLE`, `DELETE_ROLE`, `CREATE_PRIVILEGE` |
| `GRADE` (giữ vĩnh viễn) | `GRADE_TOPIC`, `GRADE_CHANGED`, `GRADE_MILESTONE`, `GRADE_REVISION_REQUESTED/APPROVED/REJECTED/EXPIRED` |
| khác | `CREATE/UPDATE/TRANSITION_WORKSPACE`, `ASSIGN/UNASSIGN_CLASS_*`, `ASSIGN_LECTURER`, `REVOKE_LECTURER_ASSIGNMENT`, `CREATE/UPDATE/DELETE_TOPIC`, `REGISTER_TOPIC`, `APPROVE/REJECT/CANCEL_REGISTRATION`, `CREATE_PROGRESS`, `UPDATE_PROGRESS(_STATUS)`, `SIMILARITY_ALERT`, `CREATE/UPDATE/DELETE_MILESTONE`, `SEND_MESSAGE`, `CREATE_THREAD`, `CREATE_POST`, `CREATE_ANNOUNCEMENT`, `CREATE/DELETE_CAL_EVENT`, `CREATE_NOTIFICATION`, `CREATE/UPDATE_DEPARTMENT`, `ENABLE/DISABLE_DEPARTMENT`, `CREATE/UPDATE_CLASS`, `SYSTEM_REMINDER` |

## K2. Tra cứu & xuất nhật ký (ADMIN)

```mermaid
sequenceDiagram
  autonumber
  actor AD as ADMIN
  participant FE as AuditLogPage
  participant S as AuditLogQueryService
  FE->>S: GET /api/audit/options → danh sách action, entity đã có
  AD->>FE: lọc: người thực hiện, hành động, nhóm (AUTH / ACCOUNT / GRADE), đối tượng, id, từ – đến
  FE->>S: GET /api/audit?... &page&size (mới nhất trước, "đến" không bao gồm)
  S-->>FE: PageResponse → AuditTable (nhãn tiếng Việt: constants/auditActions.js)
  AD->>FE: Xuất CSV
  FE->>S: GET /api/audit/export?... (cùng bộ lọc)
  S-->>FE: CSV UTF-8 có BOM (Excel hiện đúng tiếng Việt), tối đa 10.000 dòng
```
`GET /api/audit/recent` có API, chưa có UI. Chính sách lưu trữ: job 03:45 xóa bản ghi cũ hơn `APP_AUDIT_RETENTION_MONTHS` (mặc định 12) **trừ** `GRADE_*`.

## K3. Nhắc hạn tự động

```mermaid
sequenceDiagram
  autonumber
  participant J as ReminderScheduler (giờ Việt Nam)
  participant R as ReminderService
  participant DB as MySQL
  participant N as NotificationService
  participant M as NotificationMailer (REMINDER)
  Note over J: 08:00 — nhắc hạn mốc
  J->>R: remindMilestoneDeadlines(now)
  R->>DB: mốc hết hạn vào ngày (hôm nay + 2) trong đợt đang hoạt động → WARNING
  R->>DB: mốc hết hạn hôm nay → CRITICAL
  loop mỗi mốc × mỗi SV đã duyệt của đề tài
    R->>DB: đã nhắc mức này cho mốc này chưa? (trong 3 ngày gần đây)
    alt chưa
      R->>N: REMINDER "Mốc sắp hết hạn" / "hết hạn hôm nay" (dueAt = hạn mốc, link /progress)
      R->>M: email milestoneReminder
    end
  end
  R->>R: audit SYSTEM_REMINDER "Nhắc hạn mốc: N lượt"
  Note over J: 09:00 — nhắc báo cáo tiến độ
  J->>R: remindMissingProgress(now)
  R->>DB: đăng ký đã duyệt, đợt IN_PROGRESS, 7 ngày không có báo cáo
  loop mỗi đăng ký
    R->>DB: đã nhắc WARNING cho đăng ký này trong 7 ngày?
    alt chưa
      R->>N: REMINDER "Nhắc gửi báo cáo tiến độ" (dueAt = ngày mai)
      R->>M: email progressReminder
    end
  end
  R->>R: audit SYSTEM_REMINDER "Nhắc báo cáo: N sinh viên"
```

## K4. Dọn dẹp định kỳ

```mermaid
flowchart LR
  subgraph NIGHT["03:15 → 03:45 hằng đêm"]
    J1["03:15 LoginThrottleService.cleanUp<br/>bộ đếm quá cửa sổ & hết khóa"]
    J2["03:30 RefreshTokenService.cleanup<br/>token hết hạn / thu hồi > 30 ngày"]
    J3["03:30 StoredFileMaintenance.purgeOrphans<br/>file chưa gắn báo cáo > 24 giờ (xóa đĩa + DB)"]
    J4["03:45 AuditLogQueryService.purgeExpired<br/>nhật ký > 12 tháng, giữ GRADE_*"]
  end
  RAM["mỗi 60 giây RateLimiter.evictExpired<br/>cửa sổ hết hạn trong RAM"]
  BOOT["khi khởi động StoredFileMaintenance.backfill<br/>tạo hồ sơ stored_files cho báo cáo cũ có fileUrl"]
```

## K5. Giới hạn tần suất

| Nhóm (`bucket`) | Khóa | Giới hạn mặc định | Biến môi trường |
|---|---|---|---|
| `login` | IP | 10 / phút | `APP_RATE_LOGIN_PER_MINUTE` |
| `otp-verify`, `pwd-initial` | IP | 10 / phút | `APP_RATE_OTP_VERIFY_PER_MINUTE` |
| `otp-resend` | IP | 5 / 10 phút | `APP_RATE_OTP_RESEND_PER_10_MINUTES` |
| `activate` | IP | 10 / 10 phút | `APP_RATE_ACTIVATE_PER_10_MINUTES` |
| `pwd-reset-ip` / `pwd-reset-id` | IP / định danh | 10 / 15 phút · 3 / 15 phút | (hằng số) |
| `refresh` | IP | 300 / phút | `APP_RATE_REFRESH_PER_MINUTE` |
| `upload` | user id | 30 / 10 phút | `APP_RATE_UPLOAD_PER_10_MINUTES` |
| Khóa đăng nhập (DB) | SHA-256(username\|IP) | 5 lần sai / 15 phút → khóa 10 phút | `APP_LOGIN_MAX_FAILURES`, `_WINDOW_MINUTES`, `_LOCK_MINUTES` |
| Gửi lại OTP / kích hoạt | (user, mục đích) | 60 giây giữa 2 mã | (hằng số) |

```mermaid
flowchart LR
  R["RateLimiter.check(bucket, key, max, window)"] --> W["ConcurrentHashMap: cửa sổ cố định (endMillis, count)"]
  W --> C{"count > max?"}
  C -->|"có"| X["429 TOO_MANY_REQUESTS + Retry-After (giây)"]
  C -->|"không"| OK["cho qua"]
  NOTE["Lưu RAM: mất khi restart (chấp nhận được)<br/>Nhiều instance → cần Redis (P4-10)"] -.-> W
```

## K6. Lỗi chuẩn hiển thị ở giao diện

```mermaid
flowchart LR
  BE["Backend: {status, code, message, errors[], requestId}"] --> AX["api.js: Error{status, code, fieldErrors, data}"]
  AX --> F1["Form: hiện fieldErrors cạnh ô nhập"]
  AX --> F2["Trang: rẽ nhánh theo code<br/>(TOPIC_FULL, WORKSPACE_PHASE, GRADE_LOCKED, ...)"]
  AX --> F3["InlineNotice / showToast(message)"]
  AX --> F4["403 → toast cảnh báo · 5xx → toast lỗi chung · 401 → làm mới phiên"]
```

## K7. Quan sát & cảnh báo

```mermaid
flowchart TB
  REQ["Request"] --> RID["X-Request-Id (nginx $request_id hoặc tự sinh)"]
  RID --> LOG["log [requestId] · prod: JSON logstash, file xoay vòng"]
  RID --> ERR["body lỗi.requestId → người dùng báo lỗi kèm mã"]
  RID --> MAILT["mailExecutor mang MDC → log gửi mail nối với request"]
  APP["Backend"] --> HC["/actuator/health/readiness (Docker healthcheck)"]
  APP --> MET["/actuator/prometheus"]
  MET --> P["Prometheus (30 ngày)"]
  P --> AL["alerts.yml:<br/>TaskifyBackendDown (up == 0, 1 phút)<br/>TaskifyDatabaseDown (taskify_db_up == 0, 1 phút)<br/>TaskifyHigh5xxRate (5 phút)<br/>TaskifyMailFailures (> 5 lỗi / 15 phút)<br/>TaskifySlowRequests (10 phút)"]
  P --> G["Grafana: dashboard taskify-overview"]
```

## K8. CI/CD & kiểm thử

```mermaid
flowchart LR
  PUSH["push / PR vào main, master, develop"] --> J1
  PUSH --> J2
  PUSH --> J3
  subgraph J1["Job backend"]
    B1["JDK 21"] --> B2["mvnw verify<br/>325 test: unit + tích hợp MySQL thật (Testcontainers)<br/>JaCoCo ≥ 75 % (service ≥ 80 %)<br/>LayeringTest · QueryCountIT (chặn N+1)"] --> B3["upload jacoco-report"]
  end
  subgraph J2["Job frontend"]
    F1["Node 22"] --> F2["npm ci → lint (0 cảnh báo) → test (Vitest) → build"]
  end
  subgraph J3["Job stack"]
    S1["tạo .env"] --> S2["docker compose up --build --wait"] --> S3["smoke: health, đăng nhập, trang chủ qua nginx"] --> S4["log khi lỗi · down"]
  end
  SCH["CodeQL: push/PR + lịch hằng tuần"] --> CQ["phân tích bảo mật Java + JS"]
  LOCAL["Cục bộ: scripts/smoke-test.mjs · ui-check*.mjs (Playwright)<br/>ws-check.mjs · pre-demo-check.ps1"] -.-> J3
```
