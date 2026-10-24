# 03 — Kiến trúc Frontend (React 19 · Vite · Tailwind)

[← Mục lục](README.md)

Thư mục gốc: `frontend/src`.

## 3.1 Cấu trúc thư mục và trách nhiệm

```mermaid
flowchart TB
  MAIN["main.jsx<br/>StrictMode → AuthProvider → App + Toaster"] --> APP["App.jsx<br/>chờ ready → PublicRoutes hoặc AppLayout + AppRoutes"]
  APP --> ROUTES["routes/AppRoutes.jsx<br/>React.lazy + Suspense (mỗi trang 1 chunk)<br/>RequireRole bọc route theo vai trò"]
  APP --> LAYOUT["layouts/AppLayout.jsx<br/>Sidebar (config/navigation.js) · TopBar · NotificationsBell"]
  ROUTES --> PAGES["pages/*.jsx — 17 trang"]
  PAGES --> SUB["pages/&lt;trang&gt;/<br/>hook dữ liệu useXxx.js + component tab/khối"]
  PAGES --> COMP["components/<br/>TopicCard · RegistrationTable · ProgressRow · Pagination<br/>GradeHistoryModal · GradeRevisionPanel · FileDownloadButton<br/>PasswordChecklist · InlineNotice · userImport/*"]
  SUB --> HOOKS["hooks/useLoadEffect.js (tải sau render)<br/>hooks/useMediaQuery.js"]
  PAGES --> API["api/*.js — 20 module<br/>(không trang nào gọi axios trực tiếp)"]
  SUB --> API
  COMP --> API
  API --> AX["api.js — axios instance<br/>baseURL /api · withCredentials · interceptor"]
  AX --> SES["auth/session.js<br/>access token trong bộ nhớ · refreshSession · revokeSession"]
  PAGES --> WS["ws/stomp.js<br/>connectAndSubscribe (thư viện tải lười)"]
  WS --> SES
  CTX["context/AuthContext.jsx<br/>user · token · ready · login · logout · updateUser · updateToken"] --> SES
  APP --> CTX
  CONST["constants/status.js · constants/auditActions.js<br/>toast.js · Toaster.jsx"] -.-> PAGES
```

## 3.2 Khởi động ứng dụng và khôi phục phiên

```mermaid
sequenceDiagram
  autonumber
  participant B as Trình duyệt
  participant AP as AuthProvider
  participant SS as auth/session.js
  participant BE as /api/auth/refresh
  participant APP as App.jsx
  B->>AP: mount
  AP->>AP: xóa khóa cũ localStorage (token, temp_user, temp_token)
  AP->>SS: refreshSession() — 1 promise dùng chung
  SS->>BE: POST (cookie taskify_refresh HttpOnly)
  alt cookie còn hiệu lực
    BE-->>SS: {token, userId, username, fullName, role} + Set-Cookie mới
    SS->>SS: setAccessToken(token) → báo listeners
    AP->>AP: user = localStorage.user ∪ dữ liệu mới, lưu lại
  else không có / hết hạn
    BE-->>SS: 401 + xóa cookie
    AP->>AP: clearLocal()
  end
  AP->>APP: ready = true
  alt có user và token
    APP->>B: AppLayout + AppRoutes theo vai trò
  else
    APP->>B: PublicRoutes (chỉ /login)
  end
```

## 3.3 Quản lý access token trong suốt phiên

