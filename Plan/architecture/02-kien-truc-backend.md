# 02 — Kiến trúc Backend (Spring Boot 3.5 · Java 21)

[← Mục lục](README.md)

Gói gốc: `Backend/src/main/java/com/doanltmmt/Backend`.

## 2.1 Phân tầng

```mermaid
flowchart TB
  subgraph WEB["Tầng Web — controller/, dto/request, dto/response"]
    direction LR
    C1["23 @RestController<br/>≤ 120 dòng, không inject repository<br/>(LayeringTest kiểm tra tự động)"]
    C2["Record request + Bean Validation<br/>@Valid / @Validated(OnCreate)<br/>ValidationMessages.properties"]
    C3["@PreAuthorize theo vai trò<br/>hasRole / hasAnyRole / hasAuthority('TOPIC_MANAGE')"]
  end
  subgraph BIZ["Tầng Nghiệp vụ — service/, service/importer/"]
    direction LR
    S1["Service @Transactional<br/>(GET: readOnly)"]
    S2["Kiểm quyền theo dữ liệu<br/>SecurityScopeService (khoa)<br/>TopicAccessService (đề tài)"]
    S3["Dịch vụ dùng chung<br/>AuditLog · Notification · NotificationMailer<br/>EmailService · EventPublisher · OtpService · SimilarityService"]
  end
  subgraph DATA["Tầng Dữ liệu — repository/, entity/, entity/enums/"]
    direction LR
    R1["31 Spring Data JPA repository<br/>EntityGraph, truy vấn gom nhóm,<br/>khóa dòng FOR UPDATE"]
    R2["31 entity · trạng thái là enum<br/>quy tắc chuyển nằm trong enum"]
    R3["Flyway V1–V10<br/>ddl-auto=validate, open-in-view=false"]
  end
  subgraph INFRA["Tầng Hạ tầng — config/, security/, exception/, scheduler/"]
    direction LR
    I1["SecurityConfig · JWT · refresh cookie<br/>RateLimiter · PasswordPolicy"]
    I2["WebSocketConfig + interceptor STOMP"]
    I3["GlobalExceptionHandler · ApiException · ErrorCodes"]
    I4["RequestIdFilter · AppMetrics · AsyncConfig<br/>AdminBootstrap · StoredFileMaintenance"]
    I5["GradeRevisionScheduler · ReminderScheduler"]
  end
  WEB --> BIZ --> DATA
  INFRA -. "cắt ngang" .-> WEB
  INFRA -. "cắt ngang" .-> BIZ
  DATA --> DB[("MySQL 8.4")]
```

## 2.2 Bản đồ module: Controller → Service → Repository

```mermaid
flowchart LR
  classDef ctrl fill:#dbeafe,stroke:#1d4ed8,color:#000
  classDef svc fill:#dcfce7,stroke:#15803d,color:#000
  classDef shared fill:#fef3c7,stroke:#b45309,color:#000

  subgraph AUTH["Xác thực"]
    AuthController:::ctrl --> AuthService:::svc
    AuthController --> SessionCookies:::svc
    SessionCookies --> RefreshTokenService:::svc
    AuthService --> OtpService:::shared
    AuthService --> LoginThrottleService:::svc
    AuthService --> RefreshTokenService
  end

  subgraph ORG["Tổ chức & tài khoản"]
    UserController:::ctrl --> UserService:::svc
    UserImportController:::ctrl --> UserImportService:::svc
    UserImportService --> ImportWorkbook:::svc
    RoleController:::ctrl --> RoleService:::svc
    DepartmentController:::ctrl --> DepartmentService:::svc
    AcademicClassController:::ctrl --> AcademicClassService:::svc
  end

  subgraph WSP["Đợt đồ án"]
    WorkspaceController:::ctrl --> WorkspaceService:::svc
    WorkspaceClassController:::ctrl --> WorkspaceClassService:::svc
    LecturerAssignmentController:::ctrl --> LecturerAssignmentService:::svc
  end

  subgraph TOP["Đề tài & đăng ký & điểm"]
    TopicController:::ctrl --> TopicService:::svc
    TopicRegistrationController:::ctrl --> TopicRegistrationService:::svc
    TopicRegistrationController --> GradeService:::svc
    GradeRevisionController:::ctrl --> GradeService
    TopicRegistrationService --> GradeService
    ExportController:::ctrl --> ExportService:::svc
  end

  subgraph PRG["Tiến độ & mốc & file"]
    ProgressController:::ctrl --> ProgressService:::svc
    ProgressService --> SimilarityService:::shared
    MilestoneController:::ctrl --> MilestoneService:::svc
    MilestoneController --> MilestoneGradeService:::svc
    FileUploadController:::ctrl --> FileStorageService:::svc
    FileStorageService --> FileContentValidator:::svc
    SecureFileController:::ctrl --> FileDownloadService:::svc
  end

  subgraph COM["Giao tiếp"]
    MessageController:::ctrl --> MessageService:::svc
    DiscussionController:::ctrl --> DiscussionService:::svc
    AnnouncementController:::ctrl --> AnnouncementService:::svc
    CalendarController:::ctrl --> CalendarService:::svc
    NotificationController:::ctrl --> NotificationService:::shared
    NotificationSettingsController:::ctrl --> NotificationSettingsService:::svc
  end

  subgraph RPT["Báo cáo & kiểm toán"]
    DashboardController:::ctrl --> DashboardService:::svc
    DashboardController --> DashboardOverviewService:::svc
    AuditLogController:::ctrl --> AuditLogQueryService:::svc
  end
```

