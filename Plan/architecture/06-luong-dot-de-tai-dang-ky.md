# 06 — Luồng C · D · E: Đợt đồ án, Đề tài, Đăng ký

[← Mục lục](README.md)

Thành phần chính: `DepartmentAdminPage` (`useDepartmentAdmin`, `WorkspacesTab`, `WorkspaceClassesTab`, `AssignmentsTab`, `TopicsTab`) · `LecturerTopicsPage` · `StudentTopicPage` · `StudentMyRegistrationsPage` · `LecturerRegistrationPage` · `WorkspaceService` · `WorkspaceClassService` · `LecturerAssignmentService` · `TopicService` · `TopicRegistrationService`.

---

# C. Đợt đồ án (workspace)

## C1 · C2. Tạo và sửa đợt

```mermaid
sequenceDiagram
  autonumber
  actor K as DEPT_ADMIN / ADMIN
  participant FE as WorkspacesTab
  participant S as WorkspaceService
  participant SC as SecurityScopeService
  K->>FE: tên, loại, học kỳ, thời gian bắt đầu / kết thúc
  FE->>S: POST /api/workspaces
  alt ADMIN
    S->>S: bắt buộc body.departmentId
  else DEPT_ADMIN
    S->>S: departmentId = khoa của chính mình
  end
  S->>SC: requireDepartmentAccess(departmentId)
  S->>S: endAt ≥ startAt
  S->>S: status = DRAFT · audit CREATE_WORKSPACE
  S-->>FE: WorkspaceDTO
  Note over K,S: Sửa đợt — PUT /api/workspaces/{id} (API, chưa có UI)
  K->>S: PUT {name?, type?, semester?, active?, startAt?, endAt?}
  alt đợt khác DRAFT và body chạm tên / loại / học kỳ / ngày
    S-->>K: 400 WORKSPACE_PHASE "Chỉ sửa thông tin đợt khi đợt còn là bản nháp"
  else
    S->>S: cập nhật, kiểm ngày · audit UPDATE_WORKSPACE
  end
```

## C3. Chuyển trạng thái đợt

```mermaid
flowchart TB
  IN["POST /api/workspaces/{id}/transition?to=...<br/>(open = OPEN_TOPIC, close = CLOSED)"] --> AC["requireDepartmentAccess(khoa của đợt)"]
  AC --> P["WorkspaceStatus.parse(to)<br/>nhận chữ thường và tên cũ OPEN = OPEN_TOPIC"]
  P --> T{"current.canTransitionTo(target)?"}
  T -->|"không"| E1["400 INVALID_TRANSITION<br/>'Không thể chuyển đợt từ X sang Y'"]
  T -->|"có"| PRE{"target = OPEN_REGISTRATION?"}
  PRE -->|"có"| C1{"≥ 1 lớp active trong đợt?"}
  C1 -->|"không"| E2["400 'Cần gán ít nhất một lớp'"]
  C1 -->|"có"| C2{"≥ 1 đề tài trong đợt?"}
  C2 -->|"không"| E3["400 'Cần có ít nhất một đề tài'"]
  C2 -->|"có"| SAVE
  PRE -->|"không"| SAVE["status = target<br/>audit TRANSITION_WORKSPACE 'X -> Y'"]
```

Mỗi trạng thái mở khóa những thao tác gì:

```mermaid
flowchart LR
  DRAFT["DRAFT"] --- A1["sửa thông tin đợt<br/>gán lớp · phân công GV"]
  OT["OPEN_TOPIC"] --- A2["gán lớp · phân công GV<br/>GV tạo / sửa / xóa đề tài<br/>đóng / mở đề tài"]
  OR["OPEN_REGISTRATION"] --- A3["SV đăng ký / hủy<br/>GV duyệt / từ chối<br/>đóng / mở đề tài"]
  LKR["LOCK_REGISTRATION"] --- A4["GV duyệt / từ chối phần còn lại"]
  IP["IN_PROGRESS"] --- A5["nộp tiến độ · nhận xét<br/>chấm điểm mốc · chấm điểm tổng kết"]
  CL["CLOSED"] --- A6["điểm khóa · chấm lại có duyệt<br/>xem thống kê, xuất Excel"]
  DRAFT --> OT --> OR --> LKR --> IP --> CL
```

