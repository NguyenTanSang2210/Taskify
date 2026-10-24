# ADR-0010: Một định dạng lỗi cho toàn bộ API

- **Trạng thái:** Đã chấp nhận
- **Ngày:** 23/10/2026
- **Mã trong kế hoạch:** P3-03 (`Plan/plan1.md`)

## Bối cảnh

Mỗi controller trả lỗi một kiểu (chuỗi, `Map`, trang lỗi mặc định của Spring); một số lỗi lộ thông điệp Java/SQL; frontend phải đoán cấu trúc.

## Quyết định

Mọi lỗi `/api/**` — từ controller, Spring MVC, filter bảo mật và `/error` — trả cùng JSON `{timestamp, status, code, error, message, path, requestId, errors[]}`. `code` là mã ổn định (`ErrorCodes`) để frontend rẽ nhánh; `message` là tiếng Việt hiển thị thẳng cho người dùng. Một lớp `ApiException` với các factory thay cho nhiều lớp exception riêng.

## Hệ quả

- Danh sách mã và quy ước thêm mã: [`docs/error-codes.md`](../error-codes.md).
- Validation (P3-02) chạy trước kiểm tra quyền trong thân method: body sai luôn nhận 400, kể cả khi người gọi không có quyền.