Các service dùng chung (màu vàng) được gọi từ hầu hết service nghiệp vụ:

```mermaid
flowchart LR
  classDef shared fill:#fef3c7,stroke:#b45309,color:#000
  BIZ["Service nghiệp vụ<br/>(Topic, Registration, Grade, Progress,<br/>Milestone, Message, Workspace, User...)"]
  BIZ --> SSS["SecurityScopeService<br/>requireCurrentUser · hasRole · requireDepartmentAccess"]:::shared
  BIZ --> TAS["TopicAccessService<br/>isOwner · isMember · requireOwner · requireMember"]:::shared
  BIZ --> ALS["AuditLogService.log(action, entity, id, details[, actor])"]:::shared
  BIZ --> NS["NotificationService.notify / publish"]:::shared
  BIZ --> NM["NotificationMailer.send(user, EmailCategory, mail)"]:::shared
  BIZ --> EP["EventPublisher<br/>progressCreated/Updated · registrationChanged<br/>notificationCreated · messageCreated"]:::shared
  NS --> EP
  NM --> ES["EmailService.sendEmail → MailRequested event"]:::shared
  ES --> MD["MailDispatcher<br/>@Async + AFTER_COMMIT"]:::shared
  EP --> SMT["SimpMessagingTemplate<br/>(afterCommit)"]
  BIZ --> ET["EmailTemplates (HTML + plain text)"]:::shared
```

## 2.3 Chuỗi filter bảo mật HTTP

```mermaid
flowchart TB
  REQ["HTTP request"] --> RID["RequestIdFilter (HIGHEST_PRECEDENCE)<br/>nhận X-Request-Id hợp lệ [A-Za-z0-9._-]{1,64} hoặc sinh 16 ký tự<br/>→ MDC, header phản hồi, body lỗi"]
  RID --> CORS["CORS (app.cors.allowed-origins)"]
  CORS --> JWT["JwtAuthenticationFilter<br/>Authorization: Bearer → TokenAuthenticator"]
  JWT --> TA{"TokenAuthenticator.authenticate"}
  TA -->|"chữ ký HS256, hạn, iss=taskify,<br/>scope=ACCESS, user tồn tại,<br/>active & activated, tv == tokenVersion"| OK["SecurityContext = AuthUser<br/>(eraseCredentials)"]
  TA -->|"sai bất kỳ điều kiện nào"| ANON["đi tiếp KHÔNG xác thực"]
  OK --> AUTHZ
  ANON --> AUTHZ
  AUTHZ{"authorizeHttpRequests"}
  AUTHZ -->|"/error, OPTIONS /api/**,<br/>POST 9 endpoint /api/auth/* công khai,<br/>/ws/**, GET /actuator/health/**, /info"| PERMIT["permitAll"]
  AUTHZ -->|"GET /actuator/prometheus"| MT["MetricsTokenAuthorization<br/>X-Metrics-Token == METRICS_TOKEN<br/>(so sánh thời gian hằng)"]
  AUTHZ -->|"swagger (chỉ khi app.swagger.public)"| PERMIT
  AUTHZ -->|"còn lại"| NEED["authenticated()"]
  NEED -->|"chưa xác thực"| E401["401 UNAUTHORIZED<br/>SecurityErrorHandlers.unauthorized"]
  NEED --> PRE["@PreAuthorize ở controller"]
  PRE -->|"sai vai trò"| E403["403 FORBIDDEN"]
  PRE --> CTRL["Controller → Service"]
```

