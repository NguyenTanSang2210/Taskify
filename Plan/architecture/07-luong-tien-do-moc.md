# 07 — Luồng F · G: Tiến độ, File, Mốc tiến độ

[← Mục lục](README.md)

Thành phần chính: `StudentProgressPage` (Kanban) · `StudentProgressDetailPage` (nộp theo mốc) · `LecturerProgressPage` · `ProjectSpacePage` (`MilestonePanel`, `MilestoneForm`, `MilestoneGradeGrid`, `StudentMilestoneList`, `ProgressSubmitPanel`, `ProgressHistoryPanel`) · `FileDownloadButton` · `FileUploadController` · `FileStorageService` · `FileContentValidator` · `FileDownloadService` · `ProgressService` · `SimilarityService` · `MilestoneService` · `MilestoneGradeService`.

---

# F. Tiến độ & file

## F1. Tải file lên

```mermaid
flowchart TB
  U["fileApi.upload(file) — multipart"] --> C["POST /api/upload (isAuthenticated)"]
  C --> RL["RateLimiter: 30 lần / 10 phút / NGƯỜI DÙNG<br/>(không theo IP — phòng máy chung NAT)"]
  RL --> S1{"file rỗng?"}
  S1 -->|"có"| E1["400"]
  S1 -->|"không"| S2{"> 20 MB (header) hoặc nội dung thực > 20 MB?"}
  S2 -->|"có"| E2["413 PAYLOAD_TOO_LARGE"]
  S2 -->|"không"| S3["sanitizeOriginalName:<br/>bỏ đường dẫn, ký tự điều khiển, ký tự cấm trong tên file,<br/>dấu chấm đầu, cắt ≤ 200 ký tự"]
  S3 --> S4{"đuôi thuộc 13 loại?<br/>pdf doc docx xls xlsx ppt pptx zip rar png jpg jpeg txt"}
  S4 -->|"không"| E3["400 định dạng không hỗ trợ"]
  S4 -->|"có"| S5{"magic bytes khớp đuôi?<br/>%PDF- · OLE2 (doc/xls/ppt) · PK 03 04 (docx/xlsx/pptx/zip) · Rar! · PNG · FF D8 FF · txt = UTF-8 không có byte 0"}
  S5 -->|"không"| E4["400 'Nội dung file không khớp với đuôi'"]
  S5 -->|"có"| S6["ghi uploads/{uuid}.{đuôi}<br/>(kiểm tra đường dẫn vẫn nằm trong thư mục gốc)"]
  S6 --> S7["INSERT stored_files<br/>ownerId = người tải, refType = null (chưa gắn)"]
  S7 --> OUT["{fileId, fileName, originalName, fileUrl: /api/secure/files/{uuid.ext}}"]
  S7 -. "chưa gắn vào báo cáo sau 24 giờ" .-> PURGE["StoredFileMaintenance.purgeOrphans (03:30) xóa file + hồ sơ"]
```

## F2. Tải file xuống có kiểm quyền

```mermaid
sequenceDiagram
  autonumber
  actor U as Người dùng
  participant B as FileDownloadButton → api/download.js
  participant C as SecureFileController
  participant S as FileDownloadService
  participant T as TopicAccessService
  U->>B: bấm tải
  B->>C: GET /api/secure/files/{name} (axios blob + Bearer)
  C->>S: open(name)
  S->>S: tìm stored_files theo storedName (không có → 404)
  alt ADMIN hoặc chủ file
    S-->>C: cho phép
  else file gắn PROGRESS_REPORT
    S->>T: isOwner(report.topic, me) — GV hướng dẫn / khoa cùng khoa
    T-->>S: true / false
  else
    S-->>C: 403 (SV KHÔNG tải được file của SV khác)
  end
  S->>S: resolve trong thư mục upload, đọc được?
  C-->>B: file + Content-Disposition attachment, filename = tên gốc (UTF-8)
  B->>U: lưu file (saveBlob, tên lấy từ header)
```

## F3. Kiểm tra trùng lặp nội dung báo cáo

```mermaid
flowchart LR
  IN["nội dung báo cáo mới"] --> N["chuẩn hóa NFC, chữ thường (vi)<br/>bỏ dấu câu, giữ mọi chữ Unicode"]
  N --> TK["tách tiếng, bỏ hư từ<br/>(và, của, là, các, the, of ...)"]
  TK --> TF["vector tần suất:<br/>từng tiếng + cặp tiếng liền nhau (hệ thống, cơ sở)"]
  CAND["ứng viên: MỌI báo cáo trong cùng đợt<br/>(của chính SV và SV khác)"] --> TF2["vector từng ứng viên"]
  TF --> COS["cosine similarity"]
  TF2 --> COS
  COS --> BEST["báo cáo giống nhất"]
  BEST --> SAVE["lưu similarityScore (3 chữ số) + similarToReportId"]
  BEST -->|"≥ 0,8"| ALERT["audit SIMILARITY_ALERT 'Giống N% với báo cáo #id'"]
  SAVE --> VIEW["GV / khoa thấy điểm trùng (ProgressReportDTO.forReviewer)<br/>SV không thấy"]
```