```mermaid
flowchart TB
  subgraph MEM["Bộ nhớ (biến module auth/session.js)"]
    TOK["accessToken (JWT 15 phút)"]
    LST["listeners: AuthContext.setToken, WebSocket"]
  end
  subgraph LS["localStorage"]
    USR["key 'user' = {id, username, fullName, role}<br/>(chỉ thông tin hiển thị)"]
  end
  subgraph COOKIE["Cookie HttpOnly (JS không đọc được)"]
    RT["taskify_refresh · Path=/api/auth · SameSite=Strict · Secure (prod)"]
  end

  LOGIN["Đăng nhập xong (LoginPage)"] -->|"login(user, token)"| TOK
  LOGIN --> USR
  TIMER["Hẹn giờ trong AuthContext:<br/>exp − 60 giây (tối thiểu 5 giây)"] -->|"refreshSession()"| RT
  E401["Request bị 401"] -->|"interceptor: refresh 1 lần rồi gửi lại"| RT
  RT -->|"token mới"| TOK
  TOK --> LST
  OTHER["Tab khác đổi key 'user'"] -->|"storage event"| SYNC{"newValue?"}
  SYNC -->|"null → tab khác đăng xuất"| CLR["xóa token + user ở tab này"]
  SYNC -->|"có, tab này chưa có token"| RT
  LOGOUT["logout()"] -->|"POST /auth/logout"| RT
  LOGOUT --> CLR
```

## 3.4 Interceptor axios (`src/api.js`)

```mermaid
flowchart TB
  REQ["Gọi hàm api/*.js"] --> RI["request interceptor<br/>gắn Authorization: Bearer accessToken"]
  RI --> HTTP["HTTP /api/..."]
  HTTP --> RES{"Kết quả"}
  RES -->|"2xx"| OK["trả response.data"]
  RES -->|"401 và không phải /auth/*"| RETRY{"đã retry?"}
  RETRY -->|"chưa"| REF["refreshSession() → gửi lại request với token mới"]
  REF -->|"thành công"| OK
  REF -->|"thất bại"| EXP
  RETRY -->|"rồi"| EXP["sessionExpired():<br/>xóa localStorage.user → revokeSession()<br/>→ /login?reason=expired"]
  RES -->|"403"| W["toast cảnh báo<br/>(thông báo tiếng Việt từ server, hoặc câu mặc định)<br/>KHÔNG đăng xuất"]
  RES -->|"5xx / mất mạng"| D["toast lỗi chung"]
  RES -->|"4xx khác"| N
  W --> N["reject Error chuẩn hóa:<br/>message · status · url · data · code · fieldErrors{field: msg}"]
  D --> N
  EXP --> N
```

## 3.5 WebSocket client (`src/ws/stomp.js`)

```mermaid
sequenceDiagram
  participant P as Trang / component
  participant W as connectAndSubscribe
  participant L as import() @stomp/stompjs + sockjs-client
  participant S as Backend /ws
  P->>W: {destination, onMessage, onStatus}
  W-->>P: handle {deactivate()} (trả ngay)
  W->>L: tải lười (chỉ lần đầu, ~84 KB)
  L-->>W: Client, SockJS
  W->>S: beforeConnect: lấy access token MỚI NHẤT → CONNECT Authorization
  S-->>W: CONNECTED
  W->>S: SUBSCRIBE destination
  S-->>W: MESSAGE (JSON) → onMessage(payload)
  alt STOMP ERROR (token hết hạn / không đủ quyền)
    W->>W: deactivate
    W->>W: refreshSession() rồi activate lại MỘT lần
    Note over W: vẫn lỗi → dừng hẳn (không thử lại mỗi 3 giây)
  else mất mạng
    W->>S: tự kết nối lại sau 3 giây (heartbeat 10s/10s)
  end
  P->>W: unmount → deactivate()
```

Nơi dùng realtime:

| Kênh | Trang / component | Phản ứng |
|---|---|---|
| `/user/queue/notifications` | `NotificationsBell`, `NotificationsPage` | chèn thông báo mới, tăng số chưa đọc |
| `/user/queue/messages` | `ProjectChatPage` | thêm tin `MESSAGE_CREATED` vào hội thoại đang mở |
| `/topic/progress/{topicId}` | `StudentProgressPage`, `LecturerProgressPage`, `ProjectSpacePage` | tải lại danh sách báo cáo |
| `/topic/registration/{topicId}` | `LecturerRegistrationPage` | tải lại danh sách đăng ký |

## 3.6 Định tuyến theo vai trò

