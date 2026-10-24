# 08 — Luồng H: Chấm điểm, khóa điểm, chấm lại

[← Mục lục](README.md)

Thành phần chính: `LecturerRegistrationPage` + `RegistrationReviewModal` · `GradeHistoryModal` · `GradeRevisionPanel` (tab "Yêu cầu chấm lại" ở `AdminPage` và `DepartmentAdminPage`) · `GradeService` · `GradeRevisionScheduler` · `ExportService`. Quyết định thiết kế: [ADR-0008](../../docs/adr/0008-khoa-diem-va-cham-lai.md).

## Toàn cảnh

```mermaid
flowchart LR
  subgraph IP["Đợt IN_PROGRESS — GradeState OPEN"]
    G1["GV chấm lần đầu"] --> G2["GV sửa điểm (bắt buộc lý do)"]
  end
  subgraph CL["Đợt CLOSED — GradeState LOCKED"]
    R1["GV gửi yêu cầu chấm lại + lý do"] --> R2{"Khoa / ADMIN xét"}
    R2 -->|"duyệt, mở khóa 1–168 giờ"| UN["GradeState UNLOCKED"]
    R2 -->|"từ chối"| LK["vẫn LOCKED"]
    UN -->|"GV sửa 1 lần (bắt buộc lý do)"| US["yêu cầu USED → LOCKED"]
    UN -->|"quá hạn (job mỗi giờ)"| EX["yêu cầu EXPIRED → LOCKED"]
  end
  IP -->|"transition CLOSED"| CL
  G1 & G2 & US --> HIS[("grade_history<br/>mọi lần lưu điểm")]
```

## H1. Trạng thái chấm điểm (tính động)

```mermaid
flowchart TB
  IN["GradeService.gradeState(reg, now)"] --> W{"trạng thái đợt"}
  W -->|"IN_PROGRESS"| OPEN["OPEN — chấm / sửa tự do"]
  W -->|"CLOSED"| A{"có yêu cầu APPROVED<br/>và unlockUntil > now?"}
  A -->|"có"| UNL["UNLOCKED"]
  A -->|"không"| LCK["LOCKED"]
  W -->|"khác"| NG["NOT_GRADING — chưa đến giai đoạn chấm"]
  OPEN & UNL & LCK & NG --> DTO["TopicRegistrationDTO.gradeState<br/>(+ scoreRevisedAt; với người xét: scoreRevisionCount, latestRevision)"]
  DTO --> UI["RegistrationReviewModal bật/tắt ô điểm, nút 'Yêu cầu chấm lại'"]
```

## H2 · H3. Chấm / sửa điểm tổng kết

```mermaid
sequenceDiagram
  autonumber
  actor GV as Giảng viên hướng dẫn
  participant FE as RegistrationReviewModal
  participant S as GradeService
  participant DB as MySQL
  participant N as NotificationService
  participant M as NotificationMailer (GRADE)
  GV->>FE: điểm, nhận xét, lý do (khi sửa)
  FE->>S: POST /api/registration/grade/{regId} {score, feedback, reason}
  S->>DB: SELECT registration FOR UPDATE
  S->>S: người gọi là GV hướng dẫn · đăng ký đã duyệt
  S->>S: parseScore: số (nhận dấu phẩy), 0–10, bội số 0,25 (7,25 · 7,5 · 7,75)
  alt OPEN
    S->>S: đã có điểm mà thiếu lý do → 400
  else UNLOCKED
    S->>S: bắt buộc lý do, lấy yêu cầu đang mở khóa
  else LOCKED
    S-->>FE: 400 GRADE_LOCKED "hãy gửi yêu cầu chấm lại"
  else NOT_GRADING
    S-->>FE: 400 WORKSPACE_PHASE
  end
  S->>DB: INSERT grade_history (old/new score, old/new feedback, changedBy, reason, revisionRequest)
  S->>DB: registration.score, feedback
  opt dùng yêu cầu chấm lại
    S->>DB: revision.status = USED (mỗi lần duyệt chỉ sửa 1 lần)
  end
  opt mọi SV đã duyệt của đề tài đều có điểm
    S->>DB: topic.status = COMPLETED
  end
  alt chấm lần đầu
    S->>N: SV "Đề tài đã có điểm tổng kết" · audit GRADE_TOPIC
    S->>M: email "graded"
  else sửa điểm
    S->>N: SV "Điểm đã được cập nhật cũ → mới" · audit GRADE_CHANGED (+ yêu cầu, lý do)
    S->>M: email "gradeChanged"
  end
  S-->>FE: DTO đã describe
```

