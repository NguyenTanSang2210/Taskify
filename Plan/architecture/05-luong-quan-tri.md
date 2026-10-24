# 05 — Luồng B: Quản trị người dùng & tổ chức

[← Mục lục](README.md)

Thành phần chính: `AdminPage` (`useAdmin`, `CreateUserTab`, `UserListTab`, `DepartmentsTab`) · `UserImportPanel` · `RoleManagementPage` · `ProfilePage` (+ `NotificationSettingsTab`) · `UserService` · `UserImportService` + `ImportWorkbook` + `ImportKind` · `DepartmentService` · `AcademicClassService` · `RoleService` · `NotificationSettingsService`.

## B1. Tạo tài khoản

```mermaid
sequenceDiagram
  autonumber
  actor AD as ADMIN
  participant FE as CreateUserTab (useAdmin)
  participant C as UserController
  participant S as UserService
  participant DB as users / students / lecturers
  participant O as OtpService
  AD->>FE: username, họ tên, email, SĐT, vai trò, khoa,<br/>(SV) mã SV + lớp · (GV) học vị, chuyên ngành,<br/>mật khẩu tạm (tùy chọn)
  FE->>C: POST /api/users
  C->>S: create(body)
  alt không nhập mật khẩu
    S->>S: bắt buộc email hợp lệ (để gửi mã kích hoạt)
  else có mật khẩu tạm
    S->>S: PasswordPolicy
  end
  S->>S: có vai trò · username chưa có · email chưa dùng
  S->>S: không phải ADMIN → bắt buộc thuộc khoa
  S->>S: STUDENT → bắt buộc mã SV + lớp, lớp cùng khoa
  S->>DB: INSERT users<br/>activated = (có mật khẩu) · mustChangePassword = (có mật khẩu)<br/>không mật khẩu → bcrypt(UUID ngẫu nhiên)
  opt vai trò STUDENT
    S->>DB: INSERT students (id = user.id, studentCode, academicClass)
  end
  opt vai trò LECTURER
    S->>DB: INSERT lecturers (id = user.id, department, degree, speciality)
  end
  S->>S: audit CREATE_USER
  opt không mật khẩu
    S->>O: issue(ACTIVATION) → email mã 72 giờ (gửi sau commit)
  end
  S-->>FE: UserDTO
```

Hai cách cấp tài khoản và hệ quả ở lần đăng nhập đầu:

```mermaid
flowchart LR
  A["Admin tạo"] --> Q{"đặt mật khẩu tạm?"}
  Q -->|"có"| T["activated = true<br/>mustChangePassword = true"] --> L1["Đăng nhập bằng mật khẩu tạm<br/>→ bắt buộc đổi (A4)"]
  Q -->|"không"| N["activated = false<br/>email mã kích hoạt"] --> L2["Người dùng kích hoạt (A5)<br/>tự đặt mật khẩu"]
```

## B2 – B5. Danh bạ & thao tác trên tài khoản

```mermaid
flowchart TB
  subgraph LIST["UserListTab"]
    F["Bộ lọc: vai trò, từ khóa (username / họ tên / email), khoa, phân trang"]
    F -->|"ADMIN"| G1["GET /api/users?role&q&page&size"]
    F -->|"lọc theo khoa (ADMIN, DEPT_ADMIN)"| G2["GET /api/users/by-department?departmentId&role&q"]
  end
  LIST --> ACT{"Thao tác trên 1 dòng"}
  ACT -->|"Khóa / mở"| S1["PUT /api/users/{id}/status?active<br/>khóa → tokenVersion + 1 (đá khỏi mọi phiên ngay)<br/>audit LOCK_USER / UNLOCK_USER"]
  ACT -->|"Đổi vai trò"| S2["PUT /api/users/{id}/role?roleId<br/>tokenVersion + 1 (token mang vai trò cũ hết hiệu lực)<br/>audit UPDATE_USER_ROLE"]
  ACT -->|"Gửi lại mã kích hoạt"| S3["POST /api/users/{id}/resend-activation<br/>chỉ khi chưa kích hoạt và có email<br/>chờ 60 giây giữa 2 lần (429)<br/>audit RESEND_ACTIVATION"]
```

