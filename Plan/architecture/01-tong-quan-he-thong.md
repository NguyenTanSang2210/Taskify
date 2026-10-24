# 01 — Tổng quan hệ thống

[← Mục lục](README.md)

## 1.1 Sơ đồ ngữ cảnh (ai dùng hệ thống, hệ thống nói chuyện với ai)

```mermaid
flowchart LR
  ADMIN(["👤 ADMIN<br/>Quản trị hệ thống"])
  DEPT(["👤 DEPARTMENT_ADMIN<br/>Quản trị khoa"])
  GV(["👤 LECTURER<br/>Giảng viên"])
  SV(["👤 STUDENT<br/>Sinh viên"])
  OPS(["🛠 Người vận hành"])

  subgraph TASKIFY["Hệ thống Taskify"]
    APP["Ứng dụng web<br/>quản lý đề tài & tiến độ đồ án"]
  end

  SMTP[["Máy chủ SMTP<br/>(Gmail...)"]]
  PROM[["Prometheus + Grafana<br/>(tùy chọn)"]]
  GH[["GitHub Actions<br/>CI · CodeQL"]]

  ADMIN -- "tài khoản, khoa, vai trò, nhật ký,<br/>duyệt chấm lại" --> APP
  DEPT -- "đợt, lớp, phân công GV,<br/>duyệt đề tài, thống kê" --> APP
  GV -- "ra đề tài, duyệt đăng ký, mốc,<br/>nhận xét, chấm điểm, nhắn tin" --> APP
  SV -- "đăng ký đề tài, nộp tiến độ,<br/>Kanban, nhắn tin" --> APP
  APP -- "OTP, kích hoạt, thông báo,<br/>thư tổng hợp" --> SMTP
  SMTP -. email .-> ADMIN & DEPT & GV & SV
  PROM -- "GET /actuator/prometheus<br/>(X-Metrics-Token)" --> APP
  OPS --> PROM
  GH -- "build, test, dựng stack" --> APP
```

## 1.2 Sơ đồ triển khai (Docker Compose)

```mermaid
flowchart TB
  subgraph HOST["Máy chủ Docker"]
    direction TB
    subgraph FE["container taskify-frontend · nginx · cổng 5175→80"]
      STATIC["/usr/share/nginx/html<br/>React build (dist/)"]
      NGX["nginx<br/>• try_files → index.html (SPA)<br/>• /api/ → backend:8080<br/>• /ws/ → backend:8080 (Upgrade)<br/>• gắn X-Request-Id, X-Forwarded-*<br/>• CSP, X-Frame-Options DENY, nosniff"]
    end
    subgraph BE["container taskify-backend · Spring Boot 3.5 / Java 21 · cổng 8081→8080"]
      SPRING["REST API /api/**<br/>STOMP /ws (SockJS)<br/>Actuator /actuator/**"]
      UP[("volume ./Backend/uploads<br/>file người dùng tải lên")]
      LOGS[("volume backend_logs<br/>log JSON xoay vòng (prod)")]
    end
    subgraph DBC["container taskify-db · MySQL 8.4 · cổng 3307→3306"]
      MYSQL[("mysql_data<br/>schema do Flyway V1–V10 quản lý")]
    end
    subgraph MON["profile monitoring (tùy chọn)"]
      P["taskify-prometheus :9090<br/>alerts.yml"]
      G["taskify-grafana :3000<br/>dashboard taskify-overview"]
    end
  end
  BROWSER["Trình duyệt"] -->|"HTTP(S) :5175"| NGX
  NGX --> STATIC
  NGX -->|"/api, /ws"| SPRING
  SPRING -->|"JDBC HikariCP"| MYSQL
  SPRING --- UP
  SPRING --- LOGS
  SPRING -->|SMTP 587 STARTTLS| MAIL[["SMTP"]]
  P -->|scrape| SPRING
  G --> P

  DBC -. "healthcheck mysqladmin ping" .-> DBC
  BE -. "depends_on: db healthy<br/>healthcheck /actuator/health/readiness" .-> DBC
  FE -. "depends_on: backend healthy" .-> BE
```