```mermaid
flowchart LR
  subgraph PUB["Chưa đăng nhập — PublicRoutes"]
    L["/login → LoginPage<br/>bước: login · otp · changePassword · activate · forgot · reset"]
    ANY["* → /login"]
  end

  subgraph ALL["Mọi vai trò"]
    HOME{"/"}
    DASH["/dashboard → DashboardPage"]
    NOTI["/notifications → NotificationsPage"]
    PROF["/profile → ProfilePage<br/>tab: info · password · notifications"]
  end
  HOME -->|"ADMIN / DEPT / GV"| DASH
  HOME -->|"STUDENT"| ST["StudentTopicPage"]

  subgraph SV["STUDENT"]
    ST
    MR["/my-registrations → StudentMyRegistrationsPage"]
    PG["/progress → StudentProgressPage (Kanban / bảng)"]
    PD["/progress-detail?milestoneId → StudentProgressDetailPage"]
  end

  subgraph GV["LECTURER"]
    LT["/lecturer-topics → LecturerTopicsPage"]
    LRG["/lecturer → LecturerRegistrationPage"]
    LP["/lecturer-progress → LecturerProgressPage"]
  end

  subgraph SHARED["STUDENT + LECTURER"]
    PS["/project-space → ProjectSpacePage"]
    PC["/project-chat → ProjectChatPage"]
  end

  subgraph AD["ADMIN"]
    A["/admin → AdminPage<br/>(DEPT_ADMIN vào đây → chuyển /department-admin)"]
    AR["/admin/roles → RoleManagementPage"]
    AA["/admin/audit → AuditLogPage"]
  end

  subgraph DA["DEPARTMENT_ADMIN"]
    DP["/department-admin → DepartmentAdminPage"]
  end
```

`RequireRole`: khi `ready=false` không quyết định gì (tránh đá ra khi đang khôi phục phiên); sai vai trò → `Navigate /login`. Backend vẫn kiểm tra lại mọi quyền — route guard chỉ để điều hướng.

Menu (`config/navigation.js`) dựng từ dữ liệu: mục **Tổng quan**, nhóm theo vai trò (`ROLE_SECTIONS`), **Hồ sơ của tôi**; tiêu đề cổng theo vai trò (`PORTAL_TITLES`).

## 3.7 Bản đồ trang → component → API

```mermaid
flowchart LR
  classDef page fill:#dbeafe,stroke:#1d4ed8,color:#000
  classDef api fill:#dcfce7,stroke:#15803d,color:#000

  LoginPage:::page --> authApi["auth.js"]:::api
  DashboardPage:::page --> dashboardApi["dashboardApi"]:::api
  StudentTopicPage:::page --> topicApi["topicApi"]:::api
  StudentTopicPage --> registrationApi["registrationApi"]:::api
  StudentMyRegistrationsPage:::page --> registrationApi
  StudentProgressPage:::page --> progressApi["progressApi"]:::api
  StudentProgressPage --> fileApi["fileApi"]:::api
  StudentProgressPage --> registrationApi
  StudentProgressDetailPage:::page --> milestoneApi["milestoneApi"]:::api
  StudentProgressDetailPage --> fileApi
  StudentProgressDetailPage --> progressApi
  LecturerTopicsPage:::page --> topicApi
  LecturerRegistrationPage:::page --> registrationApi
  LecturerProgressPage:::page --> progressApi
  LecturerProgressPage --> topicApi
  ProjectSpacePage:::page --> progressApi
  ProjectSpacePage --> milestoneApi
  ProjectSpacePage --> fileApi
  ProjectSpacePage --> registrationApi
  ProjectSpacePage --> topicApi
  ProjectChatPage:::page --> messageApi["messageApi"]:::api
  ProjectChatPage --> registrationApi
  ProjectChatPage --> topicApi
  NotificationsPage:::page --> notificationsApi["notificationsApi"]:::api
  ProfilePage:::page --> userApi["userApi"]:::api
  ProfilePage --> notificationsApi
  AdminPage:::page --> userApi
  AdminPage --> departmentApi["departmentApi"]:::api
  AdminPage --> importApi["importApi"]:::api
  AdminPage --> registrationApi
  RoleManagementPage:::page --> roleApi["roleApi"]:::api
  AuditLogPage:::page --> auditApi["auditApi"]:::api
  DepartmentAdminPage:::page --> workspaceApi["workspaceApi"]:::api
  DepartmentAdminPage --> departmentApi
  DepartmentAdminPage --> userApi
  DepartmentAdminPage --> topicApi
  DepartmentAdminPage --> dashboardApi
  DepartmentAdminPage --> importApi
  DepartmentAdminPage --> registrationApi
```

