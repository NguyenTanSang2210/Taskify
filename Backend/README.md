# Taskify — Backend

Dịch vụ Spring Boot 3.5 (Java 21) cung cấp REST API (`/api/**`), WebSocket STOMP (`/ws`), xác thực/phân quyền và toàn bộ nghiệp vụ. Kiến trúc tổng thể: [`../docs/ARCHITECTURE.md`](../docs/ARCHITECTURE.md).

## Cấu trúc gói (`com.doanltmmt.Backend`)

| Gói | Nội dung |
|---|---|
| `controller` | Endpoint REST — nhận record `dto/request` đã `@Valid`, gọi service, trả `dto/response`. Không dùng repository, < 120 dòng |
| `service` | Nghiệp vụ `@Transactional`; `SecurityScopeService`, `TopicAccessService` kiểm tra phạm vi dữ liệu; email, OTP, refresh token, audit |
| `repository`, `entity`, `entity/enums` | JPA; trạng thái là enum, quy tắc chuyển trạng thái trong enum |
| `security` | JWT, cookie refresh, rate limit, chống dò mật khẩu, chính sách mật khẩu, xác thực WebSocket |
| `config` | Security, CORS, WebSocket, Jackson, request id, bảo trì file, tạo Admin đầu tiên (`AdminBootstrap`) |
| `exception` | `GlobalExceptionHandler`, `ApiException`, `ErrorCodes` — định dạng lỗi ở [`../docs/error-codes.md`](../docs/error-codes.md) |
| `scheduler` | Nhắc hạn, hết hạn chấm lại |

Schema: `src/main/resources/db/migration` (Flyway, `ddl-auto=validate`). Thông báo validation: `src/main/resources/ValidationMessages.properties`.

## Cấu hình

`application.properties` (chung), `application-dev.properties`, `application-prod.properties`; giá trị nhạy cảm đọc từ biến môi trường — mẫu ở `.env.example` (chạy local) và `../.env.docker.example` (Docker). Biến chính:

| Biến | Ý nghĩa |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` (Swagger mở, cho phép seed demo) / `prod` (log JSON, kiểm tra secret, tắt Swagger) |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Kết nối MySQL |
| `JWT_SECRET` | Base64 của ≥ 32 byte ngẫu nhiên (bắt buộc) |
| `JWT_EXPIRATION`, `APP_AUTH_REFRESH_TTL_DAYS`, `APP_AUTH_REFRESH_COOKIE_SECURE` | Thời hạn access token (ms, mặc định 15 phút), refresh token (ngày), cờ `Secure` của cookie |
| `APP_CORS_ALLOWED_ORIGINS` | Origin của giao diện (phân tách bằng dấu phẩy) |
| `MAIL_*` | SMTP gửi OTP/thông báo |
| `APP_SEED_ENABLED`, `APP_SEED_OTP_EMAIL` | Dữ liệu demo (chỉ profile `dev`) |
| `APP_BOOTSTRAP_ADMIN_USERNAME`, `APP_BOOTSTRAP_ADMIN_EMAIL` | Tạo Admin đầu tiên khi chưa có (xem `../docs/DEPLOYMENT.md`) |
| `APP_RATE_*` | Giới hạn tần suất (xem `application.properties`) |

## Chạy và kiểm thử

```powershell
.\mvnw.cmd spring-boot:run          # http://localhost:8080 (Swagger: /swagger-ui.html ở profile dev)
.\mvnw.cmd verify                   # test + ngưỡng JaCoCo; báo cáo: target\site\jacoco\index.html
.\mvnw.cmd test -Dtest=RefreshTokenIT
```

Test tích hợp dùng MySQL thật: không đặt `DB_URL` thì Testcontainers tự chạy `mysql:8.4` (cần Docker); đặt `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` để dùng MySQL có sẵn.

Docker: `Dockerfile` ở thư mục này, được dùng bởi `../docker-compose.yml` (healthcheck `GET /actuator/health/readiness`).