**Thứ tự khởi động:** `db` (healthy) → `backend` chạy Flyway migrate, `AdminBootstrap`, `StoredFileMaintenance.backfill`, (dev) `DataSeeder` → readiness `UP` → `frontend`.

**Biến bắt buộc** (không có giá trị mặc định, thiếu thì compose từ chối chạy): `JWT_SECRET`, `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD`. Tạo bằng `scripts/init-env.ps1`.

## 1.3 Chế độ phát triển (không Docker)

```mermaid
flowchart LR
  B["Trình duyệt<br/>localhost:5175"] --> V["Vite dev server<br/>npm run dev"]
  V -->|"proxy /api, /ws"| S["Spring Boot local<br/>mvnw spring-boot:run · :8080<br/>profile dev (+ DataSeeder nếu APP_SEED_ENABLED)"]
  S --> D[("MySQL<br/>local hoặc docker compose up -d db")]
  S -.-> SW["Swagger UI /swagger-ui.html<br/>(chỉ khi app.swagger.public=true)"]
```

Script hỗ trợ: `scripts/run-backend.ps1`, `scripts/run-frontend.ps1`, `scripts/get-otp.ps1 <user>` (lấy OTP demo từ DB), `scripts/smoke-test.mjs`, `scripts/ui-check*.mjs` (Playwright), `scripts/pre-demo-check.ps1`.

## 1.4 Đường đi của một request REST (từ đầu tới cuối)

```mermaid
sequenceDiagram
  autonumber
  participant U as Người dùng
  participant R as React page / hook
  participant AX as api.js (axios)
  participant NG as nginx
  participant RF as RequestIdFilter
  participant SEC as Spring Security<br/>JwtAuthenticationFilter
  participant C as Controller<br/>@PreAuthorize + @Valid
  participant S as Service @Transactional<br/>SecurityScope / TopicAccess
  participant DB as MySQL
  participant EV as EventPublisher / EmailService
  participant WS as STOMP broker

  U->>R: thao tác (bấm nút)
  R->>AX: gọi hàm trong src/api/*.js
  AX->>NG: HTTP + Authorization: Bearer (token trong bộ nhớ)
  NG->>RF: proxy + X-Request-Id
  RF->>SEC: MDC requestId
  SEC->>SEC: parse JWT (scope ACCESS, tv khớp tokenVersion, tài khoản bật)
  SEC->>C: SecurityContext có AuthUser
  C->>C: kiểm tra vai trò, validate body (record request)
  C->>S: gọi service
  S->>S: kiểm tra phạm vi khoa / quyền trên đề tài / trạng thái đợt
  S->>DB: đọc / ghi (JPA, khóa dòng khi cần)
  S->>EV: đăng ký sự kiện realtime + email (chờ commit)
  S-->>C: DTO
  Note over S,DB: COMMIT transaction
  EV-->>WS: afterCommit → đẩy sự kiện
  EV-->>EV: MailDispatcher gửi email trên luồng mail-*
  C-->>AX: 200 JSON (hoặc lỗi {code, message, errors, requestId})
  AX-->>R: dữ liệu / Error chuẩn hóa (status, code, fieldErrors)
  R-->>U: cập nhật giao diện / toast
  WS-->>R: client khác đang subscribe nhận sự kiện → tải lại
```

## 1.5 Luồng lớn nhất — vòng đời trọn một đợt đồ án

Sơ đồ này nối mọi chức năng lại theo đúng thứ tự diễn ra trong thực tế. Chi tiết từng bước ở các file 04–10.

