# Định dạng lỗi API và mã lỗi (P3-03)

Mọi lỗi của API `/api/**` trả về cùng một định dạng JSON. Các lỗi do controller/service ném, lỗi của Spring MVC, lỗi 401/403 ở filter bảo mật và lỗi rơi xuống `/error` đều dùng chung định dạng này:

```json
{
  "timestamp": "2026-10-23T14:08:26.072+07:00",
  "status": 400,
  "code": "VALIDATION_FAILED",
  "error": "Bad Request",
  "message": "không được để trống",
  "path": "/api/auth/login",
  "errors": [
    { "field": "username", "message": "không được để trống" },
    { "field": "password", "message": "không được để trống" }
  ]
}
```

- `message` là câu tiếng Việt, đủ để hiển thị thẳng cho người dùng.
- `code` là mã ổn định. Frontend nên rẽ nhánh theo `code`, không theo nội dung `message`.
- `errors` chỉ có khi dữ liệu không qua kiểm tra (`VALIDATION_FAILED`).
- Body lỗi **không bao giờ** chứa stacktrace, câu SQL hay thông điệp nội bộ của Java. Các chi tiết đó chỉ ghi vào log.

Ở frontend, `src/api.js` gắn các trường `status`, `code`, `fieldErrors` (`{field: message}`) và `data` vào lỗi được reject.

## Mã chung (theo HTTP status)

| code | HTTP | Khi nào |
|---|---|---|
| `BAD_REQUEST` | 400 | Lỗi 400 không mang mã riêng |
| `UNAUTHORIZED` | 401 | Chưa đăng nhập, token hết hạn hoặc bị thu hồi |
| `FORBIDDEN` | 403 | Đã đăng nhập nhưng không đủ quyền hoặc ngoài phạm vi |
| `NOT_FOUND` | 404 | Không tìm thấy dữ liệu hoặc đường dẫn |
| `METHOD_NOT_ALLOWED` | 405 | Sai phương thức HTTP |
| `CONFLICT` | 409 | Xung đột không mang mã riêng |
| `PAYLOAD_TOO_LARGE` | 413 | File vượt 20 MB |
| `UNSUPPORTED_MEDIA_TYPE` | 415 | Sai Content-Type |
| `TOO_MANY_REQUESTS` | 429 | Vượt giới hạn tần suất (có header `Retry-After`) |
| `INTERNAL_ERROR` | 500 | Lỗi hệ thống, thông báo chung |

## Mã dữ liệu đầu vào

| code | HTTP | Khi nào |
|---|---|---|
| `VALIDATION_FAILED` | 400 | Bean Validation (`@Valid`) thất bại, kèm `errors[]` |
| `MALFORMED_REQUEST` | 400 | JSON hỏng, thiếu tham số bắt buộc, sai kiểu tham số (vd. `/users/abc`) |
| `INVALID_INPUT` | 400 | Dữ liệu sai nghiệp vụ cơ bản (ngày sai định dạng, số âm, sai kiểu trong body…) |
| `DATA_CONFLICT` | 409 | Vi phạm ràng buộc CSDL (trùng khóa duy nhất…) |

## Mã nghiệp vụ

| code | HTTP | Khi nào |
|---|---|---|
| `INVALID_TRANSITION` | 400 | Chuyển trạng thái đợt không hợp lệ |
| `WORKSPACE_PHASE` | 400 | Thao tác không được phép ở giai đoạn hiện tại của đợt (đăng ký, duyệt, báo cáo, chấm điểm, cấu hình) |
| `TOPIC_FULL` | 400 | Đề tài đã đủ số sinh viên |
| `DUPLICATE_REGISTRATION` | 400 | Sinh viên đã có hoặc đang chờ duyệt một đăng ký trong đợt |
| `GRADE_LOCKED` | 400 | Điểm đã khóa, phải gửi yêu cầu chấm lại |

## Thêm mã mới

1. Khai báo hằng số trong `exception/ErrorCodes.java` và bổ sung bảng trên.
2. Ném lỗi bằng `ApiException.badRequest(ErrorCodes.X, "thông báo tiếng Việt")`. Các factory khác: `notFound`, `forbidden`, `conflict`.
3. Viết test kiểm tra `$.code` (xem `ErrorFormatIT`).
