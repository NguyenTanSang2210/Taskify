# ADR-0007: Xóa tính năng ProjectPermission

- **Trạng thái:** Đã chấp nhận
- **Ngày:** 25/09/2026 (thực hiện P2-11)
- **Mã trong kế hoạch:** D-07 (`Plan/plan1.md`)

## Bối cảnh

Entity `ProjectPermission` + API `/api/permissions` cho cấu hình quyền theo công cụ (MILESTONE, SUBMISSION, DISCUSSION…) nhưng không nơi nào trong mã áp dụng các quyền này và frontend không có giao diện — tạo cảm giác có kiểm soát truy cập trong khi không có.

## Quyết định

**Xóa** entity, bảng và API `/api/permissions`. Quyền theo đề tài được kiểm tra thống nhất bởi `TopicAccessService` (giảng viên hướng dẫn / sinh viên đã duyệt / khoa).

## Hệ quả

- Ít mã chết hơn; nếu sau này cần quyền chi tiết theo dự án thì thiết kế lại từ đầu trên `TopicAccessService`.