### Chi tiết từng trang

| Trang | Thành phần con | Gọi API | Realtime |
|---|---|---|---|
| `LoginPage` | `login/LoginStep`, `OtpStep`, `ChangePasswordStep`, `ActivateStep`, `ForgotStep`, `ResetStep`, `PasswordChecklist` | `auth.login`, `verifyOtp`, `resendOtp`, `changeInitialPassword`, `activate`, `forgotPassword`, `resetPassword` | — |
| `DashboardPage` | `dashboard/LecturerSection`, `StudentSection`, `OverviewSection`, `charts` | `getLecturerStats` / `getStudentStats` / `getOverview` + `exportOverview` (ADMIN, DEPT) | — |
| `StudentTopicPage` | `TopicCard`, `Pagination` | `topicApi.getAllTopics(studentId…)`, `registrationApi.registerTopic` | — |
| `StudentMyRegistrationsPage` | thẻ theo trạng thái Chờ / Đã duyệt / Bị từ chối | `registrationApi.getMine`, `cancel` | — |
| `StudentProgressPage` | `studentProgress/KanbanColumn`, `KanbanCard`, `ProgressForm`, `ProgressTable`, `kanbanConfig` | `getMine`, `progressApi.listByStudent`, `create`, `updateStatus`, `fileApi.upload` | `/topic/progress/{id}` |
| `StudentProgressDetailPage` | form nộp theo mốc, đồng hồ hạn | `milestoneApi.get`, `fileApi.upload`, `progressApi.create(milestoneId)` | — |
| `LecturerTopicsPage` | `lecturerTopics/TopicTable`, `TopicFormModal`, `topicForm` | `getAllTopics(lecturerId)`, `workspacesOpenForTopics`, `create`, `update`, `remove`, `open`, `close` | — |
| `LecturerRegistrationPage` | `RegistrationTable`, `lecturerRegistration/RegistrationReviewModal`, `GradeHistoryModal` | `getByTopic`, `approve`, `reject`, `grade`, `requestRevision`, `gradeHistory`, `exportExcel` | `/topic/registration/{id}` |
| `LecturerProgressPage` | `ProgressRow`, `FileDownloadButton` | `getAllTopics`, `listByTopic`, `comment` | `/topic/progress/{id}` |
| `ProjectSpacePage` | `projectSpace/MilestonePanel`, `MilestoneForm`, `MilestoneGradeGrid`, `StudentMilestoneList`, `ProgressSubmitPanel`, `ProgressHistoryPanel` | `milestoneApi.board/create/update/delete/grade`, `progressApi.*`, `fileApi.upload` | `/topic/progress/{id}` |
| `ProjectChatPage` | `projectChat/ChatPeopleSidebar`, `ChatConversation` | `getMine` / `getByTopic` (danh sách người nhận), `messageApi.conversation` (phân trang), `send` | `/user/queue/messages` |
| `NotificationsPage` | danh sách + lọc | `listMine`, `markRead`, `markAllRead` | `/user/queue/notifications` |
| `ProfilePage` | `profile/NotificationSettingsTab` | `userApi.get`, `updateProfile`, `changePassword`, `getSettings`, `saveSettings` | — |
| `AdminPage` | `admin/CreateUserTab`, `UserListTab`, `DepartmentsTab`, `userImport/UserImportPanel`, `GradeRevisionPanel`, hook `useAdmin` | `userApi.*`, `departmentApi.list/create/listClasses`, `importApi.*`, `registrationApi.listRevisions/approveRevision/rejectRevision`, `gradeHistory` | — |
| `RoleManagementPage` | ma trận vai trò × quyền | `roleApi.getAllRoles/getAllPrivileges/createRole/updateRole/deleteRole` | — |
| `AuditLogPage` | `auditLog/AuditFilters`, `AuditTable` | `auditApi.search`, `options`, `exportCsv` | — |
| `DepartmentAdminPage` | `departmentAdmin/WorkspacesTab`, `ClassesTab`, `WorkspaceClassesTab`, `AssignmentsTab`, `TopicsTab`, `StatsTab`, `UserImportPanel`, `GradeRevisionPanel`, hook `useDepartmentAdmin` | `workspaceApi.*`, `departmentApi.*`, `userApi.getByDepartment`, `topicApi.getAllTopics/open/close`, `dashboardApi.getDepartmentAdminStats` | — |