Quyền xem hồ sơ (`GET /api/users/{id}`): **đầy đủ** (email, SĐT, vai trò, khoa) cho chính chủ, ADMIN, quản trị khoa cùng khoa; người khác chỉ thấy `id` + họ tên.

## B6. Nhập sinh viên / giảng viên từ Excel

```mermaid
sequenceDiagram
  autonumber
  actor Q as ADMIN / DEPARTMENT_ADMIN
  participant FE as UserImportPanel
  participant C as UserImportController
  participant S as UserImportService
  participant WB as ImportWorkbook (Apache POI)
  participant DB as MySQL
  participant O as OtpService → Mail
  Q->>FE: chọn loại STUDENT / LECTURER, khoa (ADMIN), tùy chọn "tạo lớp còn thiếu"
  FE->>C: GET /api/users/import/template?kind
  C-->>FE: file mẫu .xlsx (tiêu đề + dòng ví dụ)
  Q->>FE: chọn file đã điền
  FE->>C: POST /api/users/import/preview (multipart)
  C->>S: preview(file, kind, departmentId, createMissingClasses)
  S->>S: resolveDepartment — DEPT_ADMIN chỉ khoa mình
  S->>WB: read: .xlsx · ≤ 5 MB · ≤ 2000 dòng · đủ cột bắt buộc (nhận tên cột bỏ dấu)
  S->>DB: tìm username / email đã tồn tại (1 truy vấn mỗi loại)
  S->>S: kiểm từng dòng (xem bảng dưới)
  S-->>FE: ImportReport {total, valid, invalid, rows[], newClasses[]} (KHÔNG ghi gì)
  FE->>FE: ImportPreviewTable + tải CSV các dòng lỗi (errorsCsv.js)
  Q->>FE: Xác nhận nhập
  FE->>C: POST /api/users/import (cùng file)
  C->>S: commit — phân tích lại y hệt
  alt không có dòng hợp lệ
    S-->>FE: 400 IMPORT_FILE_INVALID
  end
  S->>DB: (STUDENT) tạo lớp còn thiếu nếu được phép
  S->>DB: tạo user chưa kích hoạt + students / lecturers (1 transaction, bỏ qua dòng lỗi)
  S->>O: issue(ACTIVATION) cho từng tài khoản (email gửi sau commit, giãn nhịp 200 ms)
  S->>S: audit IMPORT_USERS (created, skipped, newClasses)
  S-->>FE: ImportReport committed = true, created = N
```

| Cột (SV) | Cột (GV) | Kiểm tra |
|---|---|---|
| MSSV* | Mã giảng viên* | ≤ 50 ký tự, chỉ `[A-Za-z0-9._-]`, chưa có tài khoản, không trùng trong file |
| Họ tên* | Họ tên* | ≤ 100 ký tự |
| Email* | Email* | định dạng hợp lệ, ≤ 100, chưa dùng, không trùng trong file |
| Mã lớp* | Học vị, Chuyên ngành | lớp phải có trong khoa và đang hoạt động (hoặc được phép tạo mới) · học vị ≤ 100, chuyên ngành ≤ 255 |
| Số điện thoại | Số điện thoại | ≤ 20 ký tự |

## B7 · B8. Khoa và lớp