## C4. Gán / gỡ lớp vào đợt

```mermaid
sequenceDiagram
  autonumber
  actor K as DEPT_ADMIN / ADMIN
  participant FE as WorkspaceClassesTab
  participant S as WorkspaceClassService
  FE->>S: GET /api/workspace-classes?workspaceId
  K->>FE: chọn lớp → Gán
  FE->>S: POST /assign {workspaceId, classId}
  S->>S: quyền khoa của đợt
  S->>S: đợt ở DRAFT / OPEN_TOPIC, nếu không → WORKSPACE_PHASE
  S->>S: lớp phải cùng khoa với đợt
  S->>S: tìm liên kết cũ → bật lại active = true (hoặc tạo mới)
  S->>S: audit ASSIGN_CLASS_TO_WORKSPACE
  K->>FE: Gỡ lớp
  FE->>S: POST /unassign → active = false (giữ bản ghi) · UNASSIGN_CLASS_FROM_WORKSPACE
```
Lớp active của đợt quyết định **sinh viên nào được đăng ký** đề tài trong đợt (E1).

## C5. Phân công giảng viên + quota

```mermaid
sequenceDiagram
  autonumber
  actor K as DEPT_ADMIN / ADMIN
  participant FE as AssignmentsTab
  participant S as LecturerAssignmentService
  FE->>FE: tải GV của khoa (GET /users/by-department?role=LECTURER)
  FE->>S: GET /api/assignments?workspaceId
  K->>FE: chọn GV, loại MAIN / ASSISTANT, hạn mức nhóm (0 = không giới hạn), lý do
  FE->>S: POST /api/assignments/assign
  S->>S: quyền khoa · đợt DRAFT / OPEN_TOPIC
  S->>S: GV cùng khoa với đợt (ADMIN được bỏ qua)
  S->>S: chưa có hồ sơ lecturers → tạo
  S->>S: upsert theo (workspace, lecturer, type): quota, active = true, reason
  S->>S: audit ASSIGN_LECTURER
  K->>FE: Thu hồi
  FE->>S: POST /api/assignments/revoke {assignmentId, reason} → active = false · REVOKE_LECTURER_ASSIGNMENT
```
> Ghi chú hiện trạng: `quotaMaxGroups` và phân công được lưu và hiển thị (dashboard đếm "GV chưa được phân công"), nhưng **chưa được kiểm tra** khi giảng viên tạo đề tài hay duyệt đăng ký — GV cùng khoa vẫn tạo được đề tài trong đợt kể cả khi chưa được phân công.

## C6. Khoa duyệt đề tài (đóng / mở)

```mermaid
flowchart LR
  T["TopicsTab (DepartmentAdminPage)"] -->|"GET /api/topics (tự lọc khoa của DEPT_ADMIN)"| L["danh sách đề tài + số đăng ký"]
  L -->|"Đóng"| C["POST /api/topics/{id}/close"]
  L -->|"Mở lại"| O["POST /api/topics/{id}/open"]
  C --> R["TopicService.canModifyTopic: DEPT_ADMIN đúng khoa<br/>đợt OPEN_TOPIC / OPEN_REGISTRATION"]
  O --> R
```

---

# D. Đề tài

## D1 · D2. GV tạo / sửa / xóa đề tài

```mermaid
sequenceDiagram
  autonumber
  actor GV as Giảng viên
  participant FE as LecturerTopicsPage + TopicFormModal
  participant S as TopicService
  participant DB as topics
  FE->>S: GET /api/workspaces/open-for-topics (đợt OPEN_TOPIC của khoa mình)
  GV->>FE: chọn đợt, tên, mô tả, số SV (capacity)
  FE->>S: POST /api/topics/create?lecturerId=me
  S->>S: @PreAuthorize hasAuthority TOPIC_MANAGE hoặc ADMIN
  S->>S: LECTURER chỉ tạo cho chính mình (lecturerId == currentUser)
  S->>S: bỏ qua id trong body (luôn tạo mới)
  S->>S: tên 1–255 · mô tả ≤ 10.000 · capacity 1–10 (mặc định 1)
  S->>S: đợt tồn tại và đang OPEN_TOPIC, nếu không → WORKSPACE_PHASE
  S->>S: DEPT_ADMIN: đợt thuộc khoa mình
  S->>S: người hướng dẫn phải là tài khoản LECTURER cùng khoa với đợt
  S->>DB: status = OPEN · audit CREATE_TOPIC
  GV->>FE: Sửa
  FE->>S: PUT /api/topics/{id}?lecturerId
  S->>S: canModifyTopic · đợt OPEN_TOPIC
  S->>S: capacity mới ≥ số SV đã duyệt → refreshCapacityStatus (OPEN / FULL)
  S->>S: trạng thái KHÔNG nhận từ body · audit UPDATE_TOPIC
  GV->>FE: Xóa
  FE->>S: DELETE /api/topics/{id} · canModifyTopic · đợt OPEN_TOPIC · DELETE_TOPIC
```

