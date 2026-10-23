# ADR-0006: Tên chính thức là Taskify

- **Trạng thái:** Đã chấp nhận
- **Ngày:** 25/09/2026 (thực hiện P3-12)
- **Mã trong kế hoạch:** D-06 (`Plan/plan1.md`)

## Bối cảnh

Repo dùng ba tên: "KTPM" (README, container), "DoAn_LTMMT" (pom, DB, package Java), "Taskify" (giao diện).

## Quyết định

**Taskify** cho mọi thứ người dùng và người vận hành nhìn thấy: README, tài liệu, container `taskify-*`, `artifactId`, tiêu đề Swagger.

Giữ nguyên tên DB `doan_ltmmt`, user MySQL `ktpm` và package Java `com.doanltmmt.Backend`: đổi chúng phải di chuyển dữ liệu/volume đang chạy và sửa toàn bộ mã, rủi ro cao mà lợi ích thấp.

## Hệ quả

- Lệnh sao lưu/khôi phục dùng container `taskify-db` nhưng vẫn là database `doan_ltmmt`.
