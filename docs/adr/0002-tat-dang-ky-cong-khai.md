# ADR-0002: Tắt đăng ký tài khoản công khai

- **Trạng thái:** Đã chấp nhận
- **Ngày:** 25/09/2026
- **Mã trong kế hoạch:** D-02 (`Plan/plan1.md`)

## Bối cảnh

Endpoint `/api/auth/register` cho bất kỳ ai tạo tài khoản sinh viên, kể cả người ngoài trường; không xác minh được mã sinh viên.

## Quyết định

**Tắt đăng ký công khai.** Tài khoản do Admin (hoặc khoa) tạo, sau này import từ Excel (P4-03). Người dùng kích hoạt bằng mã gửi qua email rồi tự đặt mật khẩu theo chính sách mật khẩu.

## Hệ quả

- Xóa endpoint và form đăng ký; thêm luồng kích hoạt tài khoản (P1-09).
- Nhập tay nhiều sinh viên tốn thời gian cho tới khi có import Excel — P4-03 được nâng ưu tiên (rủi ro R-10).