## F4. Sinh viên nộp báo cáo tiến độ

```mermaid
sequenceDiagram
  autonumber
  actor SV as Sinh viên
  participant FE as StudentProgressPage / ProjectSpacePage
  participant UP as /api/upload
  participant S as ProgressService
  participant DB as MySQL
  participant SIM as SimilarityService
  participant EV as EventPublisher
  participant N as NotificationService
  participant M as NotificationMailer
  FE->>FE: lấy đề tài đã duyệt (registration/mine)
  SV->>FE: tiêu đề (≤ 200), nội dung (≤ 20.000), file (tùy chọn)
  opt có file
    FE->>UP: upload → fileUrl
  end
  FE->>S: POST /api/progress/create?studentId&topicId {title, content, fileUrl, deadline, milestoneId}
  S->>S: studentId == mình · có đăng ký ĐÃ DUYỆT với đề tài
  S->>S: đợt IN_PROGRESS, nếu không → WORKSPACE_PHASE
  opt fileUrl
    S->>DB: stored_files: phải của chính SV (403) và chưa gắn báo cáo khác (409)
  end
  opt milestoneId
    S->>DB: mốc tồn tại (404) và thuộc đúng đề tài (400)
  end
  S->>SIM: mostSimilar(content, ứng viên trong đợt)
  S->>DB: INSERT progress_report status = TODO, similarityScore, similarToReportId
  S->>DB: claim file (UPDATE ... WHERE refType IS NULL) — 0 dòng → 409, rollback toàn bộ
  S->>S: audit CREATE_PROGRESS (+ SIMILARITY_ALERT nếu ≥ 80 %)
  S->>EV: PROGRESS_CREATED → /topic/progress/{topicId}
  S->>N: GV: "Sinh viên gửi báo cáo" → /lecturer-progress?topicId#pr-id
  S->>M: email GV (nhóm PROGRESS)
  S->>N: SV: "Đã gửi báo cáo" → /progress?topicId#pr-id
  S-->>FE: ProgressReportDTO
```

## F5. Kanban — sinh viên đổi trạng thái

```mermaid
sequenceDiagram
  actor SV as Sinh viên
  participant K as KanbanColumn / KanbanCard
  participant S as ProgressService
  participant WS as /topic/progress/{topicId}
  K->>K: hiển thị 3 cột TODO · IN_PROGRESS · DONE (kanbanConfig), % hoàn thành = DONE / tổng
  SV->>K: kéo thẻ sang cột khác (hoặc chọn ở chế độ bảng ProgressTable)
  K->>S: PATCH /api/progress/{id}/status {status}
  S->>S: chỉ SV sở hữu báo cáo · ProgressStatus.parse
  S->>S: lưu · audit UPDATE_PROGRESS_STATUS
  S-->>WS: PROGRESS_UPDATED
  WS-->>K: các tab / GV đang mở đề tài tải lại
```
> Ghi chú hiện trạng: đổi trạng thái Kanban **không kiểm tra giai đoạn đợt** (khác với nộp báo cáo và nhận xét, vốn yêu cầu `IN_PROGRESS`).

## F6. Giảng viên nhận xét báo cáo

```mermaid
sequenceDiagram
  autonumber
  actor GV as Giảng viên
  participant FE as LecturerProgressPage / ProgressHistoryPanel
  participant S as ProgressService
  participant N as NotificationService
  participant M as NotificationMailer
  FE->>S: GET /api/topics?lecturerId=me → chọn đề tài
  FE->>S: GET /api/progress/topic/{topicId}?page&size (forReviewer: có điểm trùng)
  FE->>FE: subscribe /topic/progress/{topicId}
  GV->>FE: nhận xét (≤ 5000), trạng thái, hạn
  FE->>S: PUT /api/progress/{id}/comment
  S->>S: GV hướng dẫn đề tài (hoặc ADMIN) · đợt IN_PROGRESS
  S->>S: cập nhật status / lecturerComment / deadline<br/>commentedAt = lần nhận xét đầu tiên (dùng đo thời gian phản hồi)
  S->>S: audit UPDATE_PROGRESS · PROGRESS_UPDATED
  S->>N: SV "Giảng viên nhận xét báo cáo #id"
  S->>M: email SV (nhóm PROGRESS)
```

## F7. Ai xem được tiến độ của ai