`csrf` tắt vì API dùng header Bearer (không dùng session/cookie). Riêng cookie refresh có chống CSRF riêng: `SameSite=Strict` + kiểm tra `Origin` (`RefreshCookie.requireSameOrigin`). `sessionCreationPolicy = STATELESS`.

## 2.4 Ba lớp phân quyền

```mermaid
flowchart LR
  R["Request đã xác thực"] --> L1
  subgraph L1["Lớp 1 — Vai trò (controller)"]
    PA["@PreAuthorize<br/>vd. hasRole('LECTURER')"]
  end
  L1 --> L2
  subgraph L2["Lớp 2 — Phạm vi khoa (SecurityScopeService)"]
    RD["requireDepartmentAccess(deptId)<br/>ADMIN: qua · DEPT_ADMIN: đúng khoa mình · khác: 403"]
    SELF["chính chủ: currentUser.id == studentId / lecturerId"]
  end
  L2 --> L3
  subgraph L3["Lớp 3 — Theo đối tượng (TopicAccessService) chống IDOR"]
    OWN["isOwner(topic,user)<br/>ADMIN luôn · DEPT_ADMIN cùng khoa của đợt · LECTURER là GV hướng dẫn"]
    MEM["isMember(topic,user)<br/>owner + STUDENT có đăng ký ĐÃ DUYỆT"]
  end
  L3 --> L4
  subgraph L4["Lớp 4 — Giai đoạn đợt (enum WorkspaceStatus)"]
    PH["allowsTopicEditing · allowsTopicOpenClose<br/>allowsConfiguration · allowsRegistrationReview<br/>== IN_PROGRESS / CLOSED"]
  end
  L4 --> OKK["Thực thi nghiệp vụ"]
```

### Ma trận vai trò (rút gọn theo nhóm endpoint)

| Nhóm | ADMIN | DEPARTMENT_ADMIN | LECTURER | STUDENT |
|---|:-:|:-:|:-:|:-:|
| Tài khoản (`/users` tạo, khóa, đổi vai trò) | ✔ | — | — | — |
| Danh sách người dùng theo khoa, nhập Excel | ✔ | ✔ (khoa mình) | — | — |
| Vai trò & quyền, khoa, nhật ký kiểm toán | ✔ | — | — | — |
| Lớp, đợt, gán lớp, phân công GV | ✔ | ✔ (khoa mình) | — | — |
| Tạo / sửa / đóng-mở đề tài | ✔ | ✔ (khoa mình) | ✔ (đề tài của mình, quyền `TOPIC_MANAGE`) | — |
| Duyệt / từ chối đăng ký, chấm điểm, gửi yêu cầu chấm lại | — | — | ✔ (đề tài của mình) | — |
| Duyệt yêu cầu chấm lại | ✔ | ✔ (khoa mình, không tự duyệt) | — | — |
| Đăng ký / hủy, nộp tiến độ, Kanban | — | — | — | ✔ (của mình) |
| Nhận xét tiến độ, mốc, lịch, thông báo theo đề tài | ✔ | (xem) | ✔ (đề tài của mình) | (xem nếu đã duyệt) |
| Tin nhắn, thảo luận | thành viên đề tài | thành viên đề tài | thành viên đề tài | thành viên đề tài |
| Dashboard | admin, overview | department-admin, overview | lecturer | student |
| Hồ sơ, thông báo, cài đặt email, tải file | ✔ | ✔ | ✔ | ✔ |

Quyền (privilege) mặc định từ migration V2: `USER_MANAGE`, `ROLE_MANAGE`, `DEPT_MANAGE`, `TOPIC_MANAGE`, `REGISTRATION_MANAGE`, `SYSTEM_REPORT`. Mã nguồn hiện chỉ kiểm tra trực tiếp `TOPIC_MANAGE` (ở `TopicController`); các quyền khác mang tính mô tả, phân quyền thực tế dựa vào vai trò.

## 2.5 Kiến trúc realtime (STOMP over SockJS)