`canModifyTopic(topic, lecturerId)`: ADMIN luôn được · DEPT_ADMIN nếu đề tài thuộc khoa mình · LECTURER nếu `lecturerId` là chính mình **và** là GV hướng dẫn của đề tài.

## D3. Đóng / mở đề tài

```mermaid
flowchart LR
  O["POST /topics/{id}/open"] --> G["canModifyTopic + đợt OPEN_TOPIC / OPEN_REGISTRATION"]
  C["POST /topics/{id}/close"] --> G
  G -->|"open"| S1["status = OPEN → refreshCapacityStatus(số SV duyệt)<br/>đủ chỗ → FULL"]
  G -->|"close"| S2["status = CLOSED<br/>(không nhận đăng ký mới)"]
```

## D4. Danh sách & tìm kiếm đề tài

```mermaid
flowchart TB
  Q["GET /api/topics?studentId&q&status&lecturerId&page&size"] --> SP["Specification JPA:<br/>status = ? · lecturer.id = ? · lower(title) LIKE %q%"]
  SP --> DA{"DEPT_ADMIN (không ADMIN)?"}
  DA -->|"có"| F["thêm điều kiện workspace.department = khoa mình<br/>(chưa thuộc khoa → trang rỗng)"]
  DA -->|"không"| PG
  F --> PG["phân trang, sắp id giảm dần"]
  PG --> AGG["1 truy vấn gom nhóm: total / pending / approved / rejected theo topicIds"]
  AGG --> MY{"có studentId?"}
  MY -->|"có"| ST["1 truy vấn: trạng thái đăng ký mới nhất của SV theo từng đề tài<br/>CHUA_DANG_KY · CHO_DUYET · DA_DUYET · TU_CHOI"]
  MY -->|"không"| OUT
  ST --> OUT["PageResponse{items[id, title, description, status, capacity,<br/>workspaceId/Name, lecturerName/Id, các đếm, registrationStatus]}"]
```
Dùng ở: `StudentTopicPage` (thẻ `TopicCard` + nút Đăng ký), `LecturerTopicsPage` (lọc `lecturerId`), `TopicsTab` của khoa, chọn đề tài ở `LecturerProgressPage` / `ProjectSpacePage` / `ProjectChatPage`.

---

# E. Đăng ký đề tài

## E1. Sinh viên đăng ký — chuỗi kiểm tra

```mermaid
flowchart TB
  A["StudentTopicPage → Đăng ký<br/>POST /api/registration/register?studentId&topicId"] --> K1{"studentId == người đang đăng nhập?"}
  K1 -->|"không"| X1["403"]
  K1 -->|"có"| K2{"đề tài FULL?"}
  K2 -->|"có"| X2["400 TOPIC_FULL"]
  K2 -->|"không"| K3{"đề tài OPEN?"}
  K3 -->|"không"| X3["400 'Đề tài không mở đăng ký'"]
  K3 -->|"có"| K4{"đợt OPEN_REGISTRATION?"}
  K4 -->|"không"| X4["400 WORKSPACE_PHASE"]
  K4 -->|"có"| K5{"SV đã xếp lớp?"}
  K5 -->|"không"| X5["400"]
  K5 -->|"có"| K6{"lớp của SV active trong đợt?"}
  K6 -->|"không"| X6["403 'Lớp của bạn không thuộc đợt'"]
  K6 -->|"có"| K7{"đã có đề tài ĐƯỢC DUYỆT trong đợt?"}
  K7 -->|"có"| X7["400 DUPLICATE_REGISTRATION"]
  K7 -->|"không"| K8{"đang có đăng ký CHỜ DUYỆT trong đợt?"}
  K8 -->|"có"| X8["400 DUPLICATE_REGISTRATION"]
  K8 -->|"không"| K9{"đã từng đăng ký chính đề tài này<br/>(kể cả bị từ chối)?"}
  K9 -->|"có"| X9["400 DUPLICATE_REGISTRATION"]
  K9 -->|"không"| K10{"số SV đã duyệt ≥ capacity?"}
  K10 -->|"có"| X10["400 TOPIC_FULL"]
  K10 -->|"không"| OK["INSERT approved = null (chờ duyệt)<br/>REGISTRATION_CREATED → /topic/registration/{id}<br/>audit REGISTER_TOPIC"]
```