```mermaid
flowchart TB
  classDef admin fill:#fde2e2,stroke:#c0392b,color:#000
  classDef dept fill:#fff2cc,stroke:#b7950b,color:#000
  classDef gv fill:#dbeafe,stroke:#1d4ed8,color:#000
  classDef sv fill:#dcfce7,stroke:#15803d,color:#000
  classDef sys fill:#ede9fe,stroke:#6d28d9,color:#000

  subgraph P0["Chuẩn bị hệ thống"]
    A0["ADMIN đầu tiên (bootstrap / seed)"]:::sys --> A1["ADMIN tạo khoa"]:::admin
    A1 --> A2["ADMIN / khoa tạo lớp"]:::admin
    A2 --> A3["Tạo tài khoản lẻ hoặc nhập Excel<br/>SV, GV, quản trị khoa"]:::admin
    A3 --> A4["Người dùng kích hoạt bằng mã email<br/>hoặc đổi mật khẩu tạm"]:::sys
  end

  subgraph P1["DRAFT — Bản nháp"]
    B1["Khoa tạo đợt (tên, loại, học kỳ, thời gian)"]:::dept
    B1 --> B2["Gán lớp vào đợt"]:::dept
    B1 --> B3["Phân công GV + quota"]:::dept
  end

  subgraph P2["OPEN_TOPIC — Mở ra đề tài"]
    C1["GV tạo / sửa / xóa đề tài (capacity 1–10)"]:::gv
    C2["Khoa xem, đóng / mở đề tài"]:::dept
  end

  subgraph P3["OPEN_REGISTRATION — Mở đăng ký"]
    D1["SV xem danh sách đề tài, đăng ký"]:::sv
    D2["SV hủy đăng ký đang chờ"]:::sv
    D3["GV duyệt / từ chối → thông báo + email"]:::gv
    D1 --> D3
  end

  subgraph P4["LOCK_REGISTRATION — Khóa đăng ký"]
    E1["Không nhận đăng ký mới<br/>GV vẫn duyệt / từ chối phần còn lại"]:::gv
  end

  subgraph P5["IN_PROGRESS — Đang thực hiện"]
    F1["GV đặt mốc (trọng số, hạn)"]:::gv
    F2["SV nộp báo cáo / bài theo mốc, kéo Kanban"]:::sv
    F3["Hệ thống chấm độ trùng lặp, đẩy realtime"]:::sys
    F4["GV nhận xét, chấm điểm mốc"]:::gv
    F5["Nhắc tự động: 7 ngày không nộp, mốc sắp hết hạn"]:::sys
    F6["Nhắn tin GV ↔ SV trong đề tài"]:::gv
    F7["GV chấm điểm tổng kết → đề tài COMPLETED"]:::gv
    F1 --> F2 --> F3 --> F4 --> F7
    F5 -.-> F2
  end

  subgraph P6["CLOSED — Kết thúc"]
    G1["Điểm bị khóa"]:::sys
    G2["GV gửi yêu cầu chấm lại"]:::gv
    G3["Khoa / ADMIN duyệt (mở khóa ≤ 168 giờ) hoặc từ chối"]:::dept
    G4["GV sửa điểm một lần → USED<br/>quá hạn → EXPIRED"]:::gv
    G5["Thống kê khoa, xuất Excel điểm, nhật ký kiểm toán"]:::dept
    G1 --> G2 --> G3 --> G4
  end

  P0 --> P1
  P1 -->|"transition OPEN_TOPIC"| P2
  P2 -->|"transition OPEN_REGISTRATION<br/>(cần ≥1 lớp và ≥1 đề tài)"| P3
  P3 -->|"transition LOCK_REGISTRATION"| P4
  P4 -->|"transition IN_PROGRESS"| P5
  P5 -->|"transition CLOSED"| P6
  P2 -. "đóng sớm" .-> P6
  P3 -. "đóng sớm" .-> P6
  P4 -. "đóng sớm" .-> P6
```

## 1.6 Công nghệ

| Lớp | Công nghệ |
|---|---|
| Frontend | React 19, Vite, Tailwind CSS, React Router, axios, `@stomp/stompjs` + `sockjs-client` (tải lười), Vitest + Testing Library, ESLint (React Compiler, `--max-warnings 0`) |
| Backend | Java 21, Spring Boot 3.5 (Web, Security, Data JPA, Validation, WebSocket, Mail, Actuator), JJWT (HS256), Flyway, Apache POI (Excel), Micrometer Prometheus, springdoc OpenAPI, JaCoCo, Testcontainers |
| Dữ liệu | MySQL 8.4, thời gian lưu UTC, ứng dụng chạy giờ `Asia/Ho_Chi_Minh` |
| Hạ tầng | Docker Compose (db, backend, frontend, + prometheus, grafana), nginx, GitHub Actions (CI, CodeQL) |
