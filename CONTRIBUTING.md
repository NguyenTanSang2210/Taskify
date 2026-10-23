# Đóng góp cho Taskify

## 1. Nhánh và phát hành

```
develop  ← mọi thay đổi được commit/push ở đây; CI chạy trên mỗi push
   │  PR develop → main (chủ dự án review và merge)
   ▼
main     ← bản ổn định; gắn tag khi kết thúc một giai đoạn (v0.3.0-business, v0.4.0-hardening, …)
```

- Mỗi task trong [`Plan/plan1.md`](Plan/plan1.md) có mã (`P3-11`, `P4-03`…). Một task = một commit (task lớn có thể nhiều commit, mỗi commit vẫn build và test được).
- **Không viết lại lịch sử** đã push (`rebase`, `commit --amend`, `push --force`). Sửa lỗi bằng commit mới.
- Người đóng góp bên ngoài: fork, tạo nhánh từ `develop`, mở PR vào `develop`.
- Chỉ chủ dự án merge vào `main` và tạo tag.

## 2. Quy ước commit

```
[<mã task>] <mô tả ngắn bằng tiếng Việt có dấu>

<thân: vì sao thay đổi, những gì thay đổi, cách đã kiểm thử>
```

Ví dụ: `[P3-14] Access token 15 phút trong bộ nhớ + refresh token xoay vòng trong cookie HttpOnly`. Thay đổi không thuộc task nào: `docs: …`, `fix: …`, `chore: …`.

## 3. Chuẩn mã nguồn

**Backend** (xem [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)):
- Controller nhận record trong `dto/request` (có Bean Validation, thông báo tiếng Việt), gọi service, trả DTO. Không inject repository, tối đa 120 dòng — `LayeringTest` sẽ fail nếu vi phạm.
- Logic nghiệp vụ và `@Transactional` ở service; kiểm tra phạm vi dữ liệu qua `SecurityScopeService` / `TopicAccessService`.
- Lỗi: `throw ApiException.badRequest(ErrorCodes.X, "thông báo tiếng Việt")` — thêm mã mới vào `ErrorCodes` và [`docs/error-codes.md`](docs/error-codes.md).
- Trạng thái dùng enum trong `entity/enums`; quy tắc chuyển trạng thái đặt trong enum.
- Danh sách trả `PageResponse` (size tối đa 100). Endpoint danh sách mới cần có ngân sách truy vấn trong `QueryCountIT`.
- Schema: thêm `Backend/src/main/resources/db/migration/V<n>__mo_ta.sql`, **không sửa** migration đã phát hành.

**Frontend:**
- Gọi API qua `src/api/*Api.js`, không gọi `api.get/post` trong trang.
- Tải dữ liệu bằng `useCallback` + `useLoadEffect`; giá trị suy ra được từ state khác thì tính trong render, không đồng bộ bằng `useEffect`.
- Trang lớn tách hook dữ liệu + component con vào `pages/<trang>/`; giữ mỗi file dưới 400 dòng.
- Văn bản giao diện bằng tiếng Việt có dấu; hiển thị lỗi bằng `err.message` (đã là tiếng Việt từ API) và `err.fieldErrors` cho từng ô nhập.

## 4. Trước khi push (Definition of Done)

```powershell
cd Backend;  .\mvnw.cmd verify          # test + ngưỡng JaCoCo (Testcontainers cần Docker)
cd frontend; npm run check              # lint 0 cảnh báo + Vitest + build
```

- [ ] Hai lệnh trên đều đạt.
- [ ] Có test tự động cho thay đổi (bắt buộc với thay đổi bảo mật/nghiệp vụ: cả chiều "được phép" và "bị chặn").
- [ ] Đã thử luồng liên quan trên giao diện (stack Docker profile dev; `node scripts/smoke-test.mjs`, `node scripts/ui-check*.mjs`).
- [ ] Không commit secret, `.env`, file trong `uploads/`, file tạm của công cụ.
- [ ] Đã cập nhật tài liệu nếu đổi API, cấu hình hoặc biến môi trường (`.env.docker.example`, `Backend/.env.example`, `docs/`).
- [ ] Đã cập nhật trạng thái task + Changelog trong `Plan/plan1.md`.
- [ ] CI xanh trên `develop` sau khi push.

## 5. Mô tả PR `develop` → `main`

```markdown
## Nội dung
Các task: P3-01 … P3-14

## Thay đổi chính
- …

## Cách kiểm thử
- CI: …
- Thủ công: …

## Lưu ý khi triển khai
- Migration DB mới: V5, V6 (sao lưu trước khi nâng cấp)
- Biến môi trường mới / đổi giá trị mặc định: …
```

## 6. Báo lỗi

- Lỗi chức năng: mở issue, ghi các bước tái hiện, vai trò tài khoản, và `requestId` trong thông báo lỗi (nếu có).
- Lỗ hổng bảo mật: **không** mở issue công khai — xem [`docs/SECURITY.md`](docs/SECURITY.md).
