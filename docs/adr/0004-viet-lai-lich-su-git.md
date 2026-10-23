# ADR-0004: Xóa file cá nhân khỏi lịch sử git

- **Trạng thái:** Đã chấp nhận
- **Ngày:** 25/09/2026 (thực hiện 22/10/2026, P1-08)
- **Mã trong kế hoạch:** D-04 (`Plan/plan1.md`)

## Bối cảnh

Thư mục `uploads/` chứa file thật của người dùng đã bị commit; gỡ ở phiên bản hiện tại thì file vẫn còn trong lịch sử và ai clone repo cũng lấy được.

## Quyết định

Các phương án: (A) chỉ gỡ khỏi phiên bản hiện tại; (B) viết lại lịch sử bằng `git filter-repo` và force push.

**Chọn B**, kèm quy trình an toàn: push hết commit trước, `git clone --mirror` làm bản sao lưu, kiểm chứng sau khi viết lại, rồi mọi người clone lại.

## Hệ quả

- Mọi bản clone cũ phải bỏ đi; hash commit trước thời điểm viết lại thay đổi.
- `uploads/` nằm trong `.gitignore`; quy ước từ nay **không viết lại lịch sử** nữa (chỉ commit/merge).