```mermaid
flowchart LR
  subgraph CLIENT["Trình duyệt"]
    STC["ws/stomp.js<br/>connectAndSubscribe(destination)"]
  end
  subgraph SERVER["Backend"]
    EP["/ws (SockJS)<br/>setAllowedOrigins(app.cors.allowed-origins)"]
    INT["WebSocketAuthChannelInterceptor<br/>(clientInboundChannel)"]
    BRK["SimpleBroker trong bộ nhớ<br/>/topic, /queue · prefix user /user"]
    PUB["EventPublisher<br/>(gửi sau commit)"]
    MET["AppMetrics<br/>taskify_websocket_sessions"]
  end
  STC -- "CONNECT + Authorization: Bearer" --> EP --> INT
  INT -- "CONNECT: TokenAuthenticator (cùng quy tắc HTTP)" --> BRK
  INT -- "SUBSCRIBE: chỉ 4 đích hợp lệ" --> BRK
  INT -- "SEND: luôn từ chối" --> X["MessagingException"]
  PUB --> BRK
  BRK -- "/user/queue/notifications<br/>/user/queue/messages" --> STC
  BRK -- "/topic/progress/{topicId}<br/>/topic/registration/{topicId}" --> STC
  EP -. "SessionConnected / Disconnect" .-> MET
```

| Kênh | Kiểm tra khi SUBSCRIBE | Sự kiện | Nơi phát |
|---|---|---|---|
| `/user/queue/notifications` | chính người dùng (định tuyến theo Principal) | `NOTIFICATION_CREATED` | `NotificationService.publish` |
| `/user/queue/messages` | chính người dùng | `MESSAGE_CREATED` (gửi cho cả người gửi và người nhận) | `MessageService.send` |
| `/topic/progress/{id}` | `TopicAccessService.isMember` | `PROGRESS_CREATED`, `PROGRESS_UPDATED` | `ProgressService.create/updateStatus/addComment` |
| `/topic/registration/{id}` | `TopicAccessService.isOwner` | `REGISTRATION_CREATED`, `REGISTRATION_UPDATED`, `REGISTRATION_CANCELLED` | `TopicRegistrationService` |

## 2.6 Kiến trúc gửi email

```mermaid
flowchart TB
  subgraph SEC["Email bảo mật — luôn gửi ngay"]
    OTP["OtpService.issue<br/>LOGIN · ACTIVATION · RESET_PASSWORD"]
  end
  subgraph NOTI["Email thông báo — theo cài đặt người nhận"]
    BIZ["TopicRegistration · Grade · Progress · Reminder<br/>NotificationMailer.send(user, category, mail)"]
    CHK{"NotificationSettings<br/>của người nhận"}
    BIZ --> CHK
    CHK -->|"nhóm bị tắt"| SKIP["bỏ qua email<br/>(thông báo trong app vẫn có)"]
    CHK -->|"emailDigest = true"| Q[("email_digest_items<br/>hàng chờ")]
    CHK -->|"gửi ngay / chưa cài đặt"| ES
    CRON["@Scheduled 07:30 VN<br/>sendDigests()"] --> Q
    Q -->|"gộp mọi mục của 1 người<br/>→ 1 thư, xóa mục đã gửi"| ES
  end
  OTP --> ES["EmailService.sendEmail<br/>publishEvent(MailRequested)"]
  ES --> TEL{"@TransactionalEventListener<br/>AFTER_COMMIT (fallbackExecution)"}
  TEL -->|"rollback → không gửi"| DROP["huỷ"]
  TEL --> EXE["mailExecutor (core 2, max 5, queue 5000,<br/>CallerRunsPolicy, mang MDC requestId)"]
  EXE --> MD["MailDispatcher.onMailRequested<br/>• chưa cấu hình JavaMailSender → bỏ qua + cảnh báo<br/>• pace(): cách nhau ≥ 200 ms toàn ứng dụng<br/>• thử tối đa 3 lần, chờ 2s × lần"]
  MD --> SMTP[["SMTP"]]
  MD --> M["counter taskify_mail_sent_total{result}"]
```

Nhóm email (`EmailCategory`): `REGISTRATION` (kết quả đăng ký), `PROGRESS` (báo cáo & nhận xét), `GRADE` (điểm & chấm lại), `REMINDER` (nhắc hạn tự động).

## 2.7 Phát sự kiện sau commit

```mermaid
sequenceDiagram
  participant S as Service (@Transactional)
  participant EP as EventPublisher
  participant TSM as TransactionSynchronizationManager
  participant DB as MySQL
  participant BR as STOMP broker
  S->>DB: INSERT/UPDATE
  S->>EP: progressCreated(pr)
  EP->>EP: dựng payload NGAY (còn trong transaction, entity còn nạp được)
  EP->>TSM: registerSynchronization(afterCommit → convertAndSend)
  alt commit thành công
    DB-->>S: COMMIT
    TSM->>BR: convertAndSend(destination, payload)
  else ném lỗi → rollback
    DB-->>S: ROLLBACK
    Note over TSM,BR: không có sự kiện ma
  end
```

