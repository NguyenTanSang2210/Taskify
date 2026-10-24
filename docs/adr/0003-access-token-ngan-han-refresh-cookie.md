# ADR-0003: Access token 15 phút trong bộ nhớ + refresh token cookie HttpOnly

- **Trạng thái:** Đã chấp nhận
- **Ngày:** 25/09/2026 (triển khai 23/10/2026, P3-14)
- **Mã trong kế hoạch:** D-03 (`Plan/plan1.md`)

## Bối cảnh

JWT sống 24 giờ và nằm trong `localStorage`: một lỗ hổng XSS là đủ lấy token dùng trong cả ngày; không có cách thu hồi một phiên cụ thể.

## Quyết định

Các phương án: (A) giữ `localStorage`, giảm thời hạn; (B) access token ngắn hạn trong bộ nhớ + refresh token trong cookie `HttpOnly`.

**Chọn B.**
- Access token 15 phút, chỉ nằm trong bộ nhớ của trang.
- Refresh token 256 bit ngẫu nhiên, chỉ lưu hash SHA-256 trong bảng `refresh_tokens`; cookie `HttpOnly; SameSite=Strict; Path=/api/auth; Secure` (prod), sống 7 ngày, xoay vòng mỗi lần dùng.
- Dùng lại token đã xoay vòng ⇒ coi như bị đánh cắp: thu hồi mọi phiên và tăng `tokenVersion`. Có 30 giây ân hạn để hai tab làm mới cùng lúc không bị đăng xuất nhầm.

## Hệ quả

- JavaScript không đọc được refresh token; access token bị lộ chỉ dùng được tối đa 15 phút.
- Tải lại trang phải gọi `/api/auth/refresh` trước khi hiển thị (màn hình chờ ngắn).
- Cookie `Secure` buộc production chạy HTTPS (`APP_AUTH_REFRESH_COOKIE_SECURE=false` chỉ dành cho môi trường thử qua HTTP).
- Endpoint dùng cookie cần chống CSRF: `SameSite=Strict` + kiểm tra header `Origin`.