## 3.8 Tách trang lớn: trang → hook dữ liệu → tab

```mermaid
flowchart LR
  DAP["DepartmentAdminPage.jsx<br/>(chỉ điều phối tab)"] --> UDA["useDepartmentAdmin.js<br/>state + toàn bộ lời gọi API + notice"]
  UDA --> T1["WorkspacesTab"]
  UDA --> T2["ClassesTab"]
  UDA --> T3["WorkspaceClassesTab"]
  UDA --> T4["AssignmentsTab"]
  UDA --> T5["TopicsTab"]
  UDA --> T6["StatsTab"]
  DAP --> T7["UserImportPanel (tự quản state)"]
  DAP --> T8["GradeRevisionPanel (tự quản state)"]

  AP["AdminPage.jsx"] --> UA["useAdmin.js"]
  UA --> A1["CreateUserTab"]
  UA --> A2["UserListTab"]
  UA --> A3["DepartmentsTab"]
  AP --> T7
  AP --> T8
```

Quy tắc chung (GĐ3 — P3-11): tệp lớn nhất ≤ 353 dòng; `useLoadEffect` tải dữ liệu sau render (không `setState` đồng bộ trong effect, thỏa React Compiler); `npm run lint` chặn mọi cảnh báo.

## 3.9 API backend chưa có giao diện

Những endpoint dưới đây đã chạy ở backend (một số đã có hàm trong `src/api/*`) nhưng **chưa trang nào gọi** — cần lưu ý khi làm tài liệu hoặc lên kế hoạch giai đoạn sau.

| Endpoint | Hàm frontend sẵn có | Ghi chú |
|---|---|---|
| `GET/POST /api/discuss/threads`, `/threads/{id}/posts` | `discussApi.threads/createThread/posts/addPost` | thảo luận nhóm theo chủ đề |
| `GET/POST /api/announcements` | `announcementApi` | thông báo chung theo đề tài / toàn hệ thống |
| `GET/POST/DELETE /api/calendar` | `calendarApi` | lịch đề tài (đã gộp mốc) |
| `GET /api/messages/inbox` | `messageApi.inbox` | hộp thư gom nhóm theo hội thoại |
| `POST /api/notifications/create` | — | gửi thông báo thủ công |
| `POST /api/auth/logout-all` | — | đăng xuất mọi thiết bị |
| `GET /api/dashboard/admin` | `dashboardApi.getAdminStats` | số liệu nhanh toàn hệ thống |
| `GET /api/audit/recent` | — | nhật ký gần đây |
| `POST /api/roles/privileges` | `roleApi.createPrivilege` | tạo quyền mới |
| `PUT /api/departments/{id}`, `/status` | — | sửa / bật-tắt khoa |
| `PUT /api/classes/{id}` | — | sửa lớp |
| `GET/PUT /api/workspaces/{id}`, `/open`, `/close` | — | xem / sửa đợt (UI dùng `/transition`) |