```mermaid
flowchart LR
  subgraph DEPT["Khoa (ADMIN)"]
    D1["GET /api/departments<br/>(ADMIN, DEPT_ADMIN)"]
    D2["POST /api/departments<br/>{code, name, ...} → CREATE_DEPARTMENT"]
    D3["PUT /api/departments/{id} → UPDATE_DEPARTMENT ⚠️"]
    D4["PUT /api/departments/{id}/status?active<br/>ENABLE / DISABLE_DEPARTMENT ⚠️"]
  end
  subgraph CLS["Lớp sinh hoạt (ADMIN, DEPT_ADMIN)"]
    C1["GET /api/classes?departmentId<br/>DEPT_ADMIN: chỉ khoa mình"]
    C2["POST /api/classes {departmentId, code, name}<br/>→ CREATE_CLASS"]
    C3["PUT /api/classes/{id} → UPDATE_CLASS ⚠️"]
  end
  DEPT --> CLS
  CLS --> USE["Dùng khi: tạo SV (B1), nhập Excel (B6),<br/>gán lớp vào đợt (C4), kiểm tra SV đăng ký (E1)"]
```
⚠️ = có API, chưa có nút trên giao diện. UI: `DepartmentsTab` (AdminPage), `ClassesTab` (DepartmentAdminPage).

## B9. Vai trò & quyền hạn

```mermaid
sequenceDiagram
  autonumber
  actor AD as ADMIN
  participant FE as RoleManagementPage
  participant S as RoleService
  FE->>S: GET /api/roles + GET /api/roles/privileges
  S-->>FE: ma trận vai trò × quyền
  AD->>FE: tạo vai trò mới
  FE->>S: POST /api/roles {name, description}
  S->>S: tên in hoa không dấu [A-Z0-9_] 2–50 ký tự (vd. THU_KY), chưa tồn tại → CREATE_ROLE
  AD->>FE: tích / bỏ quyền, sửa mô tả
  FE->>S: PUT /api/roles/{id} {description, privilegeIds}
  S->>S: UPDATE_ROLE
  AD->>FE: xóa vai trò
  FE->>S: DELETE /api/roles/{id}
  alt vai trò hệ thống (ADMIN, DEPARTMENT_ADMIN, LECTURER, STUDENT)
    S-->>FE: 400 không được xóa
  else đang có người dùng
    S-->>FE: 400 vai trò đang gán cho N người dùng
  else
    S->>S: DELETE_ROLE
  end
  Note over AD,S: POST /api/roles/privileges (tạo quyền mới) có API, chưa có UI
```

## B10. Hồ sơ cá nhân

```mermaid
sequenceDiagram
  autonumber
  actor U as Người dùng
  participant FE as ProfilePage
  participant C as UserController
  participant S as UserService
  participant SC as SessionCookies
  U->>FE: mở tab "Thông tin"
  FE->>C: GET /api/users/{myId}
  U->>FE: sửa họ tên, email, SĐT
  FE->>C: PUT /api/users/{id}/profile
  C->>S: chỉ sửa chính mình · email hợp lệ, chưa ai dùng → UPDATE_PROFILE
  FE->>FE: AuthContext.updateUser
  U->>FE: tab "Mật khẩu": hiện tại + mới
  FE->>C: PUT /api/users/{id}/password
  C->>S: chỉ chính mình · mật khẩu hiện tại đúng · mới ≠ cũ · PasswordPolicy
  S->>S: tokenVersion + 1, revokeAll refresh tokens, CHANGE_PASSWORD
  S-->>C: access token mới
  C->>SC: restart() → cookie refresh mới cho chính thiết bị này
  C-->>FE: {token}
  FE->>FE: updateToken(token) — vẫn đăng nhập, mọi thiết bị khác bị đăng xuất
```

## B11. Cài đặt email thông báo

```mermaid
flowchart LR
  T["ProfilePage → tab Thông báo<br/>NotificationSettingsTab"] -->|"GET /api/notification-settings"| G["NotificationSettingsDTO<br/>(chưa có bản ghi → mặc định bật hết, gửi ngay)<br/>+ số mục đang chờ tổng hợp"]
  T -->|"PUT /api/notification-settings"| P["{emailDigest, registration, progress, grade, reminder}"]
  P --> EFF["NotificationMailer đọc cài đặt cho từng email:<br/>nhóm tắt → bỏ · digest → xếp hàng 07:30 · còn lại → gửi ngay"]
  NOTE["Email bảo mật (OTP, kích hoạt, đặt lại mật khẩu)<br/>KHÔNG bị ảnh hưởng"] -.-> EFF
```
