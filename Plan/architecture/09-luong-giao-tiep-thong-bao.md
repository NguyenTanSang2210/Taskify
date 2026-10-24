# 09 — Luồng I: Giao tiếp, thông báo, email

[← Mục lục](README.md)

Thành phần chính: `ProjectChatPage` (`ChatPeopleSidebar`, `ChatConversation`) · `NotificationsBell` · `NotificationsPage` · `NotificationSettingsTab` · `MessageService` · `NotificationService` · `NotificationMailer` · `EmailService` · `MailDispatcher` · `EmailTemplates` · `DiscussionService` · `AnnouncementService` · `CalendarService` · `EventPublisher`.

## I1. Tin nhắn 1–1 trong phạm vi đề tài

```mermaid
sequenceDiagram
  autonumber
  actor A as Người gửi (SV / GV)
  participant FE as ProjectChatPage
  participant S as MessageService
  participant T as TopicAccessService
  participant EV as EventPublisher
  actor B as Người nhận
  FE->>FE: SV: đề tài đã duyệt → người nhận = GV hướng dẫn
  FE->>FE: GV: chọn đề tài → người nhận = SV ĐÃ DUYỆT (registration/topic)
  FE->>FE: subscribe /user/queue/messages
  A->>FE: chọn người trong ChatPeopleSidebar
  FE->>S: GET /api/messages/conversation/{peerId}?topicId&page (mới nhất trước)
  S->>T: requireMember(topic) khi có topicId
  S-->>FE: trang tin nhắn → client đảo thứ tự hiển thị, "tải tin cũ hơn" = page + 1
  A->>FE: gõ tin (≤ 5000 ký tự)
  FE->>S: POST /api/messages/send {recipientId, topicId, content}
  S->>T: người gửi là thành viên đề tài
  S->>T: người NHẬN cũng là thành viên (isMember) — chặn nhắn người ngoài
  S->>S: lưu (sender, recipient, topic, workspace) · audit SEND_MESSAGE
  S->>EV: messageCreated (sau commit)
  EV-->>FE: MESSAGE_CREATED → /user/queue/messages (người gửi, các tab khác)
  EV-->>B: MESSAGE_CREATED → /user/queue/messages (người nhận)
```
`GET /api/messages/inbox` (danh sách hội thoại gần nhất) có API, giao diện hiện chưa dùng.

## I2. Thông báo trong ứng dụng

```mermaid
flowchart TB
  subgraph SRC["Nguồn tạo thông báo (NotificationService.notify / publish)"]
    S1["Đăng ký được duyệt / bị từ chối → SV"]
    S2["SV nộp báo cáo → GV và SV"]
    S3["GV nhận xét báo cáo → SV"]
    S4["Có điểm / điểm thay đổi → SV"]
    S5["Điểm mốc → SV"]
    S6["Yêu cầu chấm lại → quản trị khoa<br/>Kết quả xét / hết hạn → GV"]
    S7["Nhắc hạn tự động (REMINDER, WARNING / CRITICAL) → SV"]
    S8["Gửi thủ công (ADMIN / GV) ⚠️"]
  end
  SRC --> SAVE[("notifications<br/>type · severity · title · content · refType/refId · linkPath · dueAt · readAt")]
  SAVE --> EV["EventPublisher.notificationCreated (sau commit)"]
  EV --> Q["/user/queue/notifications"]
  Q --> BELL["NotificationsBell: chèn đầu danh sách (giữ 6), số chưa đọc + 1"]
  Q --> PAGE["NotificationsPage: chèn vào danh sách"]
  BELL -->|"bấm 1 thông báo"| RD["POST /notifications/read/{id} → điều hướng linkPath"]
  BELL -->|"đánh dấu tất cả"| RA["POST /notifications/read-all?userId"]
  BELL -->|"mount"| INIT["GET /notifications/recent?limit=6 + /count"]
  PAGE -->|"tải"| MINE["GET /notifications/mine?userId&page&size"]
```
Đọc thông báo: mọi vai trò chỉ đọc của **chính mình** (bỏ qua `userId` gửi lên); chỉ ADMIN xem được thông báo người khác. `markRead`: chỉ chủ thông báo hoặc ADMIN.

### I3. Gửi thông báo thủ công (API, chưa có UI)

```mermaid
flowchart LR
  P["POST /api/notifications/create?userId&title&content&type"] --> R{"vai trò"}
  R -->|"ADMIN"| OK["gửi cho bất kỳ ai"]
  R -->|"LECTURER"| C{"người nhận là SV đã đăng ký<br/>đề tài do GV này hướng dẫn?"}
  C -->|"không"| X["403"]
  C -->|"có"| OK
  OK --> PUB["publish → realtime · audit CREATE_NOTIFICATION"]
```