## 2.8 Job định kỳ

```mermaid
gantt
  title Lịch job @Scheduled trong một ngày (giờ Việt Nam)
  dateFormat HH:mm
  axisFormat %H:%M
  section Dọn dẹp đêm
  Xóa bộ đếm đăng nhập hết hạn (03h15) :03:15, 10m
  Xóa refresh token cũ > 30 ngày (03h30) :03:30, 10m
  Xóa file mồ côi > 24 giờ (03h30) :03:30, 10m
  Xóa nhật ký > 12 tháng, giữ GRADE_ (03h45) :03:45, 10m
  section Thông báo buổi sáng
  Thư tổng hợp email (07h30) :07:30, 10m
  Nhắc hạn mốc — còn 2 ngày / hết hạn hôm nay (08h00) :08:00, 10m
  Nhắc báo cáo — 7 ngày chưa nộp (09h00) :09:00, 10m
  section Lặp lại
  Hết hạn mở khóa điểm (phút 05 mỗi giờ) :00:05, 5m
```

| Job | Lớp | Lịch | Việc làm |
|---|---|---|---|
| Dọn cửa sổ rate limit | `RateLimiter.evictExpired` | mỗi 60 giây | xóa cửa sổ đã hết hạn trong RAM |
| Hết hạn chấm lại | `GradeRevisionScheduler` → `GradeService.expireOverdue` | `0 5 * * * *` | `APPROVED` quá `unlockUntil` → `EXPIRED`, báo GV |
| Nhắc hạn mốc | `ReminderScheduler.remindMilestoneDeadlines` | 08:00 VN | WARNING trước 2 ngày, CRITICAL đúng ngày |
| Nhắc báo cáo | `ReminderScheduler.remindWeeklyProgress` | 09:00 VN | SV 7 ngày chưa nộp, chống nhắc trùng |
| Thư tổng hợp | `NotificationMailer.sendDigests` | 07:30 VN | 1 thư / người |
| Bộ đếm đăng nhập | `LoginThrottleService.cleanUp` | 03:15 | xóa bộ đếm hết ý nghĩa |
| Refresh token | `RefreshTokenService.cleanup` | 03:30 | xóa token hết hạn/thu hồi quá 30 ngày |
| File mồ côi | `StoredFileMaintenance.purgeOrphans` | 03:30 | file chưa gắn báo cáo sau 24 giờ |
| Nhật ký | `AuditLogQueryService.purgeExpired` | 03:45 | xóa > 12 tháng, giữ nhật ký điểm |

Chạy một lần khi khởi động (`ApplicationRunner`): `AdminBootstrap` (tạo ADMIN đầu tiên), `StoredFileMaintenance.backfill` (tạo hồ sơ file cho báo cáo cũ), `DataSeeder` (chỉ profile `dev` + `APP_SEED_ENABLED=true`).

## 2.9 Xử lý lỗi

```mermaid
flowchart LR
  T1["ApiException(code, message)<br/>vd. WORKSPACE_PHASE, TOPIC_FULL,<br/>DUPLICATE_REGISTRATION, GRADE_LOCKED,<br/>INVALID_TRANSITION, IMPORT_FILE_INVALID"] --> H
  T2["ResponseStatusException"] --> H
  T3["MethodArgumentNotValid / ConstraintViolation"] --> H
  T4["TooManyRequestsException (+ Retry-After)"] --> H
  T5["AccessDenied / Authentication"] --> H
  T6["DataIntegrityViolation"] --> H
  T7["Exception bất kỳ"] --> H
  H["GlobalExceptionHandler<br/>+ ApiErrorAttributes cho /error<br/>+ SecurityErrorHandlers cho filter"] --> B["ErrorBodies.of(...)<br/>{timestamp, status, code, error,<br/>message (tiếng Việt), path, errors[], requestId}"]
  B --> FE["api.js chuẩn hóa → Error{status, code, fieldErrors, data}<br/>403: toast cảnh báo · 5xx/mất mạng: toast lỗi · 401: làm mới phiên"]
```

