# ADR-0009: Flyway quản lý schema thay cho ddl-auto

- **Trạng thái:** Đã chấp nhận
- **Ngày:** 23/10/2026
- **Mã trong kế hoạch:** P3-01 (`Plan/plan1.md`)

## Bối cảnh

`spring.jpa.hibernate.ddl-auto=update` thay đổi schema ngầm mỗi lần khởi động, không có lịch sử và không rollback được; `SchemaFixer` chạy `ALTER TABLE` ở mọi profile.

## Quyết định

Flyway quản lý toàn bộ schema; Hibernate chỉ `validate`.
- `V1__baseline_schema.sql` sinh từ schema Hibernate của mã GĐ2 trên DB trống, viết dạng *chỉ tạo/thêm khi còn thiếu* nên chạy được cả trên DB cũ; `baseline-on-migrate=true`, `baseline-version=0`.
- Dữ liệu mặc định (vai trò, quyền) và sửa dữ liệu cũng là migration (V2, V3) thay cho các runner Java chạy mỗi lần khởi động.
- Thay đổi schema từ nay là migration mới `V<n>__...`; **không sửa** migration đã phát hành.

## Hệ quả

- `FlywayMigrationIT` kiểm tra DB trống, DB cũ thiếu cột/bảng và việc bước dữ liệu không chạy lại.
- Trước khi nâng cấp production phải sao lưu DB (migration chạy tự động khi backend khởi động).