```mermaid
flowchart LR
  A["GET /progress/student/{studentId}?topicId"] --> R{"vai trò"}
  R -->|"STUDENT"| R1["chỉ studentId == mình"]
  R -->|"DEPT_ADMIN"| R2["SV thuộc khoa mình"]
  R -->|"LECTURER"| R3["bắt buộc topicId, là GV hướng dẫn đề tài"]
  R -->|"ADMIN"| R4["mọi SV"]
  R1 --> V1["ProgressReportDTO.from (không có điểm trùng)"]
  R2 --> V2["ProgressReportDTO.forReviewer"]
  R3 --> V2
  R4 --> V2
  B["GET /progress/topic/{topicId} (phân trang)"] --> B1["ADMIN · DEPT_ADMIN cùng khoa · GV hướng dẫn"] --> V2
```

---

# G. Mốc tiến độ (milestone)

## G1. Tạo / sửa / xóa mốc

```mermaid
sequenceDiagram
  autonumber
  actor GV as Giảng viên
  participant FE as MilestonePanel + MilestoneForm (ProjectSpacePage)
  participant S as MilestoneService
  participant T as TopicAccessService
  GV->>FE: tên (≤ 255), mô tả (≤ 5000), bắt đầu, hạn chót*, trọng số 0–100
  FE->>S: POST /api/milestones
  S->>T: requireOwner(topic)
  S->>S: hạn chót ≥ bắt đầu · cả hai nằm trong [startAt, endAt] của đợt (nếu đợt có ngày)
  S->>S: audit CREATE_MILESTONE
  GV->>FE: sửa → PUT /api/milestones/{id} (chỉ trường gửi lên) · UPDATE_MILESTONE
  GV->>FE: xóa → DELETE /api/milestones/{id}
  alt mốc đã có bài nộp
    S-->>FE: 400 "đã có bài nộp — hãy sửa tên/hạn thay vì xóa"
  else
    S->>S: xóa · DELETE_MILESTONE
  end
```
Mốc cũng xuất hiện trong lịch đề tài (`CalendarService.list` gộp sự kiện + mốc) và trong nhắc hạn tự động (K3).

## G2. Sinh viên nộp bài theo mốc

```mermaid
sequenceDiagram
  actor SV as Sinh viên
  participant L as StudentMilestoneList (ProjectSpacePage)
  participant D as StudentProgressDetailPage
  participant S as ProgressService
  SV->>L: chọn mốc
  L->>D: /progress-detail?milestoneId
  D->>D: GET /api/milestones/{id} (requireMember) → hiện hạn, đếm ngược thời gian còn lại
  SV->>D: tiêu đề, nội dung, file
  D->>D: tự upload file nếu chưa upload
  D->>S: POST /api/progress/create {..., milestoneId, deadline = hạn mốc}
  S->>S: như F4 + mốc phải thuộc đề tài
  D->>SV: thành công → quay về /progress
```

## G3. Bảng điểm mốc (board)

```mermaid
flowchart TB
  Q["GET /api/milestones/board?topicId"] --> M["requireMember(topic)"]
  M --> W{"là owner?"}
  W -->|"có (GV / khoa / ADMIN)"| SA["mọi SV đã duyệt, sắp theo mã SV"]
  W -->|"không (SV)"| SS["chỉ dòng của chính mình"]
  SA --> D
  SS --> D
  D["1 truy vấn bài nộp theo (mốc, SV):<br/>lần nộp ĐẦU (quyết định đúng/trễ hạn) + báo cáo MỚI NHẤT (để mở xem)"]
  D --> G["1 truy vấn điểm mốc của đề tài"]
  G --> CELL["Ô (mốc × SV): reportId mới nhất, firstSubmittedAt,<br/>late = lần đầu > hạn chót, score, feedback"]
  CELL --> SUG["Điểm gợi ý = Σ(điểm × trọng số) / Σ trọng số đã chấm<br/>(làm tròn 2 chữ số) · complete = mọi mốc có trọng số đã chấm"]
  SUG --> OUT["MilestoneBoardDTO {milestones, weightTotal,<br/>canGrade = owner và đợt IN_PROGRESS, rows}"]
  OUT --> UI["MilestoneGradeGrid (GV) / StudentMilestoneList (SV)"]
```
Điểm gợi ý **chỉ để tham khảo** — GV vẫn tự nhập điểm tổng kết (H2).

## G4. Chấm điểm mốc

```mermaid
sequenceDiagram
  autonumber
  actor GV as Giảng viên
  participant FE as MilestoneGradeGrid
  participant S as MilestoneGradeService
  participant N as NotificationService
  GV->>FE: nhập điểm 0–10 + nhận xét (≤ 5000) cho ô (mốc, SV)
  FE->>S: PUT /api/milestones/{id}/grades/{studentId}
  S->>S: requireOwner(topic) · đợt IN_PROGRESS (WORKSPACE_PHASE)
  S->>S: SV phải có đăng ký đã duyệt trong đề tài (INVALID_INPUT)
  S->>S: upsert milestone_grades (gradedBy, gradedAt)
  S->>S: audit GRADE_MILESTONE "score=cũ -> mới"
  S->>N: SV "Đã có điểm mốc ..." → /project-space
  S-->>FE: Cell mới → bảng tính lại điểm gợi ý
```