| Ngoại lệ | HTTP | `code` |
|---|---|---|
| Validation body | 400 | `VALIDATION_FAILED` (kèm `errors[]` theo trường) |
| JSON sai, thiếu tham số, sai kiểu | 400 | `MALFORMED_REQUEST` |
| `IllegalArgumentException`, `ClassCastException` | 400 | `INVALID_INPUT` |
| Trùng khóa / vi phạm ràng buộc | 409 | `DATA_CONFLICT` |
| Lỗi nghiệp vụ | 400 | mã riêng như bảng trên |
| Không xác định | 500 | `INTERNAL_ERROR` (không lộ stacktrace / SQL) |

## 2.10 Quan sát hệ thống

```mermaid
flowchart LR
  APP["Backend"] --> LOG["Log có [requestId]<br/>prod: JSON logstash ra console + file xoay vòng"]
  APP --> H["/actuator/health/readiness<br/>(readinessState + db) → healthcheck Docker"]
  APP --> I["/actuator/info → phiên bản build"]
  APP --> PR["/actuator/prometheus (X-Metrics-Token)<br/>JVM · HTTP histogram + SLO 100ms/300ms/1s/3s<br/>HikariCP · taskify_db_up · taskify_websocket_sessions<br/>taskify_mail_sent_total{result}"]
  PR --> PROM["Prometheus + alerts.yml"] --> GRAF["Grafana dashboard taskify-overview"]
```

## 2.11 Mô hình dữ liệu (ERD)

```mermaid
erDiagram
  DEPARTMENTS ||--o{ USERS : "thuộc khoa"
  ROLES ||--o{ USERS : "vai trò"
  ROLES }o--o{ PRIVILEGES : "roles_privileges"
  USERS ||--o| LECTURERS : "hồ sơ GV (id = user.id)"
  USERS ||--o| STUDENTS : "hồ sơ SV (id = user.id)"
  DEPARTMENTS ||--o{ ACADEMIC_CLASSES : "lớp"
  ACADEMIC_CLASSES ||--o{ STUDENTS : "SV thuộc lớp"
  DEPARTMENTS ||--o{ WORKSPACES : "đợt đồ án"
  WORKSPACES ||--o{ WORKSPACE_CLASSES : "lớp tham gia (active)"
  ACADEMIC_CLASSES ||--o{ WORKSPACE_CLASSES : ""
  WORKSPACES ||--o{ LECTURER_ASSIGNMENTS : "phân công + quota"
  LECTURERS ||--o{ LECTURER_ASSIGNMENTS : ""
  WORKSPACES ||--o{ TOPICS : "đề tài"
  LECTURERS ||--o{ TOPICS : "hướng dẫn"
  TOPICS ||--o{ TOPIC_REGISTRATION : "≤ capacity SV được duyệt"
  STUDENTS ||--o{ TOPIC_REGISTRATION : "đăng ký"
  LECTURERS |o--o{ TOPIC_REGISTRATION : "reviewer"
  TOPIC_REGISTRATION ||--o{ GRADE_HISTORY : "mỗi lần lưu điểm"
  TOPIC_REGISTRATION ||--o{ GRADE_REVISION_REQUESTS : "yêu cầu chấm lại"
  GRADE_REVISION_REQUESTS |o--o{ GRADE_HISTORY : "lần sửa dùng yêu cầu"
  TOPICS ||--o{ MILESTONES : "mốc"
  TOPICS ||--o{ PROGRESS_REPORT : "báo cáo"
  STUDENTS ||--o{ PROGRESS_REPORT : "người nộp"
  MILESTONES |o--o{ PROGRESS_REPORT : "bài nộp theo mốc"
  MILESTONES ||--o{ MILESTONE_GRADES : "điểm mốc"
  STUDENTS ||--o{ MILESTONE_GRADES : ""
  TOPICS ||--o{ DISCUSSION_THREADS : "chủ đề"
  DISCUSSION_THREADS ||--o{ DISCUSSION_POSTS : "bài viết"
  USERS ||--o{ DISCUSSION_POSTS : "tác giả"
  TOPICS ||--o{ MESSAGES : "ngữ cảnh"
  USERS ||--o{ MESSAGES : "gửi / nhận"
  TOPICS |o--o{ ANNOUNCEMENTS : "null = toàn hệ thống"
  TOPICS ||--o{ CALENDAR_EVENTS : "lịch"
  USERS ||--o{ NOTIFICATIONS : "thông báo"
  USERS ||--o| NOTIFICATION_SETTINGS : "cài đặt email"
  USERS ||--o{ EMAIL_DIGEST_ITEMS : "chờ thư tổng hợp"
  USERS ||--o{ REFRESH_TOKENS : "phiên (familyId)"
  USERS ||--o{ OTP_TOKENS : "mã OTP theo mục đích"

  USERS {
    bigint id PK
    string username UK
    string email UK
    string password "bcrypt"
    bool active "khóa/mở"
    bool activated "đã kích hoạt"
    bool mustChangePassword
    int tokenVersion "thu hồi token"
  }
  WORKSPACES {
    bigint id PK
    string name
    string type
    string semester
    enum status "WorkspaceStatus"
    datetime startAt
    datetime endAt
  }
  TOPICS {
    bigint id PK
    string title
    text description
    int capacity "1..10"
    enum status "TopicStatus"
  }
  TOPIC_REGISTRATION {
    bigint id PK
    bool approved "null=chờ, 1=duyệt, 0=từ chối"
    double score "0..10 bước 0.25"
    text feedback
    string rejectReason
    datetime registeredAt
    datetime reviewedAt
  }
  PROGRESS_REPORT {
    bigint id PK
    string title
    text content
    string fileUrl
    enum status "TODO/IN_PROGRESS/DONE"
    double similarityScore
    bigint similarToReportId
    text lecturerComment
    datetime commentedAt
    datetime deadline
  }
  MILESTONES {
    bigint id PK
    string title
    datetime startDate
    datetime deadline
    int weight "trọng số"
  }
  GRADE_REVISION_REQUESTS {
    bigint id PK
    enum status "PENDING/APPROVED/REJECTED/USED/EXPIRED"
    string reason
    string reviewNote
    datetime unlockUntil
  }
  NOTIFICATIONS {
    bigint id PK
    enum type "INFO/REMINDER/SYSTEM"
    enum severity "INFO/WARNING/CRITICAL"
    string refType
    bigint refId
    string linkPath
    datetime dueAt
    datetime readAt
  }
```

