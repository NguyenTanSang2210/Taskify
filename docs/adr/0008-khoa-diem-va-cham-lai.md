# ADR-0008: Khóa điểm khi đợt kết thúc, mở khóa qua yêu cầu chấm lại

- **Trạng thái:** Đã chấp nhận
- **Ngày:** 25/09/2026 (thực hiện P2-13)
- **Mã trong kế hoạch:** D-08 (`Plan/plan1.md`)

## Bối cảnh

Giảng viên sửa được điểm bất kỳ lúc nào, kể cả khi đợt đã kết thúc và điểm đã công bố.

## Quyết định

**Không** cho chấm/sửa điểm khi workspace `CLOSED`. Khi cần sửa: giảng viên gửi yêu cầu chấm lại kèm lý do → khoa/Admin duyệt (không tự duyệt yêu cầu của mình) → điểm mở khóa tới `unlockUntil` cho **một** lần sửa → tự khóa lại. Mọi thay đổi điểm ghi `grade_history`.

## Hệ quả

- Thêm bảng `grade_revision_requests`, `grade_history`; scheduler chuyển yêu cầu quá hạn sang `EXPIRED`.
- Bản tối thiểu; đơn phúc khảo do sinh viên gửi để sau (rủi ro R-11).