## I4 · I5 · I6. Email: gửi ngay, tổng hợp hằng ngày, hạ tầng gửi

```mermaid
sequenceDiagram
  autonumber
  participant BIZ as Service nghiệp vụ
  participant NM as NotificationMailer
  participant DB as notification_settings / email_digest_items
  participant ES as EmailService
  participant TX as Transaction
  participant MD as MailDispatcher (luồng mail-*)
  participant SMTP as SMTP
  BIZ->>NM: send(user, category, EmailTemplates.xxx)
  NM->>DB: đọc cài đặt người nhận
  alt không có email / nhóm bị tắt
    NM-->>BIZ: bỏ qua
  else chế độ tổng hợp
    NM->>DB: INSERT email_digest_items (subject, body dạng text)
  else gửi ngay
    NM->>ES: sendEmail(to, subject, html)
    ES->>ES: publishEvent(MailRequested)
  end
  TX-->>MD: AFTER_COMMIT → @Async(mailExecutor)
  MD->>MD: pace(): cách email trước ≥ 200 ms
  loop tối đa 3 lần
    MD->>SMTP: gửi MIME (HTML + plain text, From "Taskify")
    alt lỗi SMTP
      MD->>MD: chờ 2s × lần thử
    end
  end
  MD->>MD: taskify_mail_sent_total{result=success|failure}
  Note over NM,SMTP: 07:30 hằng ngày — sendDigests(): mỗi người có mục chờ nhận MỘT thư gộp, rồi xóa các mục đã gửi
```

Danh mục mẫu email (`EmailTemplates`):

| Nhóm | Mẫu | Người nhận |
|---|---|---|
| Bảo mật (luôn gửi) | `loginOtp`, `activation`, `resetPassword` | chính người dùng |
| `REGISTRATION` | `registrationApproved`, `registrationRejected` | SV |
| `PROGRESS` | `progressSubmitted` | GV |
| `PROGRESS` | `progressFeedback` | SV |
| `GRADE` | `graded`, `gradeChanged` | SV |
| `GRADE` | `gradeRevisionRequested` | quản trị khoa |
| `GRADE` | `gradeRevisionReviewed` | GV |
| `REMINDER` | `progressReminder`, `milestoneReminder` | SV |
| (tổng hợp) | `digest` | người bật chế độ tổng hợp |

## I7. Thảo luận nhóm theo chủ đề (API, chưa có UI)

```mermaid
sequenceDiagram
  actor U as Thành viên đề tài
  participant S as DiscussionService
  participant T as TopicAccessService
  U->>S: GET /api/discuss/threads?topicId
  S->>T: requireMember
  S-->>U: chủ đề mới nhất trước
  U->>S: POST /api/discuss/threads {topicId, title} → CREATE_THREAD
  U->>S: GET /api/discuss/threads/{id}/posts?page (cũ → mới, như cuộc trò chuyện)
  U->>S: POST /api/discuss/threads/{id}/posts {content} → CREATE_POST
```

## I8. Thông báo chung — announcements (API, chưa có UI)

```mermaid
flowchart LR
  L["GET /api/announcements?topicId"] --> Q{"có topicId?"}
  Q -->|"không"| SYS["thông báo toàn hệ thống (topic = null) — ai đăng nhập cũng xem"]
  Q -->|"có"| MEM["requireMember → thông báo của đề tài"]
  C["POST /api/announcements {title, content, topicId?}<br/>(LECTURER, ADMIN)"] --> Q2{"có topicId?"}
  Q2 -->|"có"| OWN["requireOwner(topic)"]
  Q2 -->|"không"| ADM{"ADMIN?"}
  ADM -->|"không"| X["403 chỉ quản trị hệ thống"]
  ADM -->|"có"| SAVE
  OWN --> SAVE["lưu · audit CREATE_ANNOUNCEMENT"]
```

## I9. Lịch đề tài (API, chưa có UI)

```mermaid
flowchart LR
  L["GET /api/calendar?topicId"] --> M["requireMember"]
  M --> MERGE["sự kiện (MEETING / MILESTONE / DEADLINE)<br/>+ mọi mốc của đề tài (CalendarEventDTO.fromMilestone)<br/>sắp theo thời gian bắt đầu"]
  C["POST /api/calendar {topicId, title, description, startTime, endTime, type}<br/>(LECTURER, ADMIN)"] --> O["requireOwner · type mặc định MEETING<br/>audit CREATE_CAL_EVENT"]
  D["DELETE /api/calendar/{id}"] --> O2["requireOwner · DELETE_CAL_EVENT"]
```