Bảng độc lập (không khóa ngoại): `audit_logs` (ghi tên người thực hiện, IP, trình duyệt), `login_throttle` (khóa = SHA-256 của username|IP), `stored_files` (chủ file, `refType/refId` khi đã gắn báo cáo), `data_migrations`, `flyway_schema_history`.

Lịch sử migration: V1 baseline · V2 vai trò/quyền mặc định · V3 sửa dữ liệu GĐ2 · V4 chuẩn hóa enum trạng thái · V5 index truy vấn · V6 refresh token · V7 IP/trình duyệt trong nhật ký · V8 thời điểm nhận xét tiến độ · V9 trọng số + điểm theo mốc · V10 cài đặt email + hàng chờ thư tổng hợp.

## 2.12 Toàn bộ máy trạng thái

### Đợt đồ án — `WorkspaceStatus`
```mermaid
stateDiagram-v2
  [*] --> DRAFT: tạo đợt
  DRAFT --> OPEN_TOPIC: mở ra đề tài
  OPEN_TOPIC --> OPEN_REGISTRATION: mở đăng ký (cần ≥1 lớp active và ≥1 đề tài)
  OPEN_REGISTRATION --> LOCK_REGISTRATION: khóa đăng ký
  LOCK_REGISTRATION --> IN_PROGRESS: bắt đầu thực hiện
  IN_PROGRESS --> CLOSED: kết thúc
  OPEN_TOPIC --> CLOSED: đóng sớm
  OPEN_REGISTRATION --> CLOSED: đóng sớm
  LOCK_REGISTRATION --> CLOSED: đóng sớm
  CLOSED --> [*]
  note right of DRAFT
    DRAFT, OPEN_TOPIC: gán lớp, phân công GV
    chỉ DRAFT: sửa các trường thông tin đợt
  end note
  note right of IN_PROGRESS
    nộp tiến độ, nhận xét,
    chấm điểm mốc, chấm điểm
  end note
```

### Đề tài — `TopicStatus`
```mermaid
stateDiagram-v2
  [*] --> OPEN: GV tạo (đợt OPEN_TOPIC)
  OPEN --> FULL: số SV duyệt = capacity
  FULL --> OPEN: từ chối SV đã duyệt / tăng capacity
  OPEN --> CLOSED: GV / khoa đóng
  FULL --> CLOSED: GV / khoa đóng
  CLOSED --> OPEN: mở lại (tự tính lại FULL)
  OPEN --> COMPLETED: mọi SV đã duyệt có điểm
  FULL --> COMPLETED: mọi SV đã duyệt có điểm
  CLOSED --> COMPLETED: mọi SV đã duyệt có điểm
```

