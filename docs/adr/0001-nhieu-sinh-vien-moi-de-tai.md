# ADR-0001: Một đề tài nhận nhiều sinh viên độc lập theo capacity

- **Trạng thái:** Đã chấp nhận
- **Ngày:** 25/09/2026
- **Mã trong kế hoạch:** D-01 (`Plan/plan1.md`)

## Bối cảnh

Mô hình cũ giả định mỗi đề tài một sinh viên nhưng entity đã có `capacity`; khoa cần cho nhiều sinh viên cùng làm một đề tài.

## Quyết định

Các phương án: (A) giữ 1 SV/đề tài, bỏ `capacity`; (B) nhiều SV độc lập theo `capacity`, chấm riêng từng SV; (C) mô hình Nhóm có trưởng nhóm.

**Chọn B.** Mỗi `topic_registration` là một sinh viên, được duyệt/chấm điểm riêng; đề tài chuyển `OPEN ↔ FULL` theo số SV đã duyệt so với `capacity` (`Topic.refreshCapacityStatus`).

## Hệ quả

- Đơn giản hơn mô hình nhóm, không cần vai trò trưởng nhóm hay điểm nhóm.
- Thiết kế vẫn tương thích để thêm Team sau v1.0 (P4-04, 🕒): khi đó đăng ký có thể trỏ tới nhóm thay vì từng SV.
- Đếm số SV đã duyệt phải chính xác dưới tải đồng thời (khóa hàng khi duyệt).