## H4. Giảng viên gửi yêu cầu chấm lại

```mermaid
sequenceDiagram
  autonumber
  actor GV as Giảng viên
  participant S as GradeService
  participant U as UserRepository
  participant N as NotificationService
  participant M as NotificationMailer
  GV->>S: POST /api/registration/{regId}/grade-revisions {reason}
  S->>S: khóa dòng đăng ký · là GV hướng dẫn
  S->>S: UNLOCKED → 400 "hãy sửa điểm trước"
  S->>S: khác LOCKED → 400 "chỉ khi đợt đã đóng"
  S->>S: chưa có điểm → 400 · đã có PENDING → 400
  S->>S: INSERT grade_revision_requests status = PENDING
  S->>U: mọi DEPARTMENT_ADMIN của khoa chứa đợt
  loop từng quản trị khoa
    S->>N: "Yêu cầu chấm lại điểm" → /department-admin?tab=grade-revisions
    S->>M: email gradeRevisionRequested
  end
  S->>S: audit GRADE_REVISION_REQUESTED
```

## H5. Khoa / ADMIN duyệt hoặc từ chối

```mermaid
sequenceDiagram
  autonumber
  actor K as DEPT_ADMIN / ADMIN
  participant FE as GradeRevisionPanel
  participant S as GradeService
  participant N as NotificationService
  participant M as NotificationMailer
  FE->>S: GET /api/grade-revisions?status&page&size<br/>(DEPT_ADMIN: chỉ khoa mình)
  K->>FE: xem lịch sử điểm (GradeHistoryModal) rồi quyết định
  alt Duyệt
    FE->>S: POST /grade-revisions/{id}/approve {unlockHours 1–168 (mặc định 72), note}
  else Từ chối
    FE->>S: POST /grade-revisions/{id}/reject {note bắt buộc}
  end
  S->>S: quyền khoa của đợt · yêu cầu còn PENDING
  S->>S: KHÔNG được tự duyệt yêu cầu của chính mình (403)
  S->>S: APPROVED + unlockUntil = now + giờ · hoặc REJECTED
  S->>S: reviewedBy, reviewedAt
  S->>N: GV "Yêu cầu chấm lại đã được duyệt — sửa được đến HH:mm dd/MM/yyyy" / "bị từ chối"
  S->>M: email gradeRevisionReviewed
  S->>S: audit GRADE_REVISION_APPROVED / REJECTED
```

## H6. Tự khóa lại khi hết hạn mở khóa

```mermaid
sequenceDiagram
  participant J as GradeRevisionScheduler (cron 0 5 * * * *)
  participant S as GradeService.expireOverdue
  participant N as NotificationService
  J->>S: mỗi giờ, phút thứ 5
  S->>S: tìm APPROVED có unlockUntil < now
  loop từng yêu cầu
    S->>S: status = EXPIRED
    S->>N: GV "Hết hạn mở khóa điểm — điểm được khóa lại"
    S->>S: audit GRADE_REVISION_EXPIRED
  end
```

## H7. Lịch sử điểm

```mermaid
flowchart LR
  B["GradeHistoryModal"] -->|"GET /api/registration/{regId}/grade-history"| S["GradeService.history"]
  S --> P{"vai trò"}
  P -->|"LECTURER"| P1["phải là GV hướng dẫn"]
  P -->|"DEPT_ADMIN / ADMIN"| P2["requireDepartmentAccess khoa của đợt"]
  P1 & P2 --> L["grade_history mới nhất trước:<br/>thời điểm · người sửa · điểm cũ → mới · nhận xét · lý do · yêu cầu chấm lại liên quan"]
```
Nhật ký kiểm toán nhóm `GRADE_*` **không bị xóa** bởi chính sách lưu trữ 12 tháng (K4).

## H8. Xuất Excel danh sách đăng ký & điểm

```mermaid
flowchart LR
  BTN["LecturerRegistrationPage → Xuất Excel"] -->|"GET /api/export/excel?topicId (blob)"| E["ExportService.registrationsWorkbook"]
  E --> Q{"topicId?"}
  Q -->|"bỏ trống"| A{"ADMIN?"}
  A -->|"có"| ALL["toàn bộ đăng ký hệ thống"]
  A -->|"không"| X["400 'Vui lòng chọn đề tài'"]
  Q -->|"có"| OWN["requireOwner(topic) → đăng ký của đề tài"]
  ALL & OWN --> XL["Sheet Registrations (Apache POI):<br/>ID · Đề tài · Sinh viên · Mã SV · Giảng viên ·<br/>Trạng thái · Điểm · Nhận xét · Đã chấm lại (số lần)"]
```