## E2. Sinh viên hủy đăng ký

```mermaid
sequenceDiagram
  actor SV as Sinh viên
  participant FE as StudentMyRegistrationsPage
  participant S as TopicRegistrationService
  FE->>S: GET /api/registration/mine?studentId (kèm gradeState, điểm)
  SV->>FE: Hủy (chỉ thẻ "Chờ phê duyệt")
  FE->>S: POST /api/registration/cancel/{regId}
  S->>S: đúng chủ đăng ký · approved == null · đợt OPEN_REGISTRATION
  S->>S: DELETE bản ghi · audit CANCEL_REGISTRATION
  S-->>FE: OK
  S-)S: afterCommit → REGISTRATION_CANCELLED tới GV đang xem
```

## E3 · E4. Giảng viên duyệt / từ chối

```mermaid
sequenceDiagram
  autonumber
  actor GV as Giảng viên
  participant FE as LecturerRegistrationPage + RegistrationReviewModal
  participant S as TopicRegistrationService
  participant DB as MySQL
  participant N as NotificationService
  participant M as NotificationMailer
  participant WS as /topic/registration/{topicId}
  FE->>WS: subscribe (chỉ owner đề tài được phép)
  FE->>S: GET /api/registration/topic/{topicId}
  GV->>FE: Duyệt
  FE->>S: POST /api/registration/approve/{regId}
  S->>S: người gọi là GV hướng dẫn của đề tài · chưa duyệt
  S->>DB: SELECT topic FOR UPDATE (khóa dòng — 2 lượt duyệt đồng thời không vượt capacity)
  S->>S: đợt OPEN_REGISTRATION / LOCK_REGISTRATION, nếu không → WORKSPACE_PHASE
  S->>DB: đếm SV đã duyệt < capacity, nếu không → TOPIC_FULL
  S->>DB: approved = true, reviewer, reviewedAt
  S->>DB: refreshCapacityStatus → OPEN / FULL
  S->>WS: REGISTRATION_UPDATED (sau commit)
  S->>N: "Đăng ký đề tài đã được duyệt" → /my-registrations
  S->>M: email nhóm REGISTRATION
  S->>S: audit APPROVE_REGISTRATION
  GV->>FE: Từ chối + lý do
  FE->>S: POST /api/registration/reject/{regId}?reason
  S->>S: GV hướng dẫn · đợt cho phép duyệt
  S->>DB: approved = false, rejectReason, reviewer
  S->>DB: refreshCapacityStatus (SV đã duyệt bị từ chối → đề tài có thể còn chỗ)
  S->>N: "Đăng ký đề tài bị từ chối" + lý do
  S->>M: email REGISTRATION · audit REJECT_REGISTRATION
```

## E5 · E6. Xem đăng ký và cập nhật realtime

```mermaid
flowchart LR
  subgraph SV["Sinh viên"]
    M1["GET /registration/mine<br/>(chỉ chính mình)"] --> M2["GradeService.describe(reg, forReviewer = false)<br/>+ gradeState, scoreRevisedAt"]
  end
  subgraph GV["GV / khoa / ADMIN"]
    T1["GET /registration/topic/{id}<br/>ADMIN: mọi đề tài · DEPT: khoa mình · GV: đề tài mình"] --> T2["describe(reg, forReviewer = true)<br/>+ số lần sửa điểm, yêu cầu chấm lại gần nhất"]
  end
  EV["registerTopic / approve / reject / cancel"] -->|"EventPublisher.registrationChanged"| CH["/topic/registration/{topicId}"]
  CH --> LRP["LecturerRegistrationPage tải lại danh sách"]
```