### Đăng ký — `TopicRegistration.approved`
```mermaid
stateDiagram-v2
  [*] --> PENDING: SV đăng ký (approved = null)
  PENDING --> [*]: SV hủy (xóa bản ghi, chỉ khi OPEN_REGISTRATION)
  PENDING --> APPROVED: GV duyệt (approved = true)
  PENDING --> REJECTED: GV từ chối + lý do (approved = false)
  APPROVED --> REJECTED: GV từ chối lại (đề tài có thể còn chỗ)
  APPROVED --> GRADED: GV chấm điểm (score ≠ null)
  GRADED --> GRADED: sửa điểm (bắt buộc lý do, ghi GradeHistory)
```

### Báo cáo tiến độ — `ProgressStatus` (Kanban)
```mermaid
stateDiagram-v2
  [*] --> TODO: SV nộp
  TODO --> IN_PROGRESS
  IN_PROGRESS --> DONE
  DONE --> IN_PROGRESS
  IN_PROGRESS --> TODO
  TODO --> DONE
  DONE --> TODO
  note right of TODO
    SV chủ báo cáo kéo thẻ (PATCH /status)
    GV cũng có thể đặt trạng thái khi nhận xét
  end note
```

### Trạng thái chấm điểm — `GradeState` (tính động)
```mermaid
stateDiagram-v2
  [*] --> NOT_GRADING: đợt chưa IN_PROGRESS
  NOT_GRADING --> OPEN: đợt → IN_PROGRESS
  OPEN --> LOCKED: đợt → CLOSED
  LOCKED --> UNLOCKED: yêu cầu chấm lại APPROVED, còn hạn
  UNLOCKED --> LOCKED: GV sửa điểm (USED) / quá hạn (EXPIRED)
```

### Yêu cầu chấm lại — `GradeRevisionStatus`
```mermaid
stateDiagram-v2
  [*] --> PENDING: GV gửi (điểm LOCKED, đã có điểm, chưa có PENDING khác)
  PENDING --> APPROVED: khoa/ADMIN duyệt, unlockUntil = now + 1..168 giờ (mặc định 72)
  PENDING --> REJECTED: khoa/ADMIN từ chối
  APPROVED --> USED: GV sửa điểm một lần
  APPROVED --> EXPIRED: scheduler mỗi giờ
```

### Mã OTP — `OtpToken`
```mermaid
stateDiagram-v2
  [*] --> ACTIVE: issue() — hủy mã cũ cùng mục đích
  ACTIVE --> USED: nhập đúng (OK)
  ACTIVE --> ACTIVE: nhập sai, attempts < 5 (INVALID)
  ACTIVE --> LOCKED_USED: attempts đạt 5 (LOCKED)
  ACTIVE --> EXPIRED_USED: quá hạn (LOGIN 5 phút · RESET 10 phút · ACTIVATION 72 giờ)
  ACTIVE --> REPLACED: cấp mã mới
```

### Refresh token — `RefreshToken`
```mermaid
stateDiagram-v2
  [*] --> LIVE: issue() sau đăng nhập (familyId mới, TTL 7 ngày)
  LIVE --> ROTATED: /refresh → revokedAt + replacedById, cấp token mới cùng họ
  ROTATED --> ROTATED: dùng lại trong 30 giây (2 tab) → cấp thêm token, không báo động
  ROTATED --> FAMILY_KILLED: dùng lại sau 30 giây → thu hồi MỌI phiên + tokenVersion+1 + audit REFRESH_TOKEN_REUSE
  LIVE --> REVOKED: logout · logout-all · đổi/đặt lại mật khẩu
  LIVE --> REVOKED: hết hạn / tokenVersion lệch / tài khoản khóa
  REVOKED --> [*]: cleanup sau 30 ngày
```

### Tài khoản — `User`
```mermaid
stateDiagram-v2
  [*] --> CHUA_KICH_HOAT: Admin tạo không mật khẩu / nhập Excel / bootstrap (activated=false)
  [*] --> MAT_KHAU_TAM: Admin tạo có mật khẩu tạm (mustChangePassword=true)
  CHUA_KICH_HOAT --> HOAT_DONG: kích hoạt bằng mã email
  MAT_KHAU_TAM --> HOAT_DONG: đổi mật khẩu ở lần đăng nhập đầu
  HOAT_DONG --> BI_KHOA: Admin khóa (active=false, tokenVersion+1)
  BI_KHOA --> HOAT_DONG: Admin mở
  HOAT_DONG --> HOAT_DONG: đổi vai trò / đổi mật khẩu / logout-all (tokenVersion+1)
```
