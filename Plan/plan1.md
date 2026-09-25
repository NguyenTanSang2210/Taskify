# KẾ HOẠCH PHÁT TRIỂN TASKIFY — GIAI ĐOẠN 0 → 4

> **Phiên bản kế hoạch:** 1.3 · **Ngày lập:** 25/09/2026 · **Cập nhật:** 25/09/2026 (đã chốt Decision Log) · **Người thực hiện:** NguyenTanSang2210
> **Căn cứ:** Báo cáo phân tích hiện trạng mã nguồn ngày 25/09/2026 (commit `00862fb` trên `main`).
> **Mục tiêu tổng:** Đưa Taskify từ trạng thái "đồ án chạy demo được" lên "sản phẩm an toàn, ổn định, có thể triển khai thật", sau đó mới mở rộng tính năng.

---

## MỤC LỤC

1. [Cách sử dụng tài liệu này](#1-cách-sử-dụng-tài-liệu-này)
2. [Bảng tổng quan tiến độ](#2-bảng-tổng-quan-tiến-độ)
3. [Quy trình làm việc chuẩn cho mỗi task](#3-quy-trình-làm-việc-chuẩn-cho-mỗi-task)
4. [Giai đoạn 0 — Khôi phục môi trường & đo baseline](#4-giai-đoạn-0--khôi-phục-môi-trường--đo-baseline)
5. [Giai đoạn 1 — Vá bảo mật](#5-giai-đoạn-1--vá-bảo-mật)
6. [Giai đoạn 2 — Sửa lỗi nghiệp vụ & chức năng](#6-giai-đoạn-2--sửa-lỗi-nghiệp-vụ--chức-năng)
7. [Giai đoạn 3 — Củng cố kỹ thuật](#7-giai-đoạn-3--củng-cố-kỹ-thuật)
8. [Giai đoạn 4 — Tính năng mới & triển khai](#8-giai-đoạn-4--tính-năng-mới--triển-khai)
9. [Các quyết định cần chốt (Decision Log)](#9-các-quyết-định-cần-chốt-decision-log)
10. [Quản lý rủi ro](#10-quản-lý-rủi-ro)
11. [Phụ lục A — Ma trận phân quyền mục tiêu](#11-phụ-lục-a--ma-trận-phân-quyền-mục-tiêu)
12. [Phụ lục B — Lệnh thường dùng](#12-phụ-lục-b--lệnh-thường-dùng)
13. [Nhật ký thay đổi (Changelog)](#13-nhật-ký-thay-đổi-changelog)

---

## 1. Cách sử dụng tài liệu này

### 1.1. Ký hiệu trạng thái

| Ký hiệu | Ý nghĩa |
|:---:|---|
| ⬜ | Chưa bắt đầu |
| 🟨 | Đang làm |
| 🟦 | Đã code xong, đang review/kiểm thử |
| ✅ | Hoàn thành (đạt đủ tiêu chí nghiệm thu) |
| ⛔ | Bị chặn (ghi rõ lý do + task phụ thuộc) |
| ⏭️ | Bỏ qua (ghi rõ lý do) |
| 🕒 | Để dành — tạm hoãn, **sẽ phát triển sau v1.0** (nằm trong lộ trình tương lai) |

### 1.2. Ký hiệu mức ưu tiên

| Mức | Ý nghĩa |
|:---:|---|
| **P0** | Bắt buộc — lỗ hổng/lỗi chặn sử dụng, phải làm trước |
| **P1** | Quan trọng — ảnh hưởng lớn đến chất lượng |
| **P2** | Nên làm — cải thiện đáng kể |
| **P3** | Tùy chọn — làm khi có thời gian |

### 1.3. Viết tắt đường dẫn

| Viết tắt | Đường dẫn đầy đủ |
|---|---|
| `BE/` | `Backend/src/main/java/com/doanltmmt/Backend/` |
| `BE-TEST/` | `Backend/src/test/java/com/doanltmmt/Backend/` |
| `BE-RES/` | `Backend/src/main/resources/` |
| `FE/` | `frontend/src/` |

### 1.4. Cách cập nhật tiến độ

1. Khi bắt đầu một task: đổi `⬜` → `🟨` ở **bảng tổng quan (mục 2)** và ở **tiêu đề task**.
2. Tick `- [x]` từng bước con khi làm xong.
3. Khi xong: đổi sang `✅`, điền **Ngày xong** và **mã Commit** vào dòng "Theo dõi" của task.
4. Ghi một dòng vào **Changelog (mục 13)**.
5. Mỗi khi kết thúc một giai đoạn: kiểm tra toàn bộ **Tiêu chí hoàn thành giai đoạn**, gắn tag git.

---

## 2. Bảng tổng quan tiến độ

### 2.1. Tổng quan theo giai đoạn

| Giai đoạn | Nội dung | Số task | Ước lượng | Trạng thái | Bắt đầu | Kết thúc | Tag git |
|---|---|:---:|:---:|:---:|---|---|---|
| **0** | Khôi phục môi trường & baseline | 9 | 1,5–2 ngày | ✅ (8/9 · P0-09 chờ push) | 25/09/2026 | 25/09/2026 | `v0.1.0-baseline` (chờ) |
| **1** | Vá bảo mật | 13 | 8–10 ngày | ⬜ | | | `v0.2.0-security` |
| **2** | Sửa lỗi nghiệp vụ & chức năng | 13 | 8–10 ngày | ⬜ | | | `v0.3.0-business` |
| **3** | Củng cố kỹ thuật | 14 | 12–15 ngày | ⬜ | | | `v0.4.0-hardening` |
| **4** | Tính năng mới & triển khai | 11 (2 task 🕒 để sau v1.0) | Theo backlog | ⬜ | | | `v1.0.0` |
| | **Tổng** | **60** (58 task cho v1.0 + 2 task 🕒 sau v1.0) | **~31–37 ngày công** (chưa gồm GĐ4) | | | | |

> Ước lượng tính cho **1 người làm toàn thời gian**. Nếu làm bán thời gian (~3h/ngày) nhân khoảng ×2,5.

### 2.2. Lịch trình dự kiến (tham khảo)

| Tuần | Công việc chính |
|---|---|
| Tuần 1 | GĐ0 toàn bộ + GĐ1: P1-01 → P1-04 |
| Tuần 2 | GĐ1: P1-05 → P1-13 |
| Tuần 3 | GĐ2: P2-01 → P2-06 |
| Tuần 4 | GĐ2: P2-07 → P2-13 + bắt đầu GĐ3: P3-01 |
| Tuần 5–6 | GĐ3: P3-02 → P3-10 |
| Tuần 7 | GĐ3: P3-11 → P3-14 |
| Tuần 8+ | GĐ4 theo thứ tự ưu tiên backlog |

### 2.3. Bảng theo dõi chi tiết từng task

| ID | Tên task | Ưu tiên | Ước lượng | Trạng thái |
|---|---|:---:|:---:|:---:|
| P0-01 | Cài JDK 21 & kiểm tra Maven Wrapper | P0 | 0,5h | ✅ |
| P0-02 | Khởi động Docker & chạy full stack | P0 | 1h | ✅ |
| P0-03 | Chạy test backend, ghi baseline | P0 | 1h | ✅ |
| P0-04 | Bổ sung DataSeeder đủ luồng demo | P0 | 2h | ✅ |
| P0-05 | Smoke test 4 vai trò (tự động hóa) | P0 | 3h | ✅ |
| P0-06 | Cập nhật dependency frontend có lỗ hổng | P0 | 1h | ✅ |
| P0-07 | Sửa 18 lỗi ESLint | P1 | 1h | ✅ |
| P0-08 | Dọn repo & bổ sung lint vào CI | P1 | 1h | ✅ |
| P0-09 | Thiết lập Git workflow & tag baseline | P1 | 0,5h | 🟦 (chờ push) |
| P1-01 | Sửa luồng OTP (không cấp JWT trước OTP) | P0 | 1 ngày | ⬜ |
| P1-02 | Chặn tài khoản bị khóa & thu hồi token | P0 | 0,5 ngày | ⬜ |
| P1-03 | Loại bỏ JWT secret mặc định, kiểm tra cấu hình khi khởi động | P0 | 2h | ⬜ |
| P1-04 | Bảo mật WebSocket (CONNECT + SUBSCRIBE) | P0 | 1 ngày | ⬜ |
| P1-05 | Dịch vụ kiểm tra quyền theo đề tài + vá IDOR | P0 | 1,5 ngày | ⬜ |
| P1-06 | Response DTO — chặn lộ dữ liệu cá nhân | P0 | 1,5 ngày | ⬜ |
| P1-07 | Gia cố upload/download file | P0 | 1 ngày | ⬜ |
| P1-08 | Gỡ file cá nhân khỏi git | P0 | 1h | ⬜ |
| P1-09 | Tắt đăng ký công khai, kích hoạt tài khoản & chính sách mật khẩu | P1 | 1 ngày | ⬜ |
| P1-10 | Chống brute-force đăng nhập (bền vững) | P1 | 0,5 ngày | ⬜ |
| P1-11 | Security headers, tắt Swagger ở prod | P1 | 2h | ⬜ |
| P1-12 | Bộ test hồi quy phân quyền | P0 | 1 ngày | ⬜ |
| P1-13 | Nâng cấp dependency backend + Dependabot | P1 | 0,5 ngày | ⬜ |
| P2-01 | Cấp quyền TOPIC_MANAGE cho giảng viên + sửa initializer | P0 | 2h | ⬜ |
| P2-02 | Tách xử lý 401/403 ở frontend + toast lỗi | P0 | 0,5 ngày | ⬜ |
| P2-03 | Sửa lỗi upload/tải file đính kèm | P0 | 0,5 ngày | ⬜ |
| P2-04 | Mô hình nhiều sinh viên/đề tài (capacity) | P1 | 1,5 ngày | ⬜ |
| P2-05 | Chức năng Quên mật khẩu | P1 | 1 ngày | ⬜ |
| P2-06 | Gửi email bất đồng bộ + template tiếng Việt | P1 | 0,5 ngày | ⬜ |
| P2-07 | Sửa scheduler nhắc nhở + nhắc hạn milestone | P1 | 0,5 ngày | ⬜ |
| P2-08 | Kiểm tra trùng lặp hỗ trợ tiếng Việt | P2 | 0,5 ngày | ⬜ |
| P2-09 | Ràng buộc nghiệp vụ (điểm, trạng thái, ngày) | P1 | 0,5 ngày | ⬜ |
| P2-10 | Route cho RoleManagementPage + chặn xóa role đang dùng | P2 | 2h | ⬜ |
| P2-11 | Xóa tính năng ProjectPermission | P2 | 1h | ⬜ |
| P2-12 | Kiểm thử hồi quy nghiệp vụ toàn luồng | P0 | 0,5 ngày | ⬜ |
| P2-13 | Khóa điểm & quy trình chấm lại | P1 | 1 ngày | ⬜ |
| P3-01 | Flyway migration, bỏ ddl-auto & SchemaFixer | P1 | 1 ngày | ⬜ |
| P3-02 | Request DTO + Bean Validation | P1 | 1,5 ngày | ⬜ |
| P3-03 | Global exception handler + chuẩn hóa lỗi | P1 | 0,5 ngày | ⬜ |
| P3-04 | Chuyển trạng thái sang Enum | P1 | 1 ngày | ⬜ |
| P3-05 | Tách logic controller → service | P1 | 2 ngày | ⬜ |
| P3-06 | Phân trang API danh sách | P1 | 1 ngày | ⬜ |
| P3-07 | Tối ưu truy vấn & index | P2 | 1 ngày | ⬜ |
| P3-08 | Logging chuẩn + Actuator health | P2 | 0,5 ngày | ⬜ |
| P3-09 | Testcontainers + JaCoCo + test frontend | P1 | 2 ngày | ⬜ |
| P3-10 | Nâng cấp CI pipeline | P1 | 0,5 ngày | ⬜ |
| P3-11 | Refactor frontend (lazy route, tách trang lớn) | P2 | 2 ngày | ⬜ |
| P3-12 | Thống nhất tên dự án & cấu hình | P3 | 2h | ⬜ |
| P3-13 | Viết lại tài liệu | P2 | 0,5 ngày | ⬜ |
| P3-14 | Access token ngắn hạn + Refresh token | P1 | 1,5 ngày | ⬜ |
| P4-01 | Dashboard thống kê nâng cao | P1 | 3 ngày | ⬜ |
| P4-02 | Giao diện Audit Log cho Admin | P2 | 1 ngày | ⬜ |
| P4-03 | Import người dùng/lớp từ Excel | P1 | 2 ngày | ⬜ |
| P4-04 | Quản lý nhóm (Team) — phát triển sau v1.0 | P3 | 3–4 ngày | 🕒 |
| P4-05 | Nộp bài theo milestone + chấm theo mốc | P2 | 3 ngày | ⬜ |
| P4-06 | Cài đặt thông báo cá nhân + email digest | P3 | 1,5 ngày | ⬜ |
| P4-07 | Học phí — tạm hoãn, phát triển sau v1.0 | P3 | Chưa ước lượng | 🕒 |
| P4-08 | Observability (metrics, dashboard) | P2 | 1,5 ngày | ⬜ |
| P4-09 | Triển khai production (HTTPS, backup) | P1 | 2 ngày | ⬜ |
| P4-10 | Mở rộng realtime (broker ngoài) | P3 | 1,5 ngày | ⬜ |
| P4-11 | Hoàn thiện UI theo mockup `stitch/` + a11y | P2 | 3 ngày | ⬜ |

---

## 3. Quy trình làm việc chuẩn cho mỗi task

### 3.1. Nhánh Git

> **Quy ước đã chốt với chủ dự án (25/09/2026): KHÔNG tạo nhánh task.** Code sửa trực tiếp trên `main` local; chủ dự án tự đẩy lên `develop` rồi merge vào `main`.

```
main (local)  ← sửa code trực tiếp, mỗi task 1 commit có mã task
   │  git push origin main:develop      (chủ dự án tự đẩy)
   ▼
origin/develop ← CI chạy kiểm tra
   │  PR / merge develop → main          (chủ dự án tự merge)
   ▼
origin/main   ← gắn tag khi kết thúc mỗi giai đoạn
```

- Mỗi task = **1 commit** (task lớn có thể nhiều commit, ghi rõ `[P1-05 phần 1/3]`) để dễ truy vết và revert.
- Claude **không** tạo nhánh, **không** push/merge/tag — chỉ commit trên `main` local; việc đẩy lên GitHub do chủ dự án thực hiện.

### 3.2. Quy ước commit (Conventional Commits)

```
<loại>(<phạm-vi>): <mô tả ngắn tiếng Việt hoặc tiếng Anh> [<ID-task>]

Ví dụ:
security(auth): chỉ cấp access token sau khi xác thực OTP [P1-01]
fix(frontend): 403 không còn tự đăng xuất người dùng [P2-02]
test(security): thêm test IDOR cho milestone và calendar [P1-12]
```

### 3.3. Checklist "Định nghĩa Hoàn thành" (Definition of Done) — áp dụng cho MỌI task

- [ ] Code đã viết xong, tự review lại diff.
- [ ] Backend: `.\mvnw.cmd -q test` pass.
- [ ] Frontend: `npm run lint` và `npm run build` pass.
- [ ] Có test tự động cho thay đổi (bắt buộc với task bảo mật/nghiệp vụ).
- [ ] Đã test thủ công luồng liên quan trên giao diện.
- [ ] Không commit secret, file `.env`, file upload thật.
- [ ] Cập nhật tài liệu nếu thay đổi API/cấu hình.
- [ ] CI xanh trên `develop` sau khi chủ dự án push.
- [ ] Đã cập nhật trạng thái trong file kế hoạch này + Changelog.

### 3.4. Mẫu mô tả PR `develop` → `main` (khi chủ dự án merge, tùy chọn)

```markdown
## Task
P1-01 — Sửa luồng OTP

## Thay đổi
- ...

## Cách kiểm thử
1. ...

## Ảnh hưởng
- API thay đổi: ...
- Frontend cần cập nhật: ...
- Migration DB: có/không
```

---

## 4. GIAI ĐOẠN 0 — Khôi phục môi trường & đo baseline

**🎯 Mục tiêu:** Chạy lại được toàn bộ hệ thống trên máy sau 5 tháng, xác nhận bằng thực nghiệm các lỗi đã phát hiện qua đọc code, và có "ảnh chụp" trạng thái ban đầu để so sánh về sau.

**📌 Điều kiện bắt đầu:** Không có.
**⏱️ Ước lượng:** 1,5–2 ngày.
**⚠️ Nguyên tắc:** Giai đoạn này **không sửa logic nghiệp vụ**. Chỉ sửa môi trường, dữ liệu mẫu, lint, dependency.

---

### ✅ P0-01 — Cài JDK 21 & kiểm tra Maven Wrapper

**Vấn đề:** Máy hiện không có `java` trong PATH, không build được backend.

**Các bước:**
- [x] Cài **Eclipse Temurin JDK 21** — đã cài bản **portable** (không cần quyền admin) tại `%USERPROFILE%\.jdks\jdk-21.0.12.1+1`, đặt biến `JAVA_HOME` + `Path` cấp **User**.
- [x] `java -version` → `openjdk 21.0.12.1 2026-08-18 LTS (Temurin)`.
- [x] `.\mvnw.cmd -v` → Maven 3.9.11, Java 21.0.12.1.
- [x] `.\mvnw.cmd -q -DskipTests compile` → BUILD SUCCESS.
- [ ] (Tùy chọn) Cấu hình IntelliJ/VS Code dùng JDK 21 cho project — **bạn tự làm** (IntelliJ tự nhận JDK trong `~/.jdks`).

**✅ Nghiệm thu:** `mvnw compile` thành công, không lỗi. ✔️
**Theo dõi:** Ngày xong: `25/09/2026` · Ghi chú: Terminal mở **trước** khi cài cần mở lại để nhận `JAVA_HOME`.

---

### ✅ P0-02 — Khởi động Docker & chạy full stack

**Vấn đề:** Docker Desktop đang tắt; chưa xác nhận stack còn chạy được.

**Các bước:**
- [x] Bật Docker Desktop (Engine 28.5.1). Trước đó Docker **trống hoàn toàn** (không có volume/DB cũ → không mất dữ liệu).
- [x] Tạo `.env` gốc từ `.env.docker.example` (đã gitignore).
- [x] `JWT_SECRET` ngẫu nhiên 48 byte mới. `MAIL_USERNAME/MAIL_PASSWORD` **để mặc định** → email không gửi được, lấy OTP bằng `.\scripts\get-otp.ps1` (xem mục "Cách lấy OTP" bên dưới).
- [x] `SPRING_PROFILES_ACTIVE=dev`, `APP_SEED_ENABLED=true`; đã thêm `APP_SEED_ENABLED` + `APP_SEED_OTP_EMAIL` vào `docker-compose.yml`.
- [x] `docker compose up --build -d` → `ktpm-db` healthy, `ktpm-backend`, `ktpm-frontend` Up. Backend khởi động ~6 giây, không exception.
- [x] `http://localhost:5175` → 200.
- [ ] ~~Swagger `http://localhost:8081/swagger-ui.html`~~ → **403**: phát hiện mới — `SecurityConfig` không `permitAll` cho Swagger nên **không mở được Swagger bằng trình duyệt**. Đã đưa vào **P1-11** (mở Swagger ở dev, tắt ở prod).
- [x] Chạy local: `.\scripts\run-backend.ps1` → backend cổng 8080 khởi động OK, đăng nhập được. *(Phát hiện & đã sửa: script cũ dùng đường dẫn tương đối `../Backend` nên chạy từ thư mục gốc bị sai.)*

**✅ Nghiệm thu:** Đăng nhập được bằng tài khoản demo trên cả Docker (8081) và local (8080). ✔️
**Theo dõi:** Ngày xong: `25/09/2026` · Ghi chú: Frontend Docker và `run-frontend.ps1` cùng dùng cổng 5175 → muốn chạy frontend local phải `docker compose stop frontend` trước.

---

### ✅ P0-03 — Chạy test backend, ghi baseline

**Vấn đề:** 3/8 file test dùng `@SpringBootTest` → cần MySQL thật; chưa biết test còn pass không.

**Các bước:**
- [x] MySQL dùng service `db` của compose, cổng `3307`.
- [x] Tạo `Backend/.env` (đã gitignore) trỏ tới `localhost:3307`, user `ktpm`, dùng chung `JWT_SECRET` với `.env` gốc.
- [x] Chạy `.\mvnw.cmd test` → **19/19 pass**.
- [x] Không có test fail.

> ⚠️ Lưu ý: `.env` chỉ được nạp trong `BackendApplication.main()`, **test không tự đọc `Backend/.env`**. `scripts/pre-demo-check.ps1` đã được sửa để nạp `Backend/.env` vào biến môi trường trước khi chạy test.

**Bảng baseline test backend (25/09/2026):**

| File test | Số test | Pass | Fail | Ghi chú |
|---|:---:|:---:|:---:|---|
| BackendApplicationTests | 1 | 1 | 0 | Cần MySQL |
| TopicControllerTest | 1 | 1 | 0 | Cần MySQL |
| TopicRegistrationControllerTest | 1 | 1 | 0 | Cần MySQL |
| UserControllerTest | 3 | 3 | 0 | `@WebMvcTest` |
| RoleServiceTest | 3 | 3 | 0 | Mockito |
| TopicRegistrationServiceTest | 3 | 3 | 0 | Mockito |
| TopicServiceTest | 3 | 3 | 0 | Mockito |
| WorkspaceServiceTest | 4 | 4 | 0 | Mockito |
| **Tổng** | **19** | **19** | **0** | BUILD SUCCESS |

**✅ Nghiệm thu:** Bảng trên được điền đầy đủ. ✔️
**Theo dõi:** Ngày xong: `25/09/2026`

---

### ✅ P0-04 — Bổ sung DataSeeder đủ luồng demo

**Vấn đề:** `BE/DataSeeder.java` chỉ tạo `admin`, `deptadmin`, `student`. **Không có giảng viên**, **không có lớp học thuật (AcademicClass)**, sinh viên demo **không được gán `academicClass`** → sinh viên demo **không thể đăng ký đề tài** (lỗi `"Student must be assigned to a class"` trong `TopicRegistrationService.registerTopic`). Không thể smoke test toàn luồng.

**File ảnh hưởng:** `BE/DataSeeder.java`

**Các bước:**
- [x] Thêm lớp `AcademicClass` mẫu: mã `CNTT-K15`, thuộc khoa `CNTT` (thêm `AcademicClassRepository.findByDepartment_IdAndCode`).
- [x] Thêm user `lecturer` / `123456`, role `LECTURER`, khoa `CNTT` + bản ghi `Lecturer` (ThS, Công nghệ phần mềm).
- [x] Thêm user `student2` / `123456`.
- [x] Gán `academicClass = CNTT-K15` và `department = CNTT` cho `student`, `student2` (kể cả khi user đã tồn tại từ trước).
- [x] Tạo sẵn workspace mẫu `DRAFT` "Đồ án CNPM HK1 2026-2027".
- [x] Seeder **idempotent** — đã kiểm chứng: chạy 3 lần (Docker, test context, backend local) vẫn chỉ 1 workspace mẫu, 1 lớp, 5 user demo.
- [x] Email OTP: **không ghi email thật vào code** (repo có thể public). Thay bằng biến `APP_SEED_OTP_EMAIL` — ví dụ `ten@gmail.com` → admin nhận ở `ten+admin@gmail.com`, lecturer ở `ten+lecturer@gmail.com` (Gmail plus-addressing, cùng về một hộp thư). Để trống → `admin@example.com`… Chỉ áp dụng khi user được **tạo mới**.
- [x] Tên hiển thị tiếng Việt có dấu (đã kiểm tra lưu đúng UTF-8 trong MySQL).

**Tài khoản demo sau khi hoàn thành:**

| Username | Mật khẩu | Vai trò | Ghi chú |
|---|---|---|---|
| admin | 123456 | ADMIN | Cần OTP |
| deptadmin | 123456 | DEPARTMENT_ADMIN | Khoa CNTT, cần OTP |
| lecturer | 123456 | LECTURER | Khoa CNTT, cần OTP |
| student | 123456 | STUDENT | Lớp CNTT-K15 |
| student2 | 123456 | STUDENT | Lớp CNTT-K15 |

**✅ Nghiệm thu:** DB trống → khởi động → đủ 5 tài khoản, đăng nhập được tất cả. ✔️ (Lần khởi động đầu tiên chính là trên DB trống.)
**Theo dõi:** Ngày xong: `25/09/2026` · Commit: `98027f4`

---

### ✅ P0-05 — Smoke test 4 vai trò (xác nhận lỗi)

**Mục tiêu:** Chạy toàn bộ luồng nghiệp vụ chính, ghi lại **thực tế** chức năng nào chạy/hỏng. Đây là bằng chứng để ưu tiên GĐ1–2.

**Cách làm:** Tự động hóa bằng `node scripts/smoke-test.mjs` (gọi API như frontend, đọc OTP từ DB) → **chạy lại được sau mỗi giai đoạn**: lỗi đã sửa sẽ chuyển từ `BUG` sang `FIXED`. Kết quả chi tiết: [reports/P0-smoke-baseline.md](reports/P0-smoke-baseline.md).

**Kết quả baseline 25/09/2026: OK = 17 · BUG (lỗi đã biết, tái hiện đúng) = 12 · FAIL (ngoài dự kiến) = 0**

| # | Vai trò | Bước | Kỳ vọng | Thực tế | Lỗi liên quan |
|---|---|---|---|---|---|
| 1 | Admin | Đăng nhập + OTP | Vào Dashboard | ✅ OK | 🐞 Token dùng được **trước** OTP → **P1-01 xác nhận** |
| 2 | Admin | Tạo khoa, tạo user GV/SV | Tạo thành công | ✅ OK | |
| 3 | DeptAdmin | Tạo workspace → `OPEN_TOPIC` | Chuyển trạng thái OK | ✅ OK | |
| 4 | DeptAdmin | Gán lớp CNTT-K15 vào workspace, phân công GV | OK | ✅ OK | |
| 5 | **Lecturer** | **Tạo đề tài** | Tạo được | 🐞 **HTTP 403** | **P2-01 xác nhận** (đường vòng: DeptAdmin tạo thay → OK) |
| 6 | DeptAdmin | Chuyển workspace → `OPEN_REGISTRATION` | OK | ✅ OK | |
| 7 | Student | Đăng ký đề tài | Trạng thái "Chờ duyệt" | ✅ OK | |
| 8 | Student2 | Đăng ký **cùng** đề tài (capacity ≥ 2) | Đăng ký được | ✅ OK | |
| 9 | Lecturer | Duyệt Student, rồi duyệt Student2 | Duyệt cả 2 | ✅ SV1 · 🐞 SV2 **HTTP 400** "Topic already has an approved registration" | **P2-04 xác nhận** |
| 10 | DeptAdmin | `LOCK_REGISTRATION` → `IN_PROGRESS` | OK | ✅ OK | |
| 11 | Student | Nộp báo cáo **có file** ở trang Không gian dự án | Nộp OK | 🐞 **HTTP 400** | **P2-03 xác nhận** |
| 12 | Student | Nộp báo cáo có file ở trang Chi tiết tiến độ | Nộp OK | ✅ OK | |
| 13 | Lecturer | Bấm link tải file báo cáo | Tải được | 🐞 **HTTP 403** (không token) · ✅ có token thì tải được | **P2-03 xác nhận** |
| 14 | Lecturer | Nhận xét báo cáo | SV nhận thông báo | ✅ OK | |
| 15 | Student | Kéo thả Kanban | Đổi trạng thái OK | ✅ OK | |
| 16 | Lecturer ↔ Student | Chat | Tin nhắn tới người nhận | ✅ OK | |
| 17 | Lecturer | Chấm điểm | SV thấy điểm | ✅ OK | |
| 18 | Lecturer | Xuất Excel | Tải file .xlsx | ✅ OK | |
| 19 | Admin | Khóa tài khoản Student khi Student đang đăng nhập | Student bị đẩy ra | 🐞 Token cũ vẫn **HTTP 200** | **P1-02 xác nhận** |
| 20 | Bất kỳ | "Quên mật khẩu?" | Có luồng khôi phục | 🐞 **HTTP 404** | **P2-05 xác nhận** |
| 21 | Nginx (Docker) | Upload file 2MB qua cổng 5175 | Upload OK | 🐞 **HTTP 413** | **P2-03 xác nhận** |
| S1 | Student | Đọc `/api/audit/recent` | 403 | 🐞 **HTTP 200** (đọc toàn bộ log) | **P1-05 #12 xác nhận** |
| S2 | Lecturer khác | Tạo milestone trên đề tài người khác | 403 | 🐞 **HTTP 200** | **P1-05 #1 xác nhận** |
| S3 | Student | Xem `/api/users/{id giảng viên}` | Không lộ email | 🐞 Trả về email | **P1-05 #13 xác nhận** |
| S4 | Khách | Tự đăng ký `/api/auth/register` mật khẩu `1` | Bị chặn | 🐞 **HTTP 200** | **P1-09 xác nhận** |

> Giới hạn: kịch bản chạy ở tầng API. Hành vi giao diện "403 → tự đăng xuất" (P2-02) và realtime WebSocket chưa được tự động hóa — kiểm chứng qua đọc code; sẽ thêm E2E ở P3-09.

**Phát hiện mới trong GĐ0 (đã đưa vào kế hoạch):**
- Swagger UI bị chặn 403 (không mở được bằng trình duyệt) → **P1-11**.
- Thư mục `Backend/uploads/` không được ignore → file upload khi test xuất hiện trong `git status` → đã thêm ignore (P0-08), phần gỡ file cũ vẫn ở **P1-08**.
- Script `scripts/*.ps1` sai đường dẫn khi chạy từ thư mục gốc → **đã sửa** (P0-08).

**✅ Nghiệm thu:** Bảng được điền đủ; mọi lỗi mới phát hiện được thêm thành task trong GĐ1/GĐ2. ✔️
**Theo dõi:** Ngày xong: `25/09/2026` · Commit: `a18d1bf`

---

### ✅ P0-06 — Cập nhật dependency frontend có lỗ hổng

**Vấn đề:** `npm audit` báo 5 lỗ hổng: `websocket-driver` (critical), `axios`, `form-data`, `react-router`, `react-router-dom` (high).

**File ảnh hưởng:** `frontend/package-lock.json` (chỉ nâng bản vá trong phạm vi semver, `package.json` không đổi)

**Các bước:**
- [x] `npm audit fix` (không `--force`) → changed 35 packages.
- [x] `npm audit` → **0 vulnerabilities** (cả prod lẫn dev).
- [x] Không cần nâng thủ công.
- [x] `npm run build` OK; smoke test qua nginx (đăng nhập, API) OK.
- [x] `npx update-browserslist-db@latest` — không đổi target browser.

**✅ Nghiệm thu:** 0 lỗ hổng; build pass. ✔️
**Theo dõi:** Ngày xong: `25/09/2026` · Commit: `a121754`

---

### ✅ P0-07 — Sửa 18 lỗi ESLint

**Danh sách lỗi (đều là `no-unused-vars`):**

| File | Dòng | Biến |
|---|---|---|
| `FE/App.jsx` | 34 | `mainMarginLeft` |
| `FE/pages/AdminPage.jsx` | 144, 155 | `error` |
| `FE/pages/DashboardPage.jsx` | 12 | `loading` |
| `FE/pages/DepartmentAdminPage.jsx` | 87, 169, 197, 209, 289 | `error` |
| `FE/pages/LecturerRegistrationPage.jsx` | 161 | `error` |
| `FE/pages/LecturerTopicsPage.jsx` | 1 | `useMemo` |
| `FE/pages/RoleManagementPage.jsx` | 1, 7, 11, 31, 60, 98 | `useMemo`, `user`, `error`, `err` |
| `FE/pages/StudentProgressPage.jsx` | 325 | `e` |

**Các bước:**
- [x] Với `catch (error)` không dùng → đổi thành `catch {` (11 chỗ).
- [x] Biến state không dùng → **hiển thị thay vì xóa**: `DashboardPage` hiện "Đang tải số liệu thống kê..." khi `loading`; `RoleManagementPage` hiện thông báo lỗi khi tải quyền thất bại (trước đây lỗi bị nuốt im lặng).
- [x] Xóa import/biến thừa (`useMemo` ×2, `useAuth`/`user`, `mainMarginLeft`).
- [x] `npm run lint` → 0 lỗi.

**✅ Nghiệm thu:** `npm run lint` exit code 0. ✔️
**Theo dõi:** Ngày xong: `25/09/2026` · Commit: `c059255`

---

### ✅ P0-08 — Dọn repo & bổ sung lint vào CI

**Các bước:**
- [x] Thêm `.idea/` vào `.gitignore` gốc.
- [x] Xóa `Backend/compile_errors.txt`.
- [x] Xóa file rỗng: `FE/components/Navbar.jsx`, `FE/api/authApi.js` (đã kiểm tra không còn import).
- [x] `.github/workflows/CI.yml` job `frontend-build`: thêm `npm run lint` trước `npm run build`.
- [x] Thêm `APP_SEED_ENABLED`, `APP_SEED_OTP_EMAIL` vào `docker-compose.yml` (commit P0-04).
- [x] `TECHNICAL_REPORT.md` mục 10: sửa "Chưa có CI/CD" → mô tả CI hiện có.
- [x] **Bổ sung ngoài kế hoạch:**
  - Ignore `Backend/uploads/*` (file upload lúc chạy thử không còn lọt vào git).
  - Sửa `scripts/run-backend.ps1`, `run-frontend.ps1`, `pre-demo-check.ps1` dùng `$PSScriptRoot` → chạy được từ bất kỳ thư mục nào; `pre-demo-check` nạp `Backend/.env` cho test và dựa vào exit code; lưu UTF-8 BOM để PowerShell 5.1 đọc đúng tiếng Việt.
  - README: thêm danh sách script + tài khoản demo.

**✅ Nghiệm thu:** CI chạy cả lint; `git status` sạch sau khi build/test. ✔️ (CI trên GitHub sẽ chạy khi push nhánh.)
**Theo dõi:** Ngày xong: `25/09/2026` · Commit: `64d0382`

---

### 🟦 P0-09 — Thiết lập Git workflow & tag baseline

> **Trạng thái:** Phần **local đã xong**; các bước **đẩy lên GitHub chờ bạn xác nhận** (push/merge/tag/branch protection là thao tác công khai lên remote).

**Các bước:**
- [x] Mỗi task GĐ0 một commit có mã task trên `main` local (`98027f4` P0-04 · `a121754` P0-06 · `c059255` P0-07 · `64d0382` P0-08 · `a18d1bf` P0-05 · `6ef1fe7` Plan). *(Ban đầu làm trên nhánh `chore/P0-baseline`, đã fast-forward về `main` và xóa nhánh theo yêu cầu.)*
- [ ] Chủ dự án đẩy lên develop: `git push origin main:develop` → chờ CI xanh.
- [ ] Chủ dự án merge `develop` → `main` trên GitHub.
- [ ] Bật **Branch protection** cho `main` (Settings → Branches → Add rule: Require PR + Require status checks).
- [ ] Gắn tag trên `main` sau khi merge: `git tag -a v0.1.0-baseline -m "Baseline sau khi khôi phục môi trường"`; `git push origin v0.1.0-baseline`.
- [ ] (Tùy chọn) GitHub Project/Milestones cho 5 giai đoạn.

**✅ Nghiệm thu:** Tag `v0.1.0-baseline` tồn tại trên remote.
**Theo dõi:** Ngày xong (local): `25/09/2026` · Remote: `____`

---

### 🏁 Tiêu chí hoàn thành Giai đoạn 0

- [x] Backend build + test chạy được trên máy (19/19 pass).
- [x] Full stack chạy bằng Docker Compose và chạy local.
- [x] Đủ 5 tài khoản demo, đi được toàn bộ luồng (trừ các lỗi đã biết).
- [x] Bảng smoke test P0-05 đã điền đầy đủ (17 OK · 12 BUG đã biết · 0 FAIL).
- [x] `npm run lint` = 0 lỗi; `npm audit` = 0 lỗ hổng.
- [ ] Tag `v0.1.0-baseline` — **chờ bạn xác nhận push lên GitHub** (P0-09).

### 📘 Hướng dẫn dùng môi trường sau GĐ0

**Chạy bằng Docker (khuyến nghị):**
```powershell
docker compose up -d            # khởi động (lần đầu thêm --build)
docker compose ps               # xem trạng thái
docker compose logs -f backend  # xem log backend
docker compose up -d --build backend frontend   # build lại sau khi sửa code
docker compose down             # dừng (giữ dữ liệu)
```
- Frontend: http://localhost:5175 · Backend API: http://localhost:8081/api · MySQL: `localhost:3307` (user `ktpm` / `ktpm123`).

**Cách lấy OTP** (tài khoản `admin`, `deptadmin`, `lecturer`) khi chưa cấu hình email:
```powershell
.\scripts\get-otp.ps1 admin     # sau khi bấm Đăng nhập trên giao diện
```
Muốn nhận OTP qua email thật: điền `MAIL_USERNAME` + `MAIL_PASSWORD` (Gmail App Password) vào `.env` → `docker compose up -d backend`.

**Kiểm tra nhanh:**
```powershell
.\scripts\pre-demo-check.ps1    # backend compile + 19 test, frontend lint + build
node scripts/smoke-test.mjs     # 25 kịch bản API toàn luồng (cần Docker đang chạy, profile dev)
```

---

## 5. GIAI ĐOẠN 1 — Vá bảo mật

**🎯 Mục tiêu:** Đóng toàn bộ lỗ hổng đã phát hiện; mỗi lỗ hổng có **test hồi quy** để không tái diễn.

**📌 Điều kiện bắt đầu:** Hoàn thành GĐ0.
**⏱️ Ước lượng:** 8–10 ngày.
**📐 Nguyên tắc:**
1. **Mặc định từ chối** (deny by default): endpoint mới luôn phải có kiểm tra quyền cụ thể, không chỉ `isAuthenticated()`.
2. Kiểm tra quyền ở **tầng service**, không chỉ ở `@PreAuthorize`.
3. Không bao giờ tin `studentId`/`lecturerId`/`userId` do client gửi lên — **luôn lấy từ token**.

**Thứ tự đề xuất:** P1-03 → P1-01 → P1-02 → P1-05 → P1-12 (viết song song) → P1-06 → P1-04 → P1-07 → P1-08 → P1-09 → P1-10 → P1-11 → P1-13.

---

### ⬜ P1-01 — Sửa luồng OTP (không cấp JWT trước OTP)

**Mức độ:** 🔴 Nghiêm trọng
**Vấn đề:** `BE/controller/AuthController.java:84` sinh JWT đầy đủ quyền **trước** khi OTP được xác thực. `/api/auth/otp/verify` chỉ trả `{verified: true/false}`, không cấp gì. Frontend (`FE/pages/LoginPage.jsx:68`) lưu token vào `localStorage.temp_token`. → Kẻ tấn công biết mật khẩu ADMIN/LECTURER **bỏ qua được OTP** bằng cách dùng token này trực tiếp.

**Thiết kế mới:**

```
[1] POST /api/auth/login {username, password}
    ├─ STUDENT          → 200 {accessToken, user..., otpRequired:false}
    └─ ADMIN/LECT/DEPT  → 200 {otpRequired:true, preAuthToken (JWT, scope=OTP_PENDING, TTL 5 phút)}
                           + gửi OTP qua email
[2] POST /api/auth/otp/verify {preAuthToken, code}
    ├─ đúng  → 200 {accessToken, user...}
    └─ sai   → 401; sai quá 5 lần → vô hiệu OTP, buộc login lại
[3] POST /api/auth/otp/resend {preAuthToken}  (cooldown 60 giây)
```

**File ảnh hưởng:**
- `BE/controller/AuthController.java`
- `BE/security/JwtTokenProvider.java` (thêm `generatePreAuthToken`, `getScope`)
- `BE/security/JwtAuthenticationFilter.java` (từ chối token `scope=OTP_PENDING` cho mọi endpoint ngoài `/api/auth/otp/**`)
- `BE/security/WebSocketAuthChannelInterceptor.java` (từ chối token OTP_PENDING)
- `BE/service/OtpService.java`, `BE/entity/OtpToken.java` (thêm `attempts`, `purpose`)
- `BE/dto/LoginResponse.java` (tách `accessToken` / `preAuthToken`)
- `FE/pages/LoginPage.jsx`, `FE/api/auth.js`, `FE/context/AuthContext.jsx`

**Các bước:**
- [ ] Backend: thêm claim `scope` vào JWT (`ACCESS` | `OTP_PENDING`).
- [ ] Backend: `login()` — nếu `otpRequired` chỉ trả `preAuthToken`, không trả access token.
- [ ] Backend: `verifyOtp()` — nhận `preAuthToken` + `code`; lấy username **từ token** (không nhận username từ body); đúng → cấp access token.
- [ ] Backend: `OtpToken` thêm `attempts` (int), `purpose` (`LOGIN` / `ACTIVATION` / `RESET_PASSWORD` — dùng lại ở P1-09, P2-05). Sai > 5 lần → đánh dấu `used=true`.
- [ ] Backend: khi gửi OTP mới → vô hiệu các OTP cũ chưa dùng của user đó.
- [ ] Backend: endpoint `/otp/resend` với cooldown 60s.
- [ ] Backend: filter từ chối token có `scope != ACCESS` (trả 401).
- [ ] Frontend: lưu `preAuthToken` trong **state React** (không dùng localStorage); xóa toàn bộ `temp_token`/`temp_user`.
- [ ] Frontend: thêm nút "Gửi lại mã" có đếm ngược 60s.
- [ ] Test (xem P1-12): token OTP_PENDING gọi `/api/users` → 401; verify sai 6 lần → bị khóa OTP; verify đúng → nhận access token hoạt động.

**✅ Nghiệm thu:**
- Đăng nhập ADMIN, **không nhập OTP**, không có cách nào gọi API nghiệp vụ.
- DevTools → Application → Local Storage không còn `temp_token`.
- STUDENT đăng nhập vẫn như cũ.

**Theo dõi:** Trạng thái: ⬜ · Ngày xong: `____` · Commit: `____`

---

### ⬜ P1-02 — Chặn tài khoản bị khóa & thu hồi token

**Mức độ:** 🔴 Nghiêm trọng
**Vấn đề:** `BE/security/JwtAuthenticationFilter.java:34` chỉ kiểm tra chữ ký token, không kiểm tra `userDetails.isEnabled()`. Tài khoản bị admin khóa vẫn dùng được token cũ tới 24 giờ. Đổi mật khẩu cũng không vô hiệu token cũ.

**Thiết kế:** Thêm cột `token_version` (int) vào bảng `users`. Token chứa claim `tv`. Filter so sánh `tv == user.tokenVersion`. Tăng `tokenVersion` khi: khóa tài khoản, đổi mật khẩu, reset mật khẩu, đổi role.

**File ảnh hưởng:** `BE/entity/User.java`, `BE/security/JwtAuthenticationFilter.java`, `BE/security/WebSocketAuthChannelInterceptor.java`, `BE/security/JwtTokenProvider.java`, `BE/controller/UserController.java` (updateStatus, updateRole, changePassword), `BE/controller/AuthController.java`.

**Các bước:**
- [ ] Filter: nếu `!userDetails.isEnabled()` → không set authentication (trả 401).
- [ ] Thêm `User.tokenVersion` (mặc định 0) + claim `tv` khi tạo token.
- [ ] Filter + WS interceptor: so sánh `tv`, lệch → 401.
- [ ] Tăng `tokenVersion` ở: `updateStatus(active=false)`, `updateRole`, `changePassword`, reset mật khẩu (P2-05).
- [ ] Thêm endpoint `POST /api/auth/logout` tăng `tokenVersion` (đăng xuất mọi thiết bị) — tùy chọn.
- [ ] Frontend: khi nhận 401 → logout (đã có, sẽ tinh chỉnh ở P2-02).

**✅ Nghiệm thu:** Kịch bản #19 của P0-05: admin khóa student → request tiếp theo của student bị 401 và về trang login.
**Theo dõi:** Trạng thái: ⬜ · Ngày xong: `____` · Commit: `____`

---

### ⬜ P1-03 — Loại bỏ JWT secret mặc định, kiểm tra cấu hình khi khởi động

**Mức độ:** 🔴 Nghiêm trọng
**Vấn đề:** `docker-compose.yml:36` có `JWT_SECRET: ${JWT_SECRET:-ZGV2...}` và profile mặc định là `prod`. Nếu triển khai quên `.env` → dùng secret công khai trên GitHub → **ai cũng giả mạo được token admin**. Tương tự: `MYSQL_ROOT_PASSWORD:-root123`, `application-dev.properties` có fallback `DB_PASSWORD:123456`.

**File ảnh hưởng:** `docker-compose.yml`, `.env.docker.example`, `BE/security/JwtTokenProvider.java`, `BE-RES/application-prod.properties`.

**Các bước:**
- [ ] `docker-compose.yml`: đổi thành `JWT_SECRET: ${JWT_SECRET:?Thiếu JWT_SECRET trong .env}` (compose sẽ báo lỗi nếu thiếu). Làm tương tự cho `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD`.
- [ ] `.env.docker.example`: thay giá trị bằng placeholder `CHANGE_ME_...` + hướng dẫn sinh secret.
- [ ] `JwtTokenProvider`: thêm `@PostConstruct validateSecret()` — decode base64, yêu cầu ≥ 32 byte; nếu profile `prod` và secret trùng secret dev đã biết → **ném exception, dừng ứng dụng**.
- [ ] Cache `Key` một lần thay vì decode mỗi request (`getSigningKey()` đang tạo mới mỗi lần).
- [ ] Cập nhật CI dùng secret riêng (đã có — giữ nguyên).
- [ ] **Nếu đã từng deploy công khai với secret mặc định:** coi như đã lộ, đổi secret ngay.

**✅ Nghiệm thu:** `docker compose up` với `.env` thiếu `JWT_SECRET` → báo lỗi rõ ràng, không khởi động.
**Theo dõi:** Trạng thái: ⬜ · Ngày xong: `____` · Commit: `____`

---

### ⬜ P1-04 — Bảo mật WebSocket (CONNECT + SUBSCRIBE)

**Mức độ:** 🔴 Nghiêm trọng
**Vấn đề:**
1. `WebSocketAuthChannelInterceptor` cho phép CONNECT **không có header Authorization** (chỉ từ chối khi có token sai).
2. Không kiểm tra SUBSCRIBE → bất kỳ ai cũng subscribe được `/topic/messages/{userId}` và `/topic/notifications/{userId}` của người khác → **đọc trộm tin nhắn riêng, thông báo**.
3. `/topic/events` broadcast mọi sự kiện cho tất cả.

**Thiết kế (khuyến nghị):** Chuyển kênh cá nhân sang **user destination** của Spring:

| Kênh cũ | Kênh mới | Cách gửi backend |
|---|---|---|
| `/topic/notifications/{userId}` | `/user/queue/notifications` | `convertAndSendToUser(username, "/queue/notifications", payload)` |
| `/topic/messages/{userId}` | `/user/queue/messages` | `convertAndSendToUser(username, "/queue/messages", payload)` |
| `/topic/progress/{topicId}` | giữ nguyên | + kiểm tra SUBSCRIBE: phải là thành viên đề tài |
| `/topic/registration/{topicId}` | giữ nguyên | + kiểm tra SUBSCRIBE: phải là GV của đề tài / DeptAdmin trong khoa / Admin |
| `/topic/events` | **bỏ** hoặc chỉ ADMIN | — |

**File ảnh hưởng:**
- `BE/security/WebSocketAuthChannelInterceptor.java`
- `BE/config/WebSocketConfig.java` (`setUserDestinationPrefix("/user")`, `enableSimpleBroker("/topic","/queue")`)
- `BE/service/EventPublisher.java`
- `FE/components/NotificationsBell.jsx:29`, `FE/pages/NotificationsPage.jsx:35`, `FE/pages/ProjectChatPage.jsx:132`

**Các bước:**
- [ ] CONNECT: không có token hoặc token không hợp lệ → ném `MessagingException` (từ chối kết nối).
- [ ] SUBSCRIBE: parse destination; với `/topic/progress/{id}` và `/topic/registration/{id}` gọi `TopicAccessService` (tạo ở P1-05) để kiểm tra; không đủ quyền → từ chối.
- [ ] SEND tới `/app/**`: hiện chưa có `@MessageMapping` → từ chối toàn bộ SEND từ client cho an toàn.
- [ ] `EventPublisher`: đổi `notificationCreated`, `messageCreated` sang `convertAndSendToUser`.
- [ ] Bỏ `genericEvent` → `/topic/events` (kiểm tra frontend không subscribe — hiện không có trang nào subscribe).
- [ ] Frontend: đổi destination ở 3 file trên.
- [ ] Test: client không token → CONNECT bị từ chối; SV A subscribe `/topic/progress/{đề tài của B}` → bị từ chối.

**✅ Nghiệm thu:** Thông báo & chat realtime vẫn hoạt động; không thể nghe lén kênh của người khác.
**Theo dõi:** Trạng thái: ⬜ · Ngày xong: `____` · Commit: `____`

---

### ⬜ P1-05 — Dịch vụ kiểm tra quyền theo đề tài + vá IDOR

**Mức độ:** 🔴 Nghiêm trọng
**Vấn đề:** Nhiều endpoint chỉ kiểm tra role, không kiểm tra người dùng có liên quan tới đề tài/tài nguyên hay không (IDOR — Insecure Direct Object Reference).

**Bước 1 — Tạo `BE/service/TopicAccessService.java`:**

```java
// Định nghĩa "thành viên đề tài":
//   - ADMIN: luôn đúng
//   - DEPARTMENT_ADMIN: đề tài thuộc workspace trong khoa của mình
//   - LECTURER: là giảng viên hướng dẫn của đề tài
//   - STUDENT: có đăng ký ĐÃ DUYỆT với đề tài
boolean isTopicMember(Topic topic, User user);
void requireTopicMember(Topic topic);      // ném 403 nếu không phải thành viên
void requireTopicOwner(Topic topic);       // chỉ GV hướng dẫn / DeptAdmin trong khoa / Admin
Topic loadTopicOr404(Long topicId);
```

**Bước 2 — Áp dụng vào từng endpoint:**

| # | Endpoint | Hiện tại | Sửa thành | File |
|---|---|---|---|---|
| 1 | `POST/PUT/DELETE /api/milestones` | Mọi LECTURER | `requireTopicOwner` | `BE/controller/MilestoneController.java` |
| 2 | `GET /api/milestones`, `/{id}` | Mọi user | `requireTopicMember` | như trên |
| 3 | `POST/DELETE /api/calendar` | Mọi LECTURER | `requireTopicOwner` | `BE/controller/CalendarController.java` |
| 4 | `GET /api/calendar` | Mọi user | `requireTopicMember` | như trên |
| 5 | `GET/POST /api/discuss/**` | Mọi user | `requireTopicMember` | `BE/controller/DiscussionController.java` |
| 6 | `POST /api/announcements` (có topicId) | Mọi LECTURER | `requireTopicOwner` | `BE/controller/AnnouncementController.java` |
| 7 | `POST /api/announcements` (không topicId — toàn hệ thống) | LECTURER, ADMIN | Chỉ ADMIN | như trên |
| 8 | `GET /api/announcements?topicId` | Mọi user | `requireTopicMember` | như trên |
| 9 | `POST /api/messages/send` | Gửi cho bất kỳ ai | Người gửi **và** người nhận đều là thành viên đề tài | `BE/controller/MessageController.java` |
| 10 | `GET /api/export/excel` | LECTURER xuất toàn hệ thống | LECTURER: bắt buộc `topicId` + là owner; DEPT_ADMIN: trong khoa; ADMIN: tất cả | `BE/controller/ExportController.java` |
| 11 | `POST /api/notifications/create` | LECTURER gửi cho bất kỳ ai | ADMIN; hoặc LECTURER chỉ gửi cho SV thuộc đề tài mình | `BE/controller/NotificationController.java` |
| 12 | `GET /api/audit/recent` | **Mọi user** | Chỉ ADMIN (+ phân trang ở P3-06) | `BE/controller/AuditLogController.java` |
| 13 | `GET /api/users/{id}` | Mọi user, trả email/phone | Chính mình / ADMIN / DEPT_ADMIN cùng khoa → đầy đủ; người khác → chỉ `{id, fullName}` | `BE/controller/UserController.java` |
| 14 | `POST /api/progress/create` với `milestoneId` | Milestone bất kỳ | Milestone phải thuộc `topicId` | `BE/controller/ProgressController.java` |
| 15 | `GET /api/progress/student/{id}` | Thiếu DEPT_ADMIN | Thêm DEPT_ADMIN theo phạm vi khoa | như trên |
| 16 | `PUT /api/users/{id}/profile` | Cho đổi email tự do | Kiểm tra email trùng; đổi email → cần xác minh (tùy chọn) | `BE/controller/UserController.java` |

**Các bước:**
- [ ] Tạo `TopicAccessService` + unit test đầy đủ 4 role × (thành viên / không thành viên).
- [ ] Áp dụng lần lượt 16 mục trên (tick từng mục):
  - [ ] 1 · [ ] 2 · [ ] 3 · [ ] 4 · [ ] 5 · [ ] 6 · [ ] 7 · [ ] 8
  - [ ] 9 · [ ] 10 · [ ] 11 · [ ] 12 · [ ] 13 · [ ] 14 · [ ] 15 · [ ] 16
- [ ] Thay các đoạn lặp `if (scope.hasRole("DEPARTMENT_ADMIN") && !scope.hasRole("ADMIN")) {...}` rải rác bằng `TopicAccessService`.
- [ ] Rà soát frontend: trang nào đang gọi endpoint bị siết quyền → vẫn hoạt động đúng với người có quyền.

**✅ Nghiệm thu:** Mỗi mục có ít nhất 1 test "không có quyền → 403" trong P1-12.
**Theo dõi:** Trạng thái: ⬜ · Ngày xong: `____` · Commit: `____`

---

### ⬜ P1-06 — Response DTO: chặn lộ dữ liệu cá nhân

**Mức độ:** 🟠 Cao
**Vấn đề:** Controller trả thẳng entity JPA (`ProgressReport`, `Message`, `DiscussionThread`, `DiscussionPost`, `Announcement`, `Notification`, `Milestone`, `CalendarEvent`, `AuditLog`, `Role`) → Jackson serialize cả cây quan hệ: `User` (email, phone, department, role + toàn bộ privileges). `UserDTO` cũng chứa nguyên entity `Role` và `Department`. Payload WebSocket (`EventPublisher`) cũng gửi entity.

**Chiến lược:** Tạo DTO **giữ nguyên đường dẫn JSON mà frontend đang dùng** (ví dụ `student.user.fullName`) nhưng chỉ chứa trường an toàn → giảm tối đa sửa frontend.

**DTO cần tạo** (thư mục `BE/dto/response/`):

| DTO | Trường | Dùng ở |
|---|---|---|
| `UserSummaryDTO` | id, username, fullName | Mọi chỗ nhúng user |
| `UserDTO` (sửa) | + email, phone, active, `roleName`, `departmentId`, `departmentName` | Admin, profile |
| `StudentSummaryDTO` | id, studentCode, `user: UserSummaryDTO` | Progress, Registration |
| `ProgressReportDTO` | id, title, content, status, fileUrl, deadline, createdAt, lecturerComment, `topic {id,title}`, `student`, `milestoneId` | ProgressController, EventPublisher |
| `MessageDTO` | id, content, createdAt, `sender`, `recipient`, topicId | MessageController, EventPublisher |
| `DiscussionThreadDTO`, `DiscussionPostDTO` | … `createdBy`/`author: UserSummaryDTO` | DiscussionController |
| `AnnouncementDTO`, `NotificationDTO`, `MilestoneDTO`, `CalendarEventDTO`, `AuditLogDTO`, `RoleDTO` | trường cần thiết | Controller tương ứng |

**Các bước:**
- [ ] Trước khi sửa: ghi lại JSON mẫu hiện tại của từng endpoint (dùng Swagger/Postman) để so sánh.
- [ ] Tạo DTO + phương thức `static from(Entity)`.
- [ ] Đổi kiểu trả về của controller + `EventPublisher`.
- [ ] Grep frontend các trường đang dùng (`.user.email`, `.role.name`, `.department.name`…) → cập nhật nếu đổi.
- [ ] Cấu hình `spring.jpa.open-in-view=false` (sau khi đã dùng DTO) để tránh lazy-load ngoài transaction.
- [ ] Test: response của `/api/discuss/threads/{id}/posts` **không chứa** `email`, `phone`, `privileges`.

**✅ Nghiệm thu:** Không endpoint nào trả entity JPA trực tiếp (grep `public .*Entity` kiểu trả về trong controller = 0); frontend hoạt động bình thường.
**Theo dõi:** Trạng thái: ⬜ · Ngày xong: `____` · Commit: `____`

---

### ⬜ P1-07 — Gia cố upload/download file

**Mức độ:** 🟠 Cao
**Vấn đề:**
- `BE/controller/FileUploadController.java`: không giới hạn loại file, tối đa 100MB, không lưu ai là người upload.
- `BE/controller/SecureFileController.java`: xác định quyền bằng `findByFileUrlEndingWith(filename)` — dựa vào chuỗi `fileUrl` **do client gửi lên** khi tạo báo cáo → có thể gắn file của người khác vào báo cáo của mình; nếu 2 báo cáo cùng file → lỗi `IncorrectResultSize`.
- `Content-Disposition` ghép tên file thô (có thể chứa ký tự đặc biệt).

**Thiết kế:** Entity mới `StoredFile`:

```
stored_files(id, stored_name UNIQUE, original_name, content_type, size_bytes,
             owner_id FK users, created_at, ref_type NULL, ref_id NULL)
```

**Các bước:**
- [ ] Tạo entity/repository `StoredFile` + `FileStorageService` (lưu, đọc, kiểm tra quyền).
- [ ] Whitelist phần mở rộng: `pdf, doc, docx, xls, xlsx, ppt, pptx, zip, rar, png, jpg, jpeg, txt`; kiểm tra **magic bytes** (Apache Tika `tika-core`) khớp loại khai báo.
- [ ] Giảm giới hạn: `max-file-size=20MB` (cấu hình được qua env).
- [ ] Upload trả về `{fileId, fileName, fileUrl: "/api/secure/files/{storedName}"}`.
- [ ] `ProgressController.create`: nhận `fileId` (hoặc `fileUrl`), kiểm tra `StoredFile.owner == currentUser` → gán `refType=PROGRESS_REPORT, refId`.
- [ ] `SecureFileController`: tra `StoredFile` theo `storedName`, quyền = owner / thành viên đề tài của báo cáo (`TopicAccessService`) / Admin.
- [ ] `Content-Disposition`: dùng `ContentDisposition.attachment().filename(originalName, UTF_8)`.
- [ ] Đường dẫn upload lấy từ cấu hình `app.upload.dir` (thay vì `Paths.get("uploads")` cứng ở 2 nơi).
- [ ] Job dọn file mồ côi (upload > 24h không gắn vào báo cáo) — tùy chọn.

**✅ Nghiệm thu:** Upload `.exe` → 400; SV A không tải được file của SV B (khác đề tài) → 403; tên file tiếng Việt tải về đúng.
**Theo dõi:** Trạng thái: ⬜ · Ngày xong: `____` · Commit: `____`

---

### ⬜ P1-08 — Gỡ file cá nhân khỏi git

**Mức độ:** 🟠 Cao
**Vấn đề:** 3 file tài liệu thật của sinh viên đã commit trong `Backend/uploads/` (`Nhom5.doc` ×2, `Phieu theo doi tien do DACN.doc`).

**Quyết định D-04 = B:** Xóa khỏi **cả lịch sử git** (viết lại lịch sử + force push).

> ⚠️ **Thao tác phá hủy, không hoàn tác được trên remote.** Chỉ thực hiện khi: mọi commit trên `main` local đã được push lên `develop`/`main`, và đã có bản backup mirror. Nên làm **ngay đầu GĐ1**, trước khi có thêm commit mới, để giảm xung đột.

**Các bước:**

*Bước A — Gỡ khỏi phiên bản hiện tại:*
- [ ] `git rm --cached -r Backend/uploads` + thêm `Backend/uploads/` vào `.gitignore` (giữ `Backend/uploads/.gitkeep` nếu cần thư mục).
- [ ] Commit & push lên `develop`, merge vào `main`.

*Bước B — Chuẩn bị:*
- [ ] Đảm bảo không còn thay đổi chưa commit/chưa push ở mọi nhánh (`git status`, `git log origin/<nhánh>..<nhánh>`).
- [ ] Backup toàn bộ repo: `git clone --mirror https://github.com/NguyenTanSang2210/<repo>.git D:\backup\taskify-mirror-<ngày>.git`.
- [ ] Cài công cụ: `pip install git-filter-repo`.
- [ ] Tạm **tắt Branch protection** của `main` trên GitHub (để cho phép force push), ghi nhớ bật lại.

*Bước C — Viết lại lịch sử (trên một bản clone mới):*
- [ ] `git clone https://github.com/NguyenTanSang2210/<repo>.git taskify-clean; cd taskify-clean`
- [ ] `git filter-repo --path Backend/uploads --invert-paths`
- [ ] Kiểm tra: `git log --all --oneline -- Backend/uploads` → **rỗng**.
- [ ] Kiểm tra code vẫn nguyên vẹn: so sánh `git diff` giữa bản clean và bản backup ở HEAD (chỉ khác thư mục uploads).
- [ ] `git remote add origin <url>` (filter-repo tự gỡ remote) → `git push --force --all` → `git push --force --tags`.

*Bước D — Hoàn tất:*
- [ ] Bật lại Branch protection cho `main`.
- [ ] Trên máy làm việc: **xóa bản clone cũ, clone lại** (không pull vào bản cũ — sẽ đưa lịch sử cũ quay lại).
- [ ] Nếu repo đã từng có fork/Pull Request cũ chứa file: liên hệ GitHub Support để xóa cache (tùy chọn).
- [ ] Giữ bản backup mirror ở nơi riêng tư, **không** đẩy lên đâu cả; xóa sau khi xác nhận ổn định (ví dụ 30 ngày).

**✅ Nghiệm thu:** `git ls-files Backend/uploads` rỗng **và** `git log --all -- Backend/uploads` rỗng trên bản clone mới từ GitHub.
**Theo dõi:** Trạng thái: ⬜ · Ngày xong: `____` · Vị trí backup: `____`

---

### ⬜ P1-09 — Tắt đăng ký công khai, kích hoạt tài khoản & chính sách mật khẩu

**Mức độ:** 🟡 Trung bình
**Vấn đề:** `POST /api/auth/register` công khai: ai cũng tạo được tài khoản STUDENT, không validate, không có khoa/lớp, không email xác minh. `FE/pages/LoginPage.jsx` có form "Đăng ký mới" (`renderRegisterForm`, nút "Đăng ký ngay"). Trái với tài liệu vận hành ("Admin cấp tài khoản tập trung"). Không có chính sách mật khẩu ở đâu cả.

**Quyết định D-02 = A:** Tắt hoàn toàn đăng ký công khai. Tài khoản chỉ do **Admin tạo** (hoặc **import Excel** ở P4-03). Người dùng **tự đặt mật khẩu lần đầu** qua email kích hoạt — đúng quy trình Bước 1–3 ở tài liệu vận hành mục 1.

**Luồng kích hoạt tài khoản mới:**
```
Admin tạo user (không nhập mật khẩu, hoặc mật khẩu tạm)
  → user.status = PENDING_ACTIVATION, mật khẩu ngẫu nhiên không ai biết
  → gửi email: mã kích hoạt (OTP purpose=ACTIVATION, hạn 72 giờ)
Người dùng vào trang "Kích hoạt tài khoản" → nhập username/email + mã + mật khẩu mới
  → POST /api/auth/activate → active=true, tăng tokenVersion
Admin có nút "Gửi lại email kích hoạt"
```

**Các bước:**

*Backend:*
- [ ] Xóa endpoint `POST /api/auth/register` trong `BE/controller/AuthController.java` (cùng các dependency không còn dùng: `StudentRepository`, `RoleRepository` nếu thừa).
- [ ] Xóa quy tắc permitAll thừa nếu có; kiểm tra `/api/auth/register` trả **404**.
- [ ] Thêm `purpose=ACTIVATION` cho `OtpToken` (đã chuẩn bị ở P1-01), hạn 72 giờ.
- [ ] `UserController.create`: mật khẩu **không bắt buộc**; nếu bỏ trống → sinh ngẫu nhiên, đặt `active=false`, gửi email kích hoạt. Nếu Admin nhập mật khẩu tạm → bắt buộc đổi ở lần đăng nhập đầu (cờ `mustChangePassword=true`).
- [ ] Endpoint mới: `POST /api/auth/activate {identifier, code, newPassword}` và `POST /api/users/{id}/resend-activation` (ADMIN).
- [ ] Tạo `PasswordPolicy` (≥ 8 ký tự, có chữ và số, không chứa username) áp dụng ở: kích hoạt, đổi mật khẩu, reset mật khẩu (P2-05), tạo user có mật khẩu tạm.
- [ ] `changePassword`: không cho mật khẩu mới trùng mật khẩu cũ.
- [ ] Login với user `mustChangePassword=true` → trả cờ để frontend bắt đổi mật khẩu trước khi vào hệ thống.

*Frontend:*
- [ ] Xóa form đăng ký trong `FE/pages/LoginPage.jsx` (`isRegistering`, `handleRegisterSubmit`, `renderRegisterForm`, nút "Đăng ký ngay") và hàm `register` trong `FE/api/auth.js`.
- [ ] Thay bằng dòng hướng dẫn: "Chưa có tài khoản? Liên hệ quản trị viên khoa."
- [ ] Trang/modal **Kích hoạt tài khoản** (có thể dùng chung component với Quên mật khẩu ở P2-05).
- [ ] Màn hình **bắt buộc đổi mật khẩu** khi `mustChangePassword=true`.
- [ ] `AdminPage`: bỏ bắt buộc ô mật khẩu khi tạo user; thêm nút "Gửi lại email kích hoạt"; cột trạng thái "Chưa kích hoạt".
- [ ] Hiển thị yêu cầu mật khẩu (checklist trực quan) ở mọi form đặt mật khẩu.

**✅ Nghiệm thu:**
- `POST /api/auth/register` → 404; trang login không còn nút đăng ký.
- Admin tạo user không có mật khẩu → user nhận email → tự kích hoạt → đăng nhập được.
- Đặt mật khẩu `123` → 400 với thông báo rõ ràng.

**Theo dõi:** Trạng thái: ⬜ · Ngày xong: `____` · Commit: `____`

---

### ⬜ P1-10 — Chống brute-force đăng nhập (bền vững)

**Mức độ:** 🟡 Trung bình
**Vấn đề:** `AuthController` khóa theo username bằng `ConcurrentHashMap` tĩnh → mất khi restart, không chia sẻ giữa nhiều instance, và kẻ xấu có thể **cố tình khóa tài khoản người khác** (chỉ cần nhập sai 5 lần).

**Các bước:**
- [ ] Thêm thư viện **Bucket4j** (hoặc tự cài) — giới hạn theo **IP**: 10 lần login/phút; theo **username + IP**: 5 lần sai → khóa 10 phút.
- [ ] Lưu `failedAttempts`, `lockedUntil` vào DB (bảng `users`) thay vì RAM — hoặc giữ RAM nhưng có giới hạn theo IP để tránh DoS tài khoản.
- [ ] Áp dụng rate limit cho `/api/auth/otp/**` và `/api/auth/password/**` (P2-05).
- [ ] Đọc IP thật qua header `X-Forwarded-For` (nginx đã gửi) — cấu hình `server.forward-headers-strategy=native`.
- [ ] Ghi audit log `LOGIN_FAILED`, `LOGIN_LOCKED`, `LOGIN_SUCCESS`.

**✅ Nghiệm thu:** Restart backend không reset bộ đếm (nếu lưu DB); 11 request login/phút từ 1 IP → 429.
**Theo dõi:** Trạng thái: ⬜ · Ngày xong: `____` · Commit: `____`

---

### ⬜ P1-11 — Security headers, tắt Swagger ở prod

**Mức độ:** 🟡 Trung bình

**Các bước:**
- [ ] `frontend/nginx.conf`: thêm header
  ```nginx
  add_header X-Content-Type-Options "nosniff" always;
  add_header X-Frame-Options "DENY" always;
  add_header Referrer-Policy "strict-origin-when-cross-origin" always;
  add_header Content-Security-Policy "default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src https://fonts.gstatic.com; connect-src 'self' ws: wss:" always;
  ```
- [ ] `application-prod.properties`: `springdoc.api-docs.enabled=false`, `springdoc.swagger-ui.enabled=false`.
- [ ] **(Phát hiện ở GĐ0)** Swagger hiện bị `SecurityConfig` chặn 403 ở mọi môi trường → cho phép `/swagger-ui/**`, `/swagger-ui.html`, `/api-docs/**` **chỉ ở profile dev**; thêm cấu hình Bearer auth trong `OpenApiConfig` để thử API có token ngay trên Swagger.
- [ ] `application.properties`: `server.error.include-message` → chỉ `always` ở dev; prod `never` (tránh lộ thông tin nội bộ) — hoặc làm chuẩn ở P3-03.
- [ ] `/api/users/test` (endpoint thử nghiệm) → xóa.
- [ ] Kiểm tra CORS chỉ cho origin cấu hình (đã có) — thêm cho `/ws/**`.

**✅ Nghiệm thu:** `curl -I http://localhost:5175` thấy đủ header; `/swagger-ui.html` ở prod → 404.
**Theo dõi:** Trạng thái: ⬜ · Ngày xong: `____` · Commit: `____`

---

### ⬜ P1-12 — Bộ test hồi quy phân quyền

**Mức độ:** 🔴 Bắt buộc
**Mục tiêu:** Mỗi lỗ hổng GĐ1 có test tự động. Viết **song song** với P1-01 → P1-07.

**Cấu trúc:** `BE-TEST/security/` dùng `@SpringBootTest` + `@AutoConfigureMockMvc` + dữ liệu fixture (2 khoa, 2 GV, 3 SV, 2 đề tài).

**Danh sách test tối thiểu:**

| # | Test | Kỳ vọng |
|---|---|---|
| 1 | Token OTP_PENDING gọi `/api/users` | 401 |
| 2 | Verify OTP sai 6 lần | OTP bị vô hiệu |
| 3 | User bị khóa dùng token cũ | 401 |
| 4 | Token sau khi đổi mật khẩu (tv cũ) | 401 |
| 5 | GV A tạo/sửa/xóa milestone đề tài GV B | 403 |
| 6 | GV A xóa lịch đề tài GV B | 403 |
| 7 | SV không thuộc đề tài đọc/đăng thảo luận | 403 |
| 8 | SV gửi tin nhắn cho user ngoài đề tài | 403 |
| 9 | GV export không có topicId / topicId của người khác | 400 / 403 |
| 10 | SV gọi `/api/audit/recent` | 403 |
| 11 | SV gọi `/api/users/{id người khác}` | Không có email/phone |
| 12 | DeptAdmin khoa A truy cập workspace khoa B | 403 |
| 13 | SV tải file của SV khác đề tài | 403 |
| 14 | Upload file `.exe` | 400 |
| 15 | WS CONNECT không token | Bị từ chối |
| 16 | WS SUBSCRIBE `/topic/progress/{đề tài khác}` | Bị từ chối |
| 17 | GV B gửi notification tới SV của GV A | 403 |
| 18 | Response posts không chứa `email`/`privileges` | Pass |

**✅ Nghiệm thu:** 18/18 test pass; chạy trong CI.
**Theo dõi:** Trạng thái: ⬜ · Số test pass: `__/18`

---

### ⬜ P1-13 — Nâng cấp dependency backend + Dependabot

**Mức độ:** 🟡 Trung bình

| Thư viện | Hiện tại | Mục tiêu | Lưu ý |
|---|---|---|---|
| Spring Boot | 3.4.12 | 3.5.x (bản vá mới nhất) | Đọc release notes |
| jjwt | 0.11.5 | 0.12.x | **API thay đổi**: `parserBuilder()` → `parser().verifyWith(key)`, `setSubject` → `subject()` |
| Apache POI | 5.2.3 | 5.4.x | Có CVE ở bản cũ |
| springdoc-openapi | 2.3.0 | 2.8.x | Tương thích Boot 3.4/3.5 |
| dotenv-java | 3.0.0 | 3.x mới nhất | |

**Các bước:**
- [ ] Nâng từng thư viện một, chạy test sau mỗi lần.
- [ ] Viết lại `JwtTokenProvider` theo API jjwt 0.12.
- [ ] Tạo `.github/dependabot.yml` (maven `/Backend`, npm `/frontend`, github-actions `/`) — lịch hàng tuần.
- [ ] (Tùy chọn) Thêm job OWASP Dependency-Check hoặc bật GitHub CodeQL.

**✅ Nghiệm thu:** Build + toàn bộ test pass; Dependabot tạo PR tự động.
**Theo dõi:** Trạng thái: ⬜ · Ngày xong: `____` · Commit: `____`

---

### 🏁 Tiêu chí hoàn thành Giai đoạn 1

- [ ] Không thể bỏ qua OTP; tài khoản khóa bị chặn ngay.
- [ ] Không còn secret mặc định trong cấu hình prod.
- [ ] WebSocket yêu cầu xác thực + kiểm soát subscribe.
- [ ] 16 điểm IDOR ở P1-05 đã vá.
- [ ] Không controller nào trả entity JPA.
- [ ] Upload có whitelist + gắn chủ sở hữu.
- [ ] File cá nhân đã rời khỏi git **và lịch sử git** (D-04).
- [ ] Không còn đăng ký công khai; tài khoản mới kích hoạt qua email (D-02).
- [ ] Bộ test bảo mật 18/18 pass trong CI.
- [ ] Chạy lại smoke test P0-05 — không có hồi quy.
- [ ] Merge `develop` → `main`, tag `v0.2.0-security`.

---

## 6. GIAI ĐOẠN 2 — Sửa lỗi nghiệp vụ & chức năng

**🎯 Mục tiêu:** Mọi luồng nghiệp vụ trong tài liệu vận hành chạy đúng, trải nghiệm người dùng không bị gián đoạn.

**📌 Điều kiện bắt đầu:** GĐ1 hoàn thành (hoặc ít nhất P1-01, P1-05, P1-12). **P2-01, P2-02, P2-03 có thể làm sớm song song với GĐ1** vì chặn luồng chính.
**⏱️ Ước lượng:** 7–9 ngày.

---

### ⬜ P2-01 — Cấp quyền TOPIC_MANAGE cho giảng viên + sửa initializer

**Mức độ:** 🔴 Chặn chức năng
**Vấn đề:**
1. `BE/controller/TopicController.java` yêu cầu `hasAuthority('TOPIC_MANAGE') or hasRole('ADMIN')` cho tạo/sửa/xóa/mở/đóng đề tài.
2. `BE/config/RolePrivilegeInitializer.java:43` chỉ cấp `REGISTRATION_MANAGE` cho LECTURER → **giảng viên nhận 403** → frontend tự đăng xuất (xem P2-02).
3. Initializer gọi `setPrivileges(...)` **mỗi lần khởi động** → mọi chỉnh sửa quyền qua trang quản lý vai trò **bị ghi đè khi restart**.

**Các bước:**
- [ ] Xác nhận lỗi bằng kịch bản #5 ở P0-05.
- [ ] Thêm `TOPIC_MANAGE` vào quyền mặc định của LECTURER (logic chỉ sửa đề tài của mình đã có ở `TopicService.canModifyTopic`).
- [ ] Sửa initializer: **chỉ gán privileges mặc định khi role được tạo mới lần đầu**; role đã tồn tại → giữ nguyên (chỉ bổ sung privilege mới nếu cần qua migration).
  > Về lâu dài chuyển sang Flyway seed ở P3-01 và xóa initializer.
- [ ] Với DB đang có: viết script/migration thêm `TOPIC_MANAGE` cho LECTURER một lần.
- [ ] Test: LECTURER tạo đề tài trong workspace `OPEN_TOPIC` → 200; LECTURER sửa đề tài của GV khác → 403.

**✅ Nghiệm thu:** Giảng viên tạo/sửa/đóng/mở đề tài được; đổi quyền qua trang Role → restart → quyền vẫn giữ.
**Theo dõi:** Trạng thái: ⬜ · Ngày xong: `____` · Commit: `____`

---

### ⬜ P2-02 — Tách xử lý 401/403 ở frontend + toast lỗi

**Mức độ:** 🔴 Trải nghiệm nghiêm trọng
**Vấn đề:** `FE/api.js:57` — mọi 401 **và 403** đều xóa token và chuyển về `/login`. Bất kỳ thao tác nào bị từ chối quyền (kể cả "Out of scope" hợp lệ) đều đá người dùng ra. Commit `4760069` đã phải vá cục bộ lỗi này cho Kanban.

**Các bước:**
- [ ] `api.js`: **401** → logout + redirect login (kèm `?reason=expired`); **403** → chỉ reject lỗi với message "Bạn không có quyền thực hiện thao tác này".
- [ ] Xóa cơ chế "retry direct backend" phức tạp (`canRetryDirect`) nếu không còn cần — dev đã có Vite proxy; kiểm tra kỹ trước khi xóa.
- [ ] Tạo `FE/components/Toast.jsx` + `ToastContext` hiển thị lỗi toàn cục (hoặc dùng thư viện `sonner`/`react-hot-toast`).
- [ ] Trang login: hiển thị "Phiên đăng nhập đã hết hạn" khi `reason=expired`.
- [ ] Tự động logout khi token hết hạn: decode `exp` trong JWT, đặt timer.
- [ ] Rà các trang đang tự bắt 403 riêng (Kanban) → dùng cơ chế chung.

**✅ Nghiệm thu:** SV cố truy cập tài nguyên không có quyền → thấy thông báo lỗi, **vẫn đăng nhập**.
**Theo dõi:** Trạng thái: ⬜ · Ngày xong: `____` · Commit: `____`

---

### ⬜ P2-03 — Sửa lỗi upload/tải file đính kèm

**Mức độ:** 🔴 Chặn chức năng
**Vấn đề (phát hiện khi đối chiếu code):**
1. `FE/pages/ProjectSpacePage.jsx:126` — `fileUrl = await fileApi.upload(file)` nhưng `fileApi.upload` trả **object** `{fileName, fileUrl}` → gửi object vào body `Map<String,String>` → backend lỗi **400** khi nộp báo cáo có file từ trang này.
2. `FE/components/ProgressRow.jsx:49`, `FE/pages/ProjectSpacePage.jsx:454` — tải file bằng `<a href={fileUrl}>` → trình duyệt **không gửi header Authorization** → `/api/secure/files/...` trả **401** → không tải được file.
3. `frontend/nginx.conf` không đặt `client_max_body_size` → nginx mặc định **1MB** → upload file > 1MB qua Docker bị **413**.
4. Vite proxy `/uploads` thừa (backend đã chặn truy cập trực tiếp).

**Các bước:**
- [ ] Sửa `ProjectSpacePage.jsx`: `const up = await fileApi.upload(file); fileUrl = up.fileUrl;` (hoặc dùng `fileId` sau P1-07).
- [ ] Tạo `fileApi.download(fileUrl, fileName)`: gọi axios với `responseType: 'blob'` → tạo `URL.createObjectURL` → tự click tải.
- [ ] Thay mọi `<a href={fileUrl}>` bằng nút gọi `fileApi.download`.
- [ ] `nginx.conf`: thêm `client_max_body_size 25m;` (khớp giới hạn backend ở P1-07).
- [ ] Xóa proxy `/uploads` trong `vite.config.js`.
- [ ] Test thủ công kịch bản #11, #12, #13, #21 ở P0-05.

**✅ Nghiệm thu:** Nộp báo cáo kèm file 10MB từ **cả hai trang** qua Docker thành công; GV tải được file, tên file đúng.
**Theo dõi:** Trạng thái: ⬜ · Ngày xong: `____` · Commit: `____`

---

### ⬜ P2-04 — Mô hình nhiều sinh viên/đề tài (capacity)

**Mức độ:** 🟠 Cao
**Vấn đề:** `BE/service/TopicRegistrationService.java:158` — nếu đề tài đã có 1 đăng ký được duyệt thì từ chối duyệt thêm; đồng thời set `topic.status = "REGISTERED"` khiến SV khác không đăng ký được. Trường `capacity` gần như vô nghĩa. Mâu thuẫn với mục tiêu "quản lý công việc nhóm".

**✅ Quyết định D-01 = B:** Nhiều sinh viên độc lập trên một đề tài theo `capacity`, chấm điểm riêng từng sinh viên. Mô hình Team **để dành phát triển sau v1.0** (P4-04 🕒).

> 🧩 **Yêu cầu tương thích với Team sau này:** Giữ `TopicRegistration` là bản ghi **theo từng sinh viên** (không gộp). Khi làm P4-04 chỉ cần thêm cột `team_id NULL` vào `topic_registration` và bảng `teams` — không phải viết lại luồng đăng ký/duyệt/chấm điểm của P2-04.

**Thiết kế:**
- `capacity` = số SV tối đa của đề tài (mặc định 1, tối đa ví dụ 5).
- Trạng thái đề tài: `OPEN` (còn chỗ) → `FULL` (đủ SV được duyệt) → `CLOSED` (GV đóng) → `COMPLETED` (đã chấm hết).
- Được đăng ký khi `status=OPEN`; được duyệt khi `approvedCount < capacity`; duyệt đủ → `FULL`; SV bị hủy/từ chối khiến còn chỗ → quay lại `OPEN`.
- Chấm điểm **từng sinh viên** (đã là theo registration — giữ nguyên).
- Báo cáo tiến độ vẫn theo từng SV; GV xem theo đề tài (đã có). Kanban nhóm: hiển thị báo cáo của cả nhóm (tùy chọn).

**Các bước:**
- [ ] Bỏ điều kiện `existsByTopic_IdAndApprovedTrue` trong `approve()`.
- [ ] Chống vượt capacity khi 2 GV/2 request duyệt đồng thời: thêm `TopicRepository.findByIdForUpdate` với `@Lock(PESSIMISTIC_WRITE)`.
- [ ] Cập nhật trạng thái `FULL`/`OPEN` sau approve/cancel/reject.
- [ ] `grade()`: chỉ đặt `COMPLETED` khi **tất cả** SV đã duyệt đều có điểm.
- [ ] Thêm unique constraint `(student_id, topic_id)` cho `topic_registration`.
- [ ] Frontend: hiển thị "2/3 chỗ" trên `TopicCard`, trạng thái "Đã đủ" ; trang GV hiển thị danh sách thành viên.
- [ ] Cập nhật test `TopicRegistrationServiceTest` (hiện có thể đang test hành vi 1 SV).

**✅ Nghiệm thu:** Đề tài capacity=3 duyệt được 3 SV, SV thứ 4 không đăng ký được; hủy 1 → mở lại.
**Theo dõi:** Trạng thái: ⬜ · Ngày xong: `____` · Commit: `____`

---

### ⬜ P2-05 — Chức năng Quên mật khẩu

**Mức độ:** 🟠 Cao
**Vấn đề:** Nút "Quên mật khẩu?" ở `FE/pages/LoginPage.jsx:175` không có chức năng; tài liệu vận hành mục 8 mô tả tính năng này.

**Thiết kế:**
```
POST /api/auth/password/forgot  {identifier: username hoặc email}
     → luôn trả 200 "Nếu tài khoản tồn tại, mã đã được gửi" (chống dò tài khoản)
POST /api/auth/password/reset   {identifier, code, newPassword}
     → kiểm tra OTP purpose=RESET_PASSWORD, áp PasswordPolicy, tăng tokenVersion
```

**Các bước:**
- [ ] Backend: 2 endpoint trên, dùng lại `OtpService` với `purpose=RESET_PASSWORD` (đã chuẩn bị ở P1-01), OTP hạn 10 phút.
- [ ] Rate limit (P1-10): 3 yêu cầu/15 phút/identifier.
- [ ] Audit log `PASSWORD_RESET_REQUESTED`, `PASSWORD_RESET_DONE`.
- [ ] Email tiếng Việt có hướng dẫn.
- [ ] Frontend: modal/trang 3 bước (nhập email → nhập mã + mật khẩu mới → thành công).
- [ ] Test: reset thành công → token cũ vô hiệu; mã sai/hết hạn → 400.

**✅ Nghiệm thu:** Người dùng tự lấy lại mật khẩu qua email mà không cần admin.
**Theo dõi:** Trạng thái: ⬜ · Ngày xong: `____` · Commit: `____`

---

### ⬜ P2-06 — Gửi email bất đồng bộ + template tiếng Việt

**Mức độ:** 🟡 Trung bình
**Vấn đề:** `EmailService.sendEmail` có `@Async` nhưng ứng dụng **không có `@EnableAsync`** → gửi đồng bộ → đăng nhập ADMIN/GV chờ SMTP (2–5 giây), duyệt đăng ký chậm. Email gửi **trong transaction** → transaction rollback thì email vẫn đi. Nội dung email tiếng Anh, text thô.

**Các bước:**
- [ ] Tạo `BE/config/AsyncConfig.java`: `@EnableAsync` + `ThreadPoolTaskExecutor` (core 2, max 5, queue 100, tên thread `mail-`).
- [ ] Chuyển gửi email sang **domain event**: publish `RegistrationApprovedEvent`... → `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async`.
- [ ] Template HTML tiếng Việt (Thymeleaf hoặc chuỗi template đơn giản): OTP, duyệt/từ chối đăng ký, nhận xét báo cáo, chấm điểm, nhắc nhở, reset mật khẩu.
- [ ] Thay `System.out/err` bằng SLF4J logger.
- [ ] Retry 2 lần khi lỗi SMTP tạm thời (Spring Retry — tùy chọn).
- [ ] Tắt gửi email thật trong test (mock `JavaMailSender`).

**✅ Nghiệm thu:** Đăng nhập ADMIN trả response < 500ms; email tiếng Việt có định dạng.
**Theo dõi:** Trạng thái: ⬜ · Ngày xong: `____` · Commit: `____`

---

### ⬜ P2-07 — Sửa scheduler nhắc nhở + nhắc hạn milestone

**Mức độ:** 🟡 Trung bình
**Vấn đề:** `BE/scheduler/ReminderScheduler.java:40` — `regRepo.findAll()` rồi truy vấn từng đăng ký (N+1); **gửi lặp lại mỗi ngày** cho cùng một SV; không lọc workspace đang `IN_PROGRESS`; múi giờ phụ thuộc server. Tài liệu hứa "nhắc khi sắp đến hạn milestone" nhưng chưa có.

**Các bước:**
- [ ] Viết query JPQL: các đăng ký `approved=true`, workspace `IN_PROGRESS`, báo cáo gần nhất > 7 ngày hoặc chưa có.
- [ ] Chống trùng: bỏ qua nếu đã có notification `type=REMINDER, refType=TOPIC_REGISTRATION, refId=regId` trong 7 ngày gần nhất.
- [ ] `@Scheduled(cron = "0 0 9 * * *", zone = "Asia/Ho_Chi_Minh")`.
- [ ] Job mới: nhắc milestone còn **2 ngày** và **ngày cuối** tới hạn → gửi cho SV của đề tài (chống trùng theo `refType=MILESTONE`).
- [ ] Tùy chọn: gửi kèm email (dùng P2-06).
- [ ] Unit test logic chọn đối tượng nhắc.

**✅ Nghiệm thu:** Chạy job 2 lần liên tiếp → mỗi SV chỉ nhận 1 thông báo.
**Theo dõi:** Trạng thái: ⬜ · Ngày xong: `____` · Commit: `____`

---

### ⬜ P2-08 — Kiểm tra trùng lặp hỗ trợ tiếng Việt

**Mức độ:** 🟢 Thấp
**Vấn đề:** `BE/service/SimilarityService.java:30` dùng regex `[^a-z0-9\s]` → xóa mọi ký tự có dấu → "Xây dựng hệ thống" thành "x y d ng h th ng" → kết quả vô nghĩa. Chỉ so với báo cáo cũ **của chính SV đó**; kết quả chỉ ghi audit log, GV không thấy.

**Các bước:**
- [ ] Đổi regex thành `[^\p{L}\p{N}\s]` (giữ chữ Unicode) + chuẩn hóa `Normalizer.Form.NFC`.
- [ ] Bỏ stopword tiếng Việt phổ biến (và, của, là, các, những…) — danh sách ngắn.
- [ ] Mở rộng phạm vi so sánh: báo cáo của **SV khác trong cùng workspace**.
- [ ] Thêm cột `similarity_score` + `similar_to_report_id` vào `progress_report`.
- [ ] Frontend (trang GV): badge cảnh báo "Trùng lặp 85% với báo cáo #…".
- [ ] Unit test với văn bản tiếng Việt.

**✅ Nghiệm thu:** 2 báo cáo tiếng Việt gần giống → điểm ≥ 0,8 và GV thấy cảnh báo.
**Theo dõi:** Trạng thái: ⬜ · Ngày xong: `____` · Commit: `____`

---

### ⬜ P2-09 — Ràng buộc nghiệp vụ (điểm, trạng thái, ngày tháng)

**Mức độ:** 🟡 Trung bình

| # | Vấn đề | Vị trí | Sửa |
|---|---|---|---|
| 1 | Điểm không giới hạn, `body.get("score")` null → NPE 500 | `TopicRegistrationService.grade` | 0 ≤ score ≤ 10, bước 0,25/0,5; thiếu → 400 |
| 2 | `updateTopic` cho phép ghi đè `status` tùy ý | `TopicService.updateTopic:83` | Bỏ — chỉ đổi qua open/close |
| 3 | `capacity` âm/0 | `TopicService` | 1 ≤ capacity ≤ 10 |
| 4 | `RuntimeException("Topic not found")` → 500 | `TopicRegistrationService` (nhiều chỗ) | `ResponseStatusException(404)` |
| 5 | `cancel` ném `RuntimeException` | như trên | 400 với thông báo rõ |
| 6 | Deadline sai định dạng bị **bỏ qua im lặng** | `ProgressController` | Trả 400 |
| 7 | Milestone: deadline < startDate, ngoài khoảng workspace | `MilestoneController` | Validate |
| 8 | Workspace: `endAt < startAt` | `WorkspaceService` | Validate |
| 9 | `Long.valueOf(body.get("recipientId"))` null → 500 | `MessageController`, `DiscussionController`… | 400 |
| 10 | Nội dung tin nhắn/bài thảo luận rỗng hoặc quá dài | Message, Discussion | 1–5000 ký tự |
| 11 | Chấm điểm khi workspace `CLOSED` vẫn được phép | `grade()` | **D-08:** chỉ chấm khi `IN_PROGRESS`; khi `CLOSED` → điểm bị khóa, chấm lại qua quy trình ở **P2-13** |
| 12 | Tạo user: email không đúng định dạng | `UserController.create` | Validate email |

**Các bước:**
- [ ] Sửa lần lượt 12 mục (tick trong bảng khi xong): 1 ⬜ 2 ⬜ 3 ⬜ 4 ⬜ 5 ⬜ 6 ⬜ 7 ⬜ 8 ⬜ 9 ⬜ 10 ⬜ 11 ⬜ 12 ✅
- [ ] Mỗi mục có unit test.
- [ ] Frontend: validate tương ứng ở form (min/max, required) để báo lỗi sớm.

> Ghi chú: P3-02 sẽ chuẩn hóa bằng Bean Validation; ở đây sửa **logic** trước để người dùng không gặp lỗi 500.

**✅ Nghiệm thu:** Không còn lỗi 500 do dữ liệu đầu vào ở các endpoint trên.
**Theo dõi:** Trạng thái: ⬜ · Ngày xong: `____` · Commit: `____`

---

### ⬜ P2-10 — Route cho RoleManagementPage + chặn xóa role đang dùng

**Mức độ:** 🟢 Thấp
**Vấn đề:** `FE/pages/RoleManagementPage.jsx` (314 dòng) được import trong `App.jsx` nhưng **không có route** → không truy cập được. `RoleService.deleteRole` không kiểm tra user đang dùng role → lỗi khóa ngoại 500; có thể xóa cả role hệ thống.

**Các bước:**
- [ ] Thêm route `/admin/roles` (`RequireRole role="ADMIN"`) + mục menu sidebar cho ADMIN.
- [ ] Hiển thị `loading`/`error` state (đang bị lint báo không dùng).
- [ ] `deleteRole`: chặn xóa 4 role hệ thống (`ADMIN`, `DEPARTMENT_ADMIN`, `LECTURER`, `STUDENT`); chặn xóa nếu `userRepo.existsByRole_Id(id)` → 400 kèm số user.
- [ ] Test cho `deleteRole`.

**✅ Nghiệm thu:** Admin vào được trang quản lý vai trò; xóa role đang dùng → thông báo rõ ràng.
**Theo dõi:** Trạng thái: ⬜ · Ngày xong: `____` · Commit: `____`

---

### ⬜ P2-11 — Xóa tính năng ProjectPermission

**Mức độ:** 🟢 Thấp
**Vấn đề:** Entity `ProjectPermission` + API `/api/permissions` cho phép cấu hình quyền theo công cụ (MILESTONE, SUBMISSION, DISCUSSION…) **nhưng không nơi nào trong code áp dụng** các quyền này, và frontend không có giao diện. Đây là tính năng "treo".

**✅ Quyết định D-07:** Xóa (hiện chưa có nhu cầu rõ ràng). Nếu sau này có nhu cầu, thiết kế lại từ đầu dựa trên `TopicAccessService` (P1-05).

**Các bước:**
- [ ] Xác nhận frontend không gọi `/api/permissions` (đã kiểm tra: không có).
- [ ] Kiểm tra DB dev: bảng `project_permissions` có dữ liệu không → nếu có, export lưu lại trước khi xóa.
- [ ] Xóa `BE/entity/ProjectPermission.java`, `BE/repository/ProjectPermissionRepository.java`, `BE/controller/ProjectPermissionController.java`.
- [ ] Xóa bảng: trước P3-01 thì `DROP TABLE project_permissions` thủ công trên DB dev (vì `ddl-auto=update` không tự xóa bảng); sau P3-01 thì viết migration `DROP TABLE IF EXISTS project_permissions`.
- [ ] Cập nhật tài liệu API/Swagger, xóa khỏi `TECHNICAL_REPORT.md` nếu có nhắc.
- [ ] Build + test pass.

**✅ Nghiệm thu:** Không còn class/bảng `ProjectPermission`; `/api/permissions` → 404.
**Theo dõi:** Trạng thái: ⬜ · Ngày xong: `____` · Commit: `____`

---

### ⬜ P2-12 — Kiểm thử hồi quy nghiệp vụ toàn luồng

**Mức độ:** 🔴 Bắt buộc

**Các bước:**
- [ ] Viết test tích hợp `BE-TEST/flow/FullLifecycleIT.java` đi hết vòng đời: tạo workspace → OPEN_TOPIC → GV tạo đề tài → gán lớp → OPEN_REGISTRATION → 2 SV đăng ký → GV duyệt → LOCK → IN_PROGRESS → SV nộp báo cáo → GV nhận xét → GV chấm điểm → CLOSED.
- [ ] Chạy lại toàn bộ bảng smoke test P0-05 → cột "Thực tế" phải khớp "Kỳ vọng" 21/21.
- [ ] Ghi video demo ngắn (tùy chọn — hữu ích cho báo cáo).

**✅ Nghiệm thu:** Test vòng đời pass; smoke test 21/21.
**Theo dõi:** Trạng thái: ⬜ · Kết quả smoke test: `__/21`

> 📌 Nên làm P2-13 **trước** P2-12 để test vòng đời bao gồm cả khóa điểm và chấm lại.

---

### ⬜ P2-13 — Khóa điểm & quy trình chấm lại

**Mức độ:** 🟠 Cao
**Căn cứ:** Quyết định **D-08** — khi workspace `CLOSED` thì **khóa điểm**; nhưng vẫn **có thể chấm lại/chỉnh sửa** khi nghiệp vụ yêu cầu, thông qua quy trình có kiểm soát.
**Hiện trạng:** `TopicRegistrationService.grade()` cho chấm/sửa điểm tự do cả ở `IN_PROGRESS` lẫn `CLOSED`, không lưu lịch sử thay đổi điểm (chỉ có audit log dạng chữ).

**Quy tắc nghiệp vụ:**

| Trạng thái workspace | Giảng viên chấm/sửa điểm | Ghi chú |
|---|---|---|
| `DRAFT` → `LOCK_REGISTRATION` | ❌ | Chưa đến giai đoạn chấm |
| `IN_PROGRESS` | ✅ Tự do (đề tài mình hướng dẫn) | Mọi lần sửa đều lưu lịch sử |
| `CLOSED` | 🔒 **Khóa** | Chỉ sửa được khi có **yêu cầu chấm lại đã được duyệt** và còn hạn |

**Luồng chấm lại:**
```
[GV] Gửi yêu cầu chấm lại cho 1 SV (bắt buộc nêu lý do)
        │  → thông báo cho DeptAdmin của khoa
        ▼
[DeptAdmin / Admin] Duyệt hoặc Từ chối (kèm ghi chú)
        │  Duyệt → mở khóa điểm của SV đó trong N giờ (mặc định 72h)
        ▼
[GV] Sửa điểm/nhận xét (bắt buộc nêu lý do chỉnh sửa)
        │  → yêu cầu chuyển USED, điểm tự khóa lại
        │  → SV nhận thông báo "Điểm đã được cập nhật"
        ▼
Hết N giờ mà GV không sửa → yêu cầu chuyển EXPIRED, điểm vẫn khóa
```
> **Tùy chọn mở rộng (làm sau nếu nghiệp vụ cần):** SV gửi **đơn phúc khảo** → GV/DeptAdmin xem xét → nếu chấp nhận thì tạo yêu cầu chấm lại như trên.

**Thiết kế dữ liệu (migration mới):**
```
grade_revision_requests(
  id, registration_id FK, requested_by FK users, reason TEXT NOT NULL,
  status ENUM(PENDING, APPROVED, REJECTED, USED, EXPIRED),
  reviewed_by FK users NULL, reviewed_at, review_note,
  unlock_until DATETIME NULL, created_at)

grade_history(
  id, registration_id FK, old_score, new_score, old_feedback, new_feedback,
  changed_by FK users, reason TEXT, revision_request_id FK NULL, changed_at)
```

**API mới:**

| Method | Endpoint | Quyền | Mô tả |
|---|---|---|---|
| POST | `/api/registration/{regId}/grade-revisions` | LECTURER (Own) | Gửi yêu cầu; chỉ khi workspace `CLOSED`, SV đã có điểm, không có yêu cầu PENDING khác |
| GET | `/api/grade-revisions?status=PENDING` | DEPT_ADMIN (Dept), ADMIN | Danh sách yêu cầu, phân trang |
| POST | `/api/grade-revisions/{id}/approve` | DEPT_ADMIN (Dept), ADMIN | Body: `{note, unlockHours}` (mặc định 72, tối đa 168) |
| POST | `/api/grade-revisions/{id}/reject` | DEPT_ADMIN (Dept), ADMIN | Body: `{note}` bắt buộc |
| GET | `/api/registration/{regId}/grade-history` | LECTURER (Own), DEPT_ADMIN (Dept), ADMIN | Lịch sử thay đổi điểm |
| POST | `/api/registration/grade/{regId}` (sửa) | LECTURER (Own) | Thêm trường `reason` (bắt buộc khi sửa điểm đã có) |

**Các bước:**

*Backend:*
- [ ] Entity + repository `GradeRevisionRequest`, `GradeHistory` + migration (hoặc tạo bằng `ddl-auto` nếu làm trước P3-01, rồi đưa vào migration ở P3-01).
- [ ] `GradeService` (tách khỏi `TopicRegistrationService`): hàm `canEditGrade(registration)` theo bảng quy tắc trên.
- [ ] Sửa `grade()`:
  - `IN_PROGRESS` → cho phép; nếu đã có điểm thì bắt buộc `reason`.
  - `CLOSED` → chỉ cho phép khi có yêu cầu `APPROVED` và `now < unlock_until`; sau khi lưu → đặt yêu cầu thành `USED`.
  - Mọi lần lưu → ghi `grade_history`.
- [ ] Không cho duyệt yêu cầu của chính mình (người duyệt ≠ người gửi — trường hợp Admin kiêm GV).
- [ ] Job định kỳ (mỗi giờ): chuyển yêu cầu `APPROVED` quá `unlock_until` sang `EXPIRED`.
- [ ] Thông báo + email: DeptAdmin nhận yêu cầu mới; GV nhận kết quả duyệt/từ chối; SV nhận thông báo khi điểm thay đổi (hiển thị điểm cũ → mới).
- [ ] Audit log: `GRADE_REVISION_REQUESTED`, `GRADE_REVISION_APPROVED`, `GRADE_REVISION_REJECTED`, `GRADE_CHANGED`.
- [ ] Xuất Excel (`ExportController`): thêm cột "Đã chấm lại (số lần)".

*Frontend:*
- [ ] `LecturerRegistrationPage`: điểm của workspace đã đóng hiển thị badge 🔒 "Đã khóa"; nút "Yêu cầu chấm lại" → modal nhập lý do; hiển thị trạng thái yêu cầu (Chờ duyệt / Được mở đến hh:mm dd/MM / Bị từ chối + ghi chú).
- [ ] Khi đang được mở khóa: cho sửa điểm, bắt buộc ô "Lý do chỉnh sửa".
- [ ] `DepartmentAdminPage`: tab mới "Yêu cầu chấm lại" (danh sách chờ duyệt, xem lịch sử điểm, Duyệt/Từ chối).
- [ ] Modal "Lịch sử điểm" (bảng: thời gian, người sửa, điểm cũ → mới, lý do).
- [ ] `StudentMyRegistrationsPage`: hiển thị ghi chú "Điểm đã được cập nhật ngày …" nếu có thay đổi.

*Test:*
- [ ] Chấm khi `IN_PROGRESS` → OK + có bản ghi lịch sử.
- [ ] Chấm khi `CLOSED` không có yêu cầu → 400/403 "Điểm đã bị khóa".
- [ ] Gửi yêu cầu → DeptAdmin khoa khác duyệt → 403.
- [ ] Duyệt → sửa trong hạn → OK, yêu cầu thành `USED`, sửa lần 2 → bị khóa.
- [ ] Duyệt → quá hạn → `EXPIRED`, sửa → bị khóa.
- [ ] Gửi 2 yêu cầu PENDING cùng lúc cho 1 SV → yêu cầu thứ 2 bị từ chối.

**✅ Nghiệm thu:** Không thể sửa điểm ở workspace đã đóng nếu không có yêu cầu được duyệt; mọi thay đổi điểm đều truy vết được (ai, khi nào, lý do, ai duyệt).
**Theo dõi:** Trạng thái: ⬜ · Ngày xong: `____` · Commit: `____`

---

### 🏁 Tiêu chí hoàn thành Giai đoạn 2

- [ ] Giảng viên tạo và quản lý đề tài bình thường.
- [ ] 403 không còn đăng xuất người dùng.
- [ ] Upload/tải file hoạt động qua Docker.
- [ ] Nhiều SV/đề tài theo `capacity` đã triển khai (D-01 = B).
- [ ] Điểm bị khóa khi workspace đóng; quy trình chấm lại hoạt động, có lịch sử điểm (D-08).
- [ ] ProjectPermission đã được xóa (D-07).
- [ ] Quên mật khẩu hoạt động.
- [ ] Email bất đồng bộ, tiếng Việt.
- [ ] Nhắc nhở không trùng lặp; có nhắc hạn milestone.
- [ ] Không còn lỗi 500 do dữ liệu đầu vào ở luồng chính.
- [ ] Smoke test 21/21, test vòng đời pass.
- [ ] Tag `v0.3.0-business`.

---

## 7. GIAI ĐOẠN 3 — Củng cố kỹ thuật

**🎯 Mục tiêu:** Mã nguồn dễ bảo trì, dễ mở rộng, có kiểm thử và giám sát — nền móng cho GĐ4.

**📌 Điều kiện bắt đầu:** GĐ2 hoàn thành.
**⏱️ Ước lượng:** 12–15 ngày.
**⚠️ Nguyên tắc:** Refactor **không thay đổi hành vi**. Mỗi task refactor phải có test bao phủ **trước** khi sửa.

**Thứ tự đề xuất:** P3-09 (test nền) → P3-01 → P3-03 → P3-02 → P3-04 → P3-05 → P3-06 → P3-07 → P3-08 → P3-10 → P3-11 → P3-14 → P3-12 → P3-13.

---

### ⬜ P3-01 — Flyway migration, bỏ ddl-auto & SchemaFixer

**Vấn đề:** `spring.jpa.hibernate.ddl-auto=update` (`BE-RES/application.properties:6`) — schema thay đổi ngầm, không thể rollback, không có lịch sử. `BE/SchemaFixer.java` chạy `ALTER TABLE` ở **mọi profile** mỗi lần khởi động. `RolePrivilegeInitializer` ghi dữ liệu mỗi lần khởi động.

**Các bước:**
- [ ] Thêm `flyway-core` + `flyway-mysql`.
- [ ] Xuất schema hiện tại từ DB dev: `mysqldump --no-data doan_ltmmt > V1__baseline.sql` → làm sạch → `BE-RES/db/migration/V1__baseline.sql`.
- [ ] `spring.flyway.baseline-on-migrate=true`, `baseline-version=1` (cho DB đang chạy).
- [ ] `V2__topic_registration_approved_nullable.sql` (thay SchemaFixer) → xóa `SchemaFixer.java`.
- [ ] `V3__seed_roles_privileges.sql` (INSERT IGNORE) → xóa/đơn giản hóa `RolePrivilegeInitializer`.
- [ ] Các thay đổi schema từ GĐ1–2 (`token_version`, `stored_files`, `similarity_score`, `attempts`, `purpose`, unique constraint…) viết thành migration V4, V5….
- [ ] `ddl-auto=validate` cho mọi profile.
- [ ] Test: DB trống → Flyway tạo đủ bảng → app khởi động; DB cũ → baseline → migrate OK.

**✅ Nghiệm thu:** Không còn `ddl-auto=update`, `SchemaFixer`; `flyway_schema_history` có đủ bản ghi.
**Theo dõi:** Trạng thái: ⬜ · Ngày xong: `____` · Commit: `____`

---

### ⬜ P3-02 — Request DTO + Bean Validation

**Vấn đề:** Hầu hết endpoint nhận `Map<String, String/Object>` → không có kiểu, không validate, dễ NPE, Swagger không mô tả được body. `TopicController` nhận thẳng entity `Topic` (mass assignment).

**Danh sách request DTO cần tạo** (`BE/dto/request/`):

| DTO | Endpoint |
|---|---|
| `RegisterRequest` | `/api/auth/register` (nếu giữ) |
| `OtpVerifyRequest`, `ForgotPasswordRequest`, `ResetPasswordRequest` | `/api/auth/**` |
| `CreateUserRequest`, `UpdateProfileRequest`, `ChangePasswordRequest` | `/api/users/**` |
| `CreateTopicRequest`, `UpdateTopicRequest` | `/api/topics/**` |
| `GradeRequest` | `/api/registration/grade` |
| `CreateProgressRequest`, `UpdateProgressStatusRequest`, `ProgressCommentRequest` | `/api/progress/**` |
| `WorkspaceRequest`, `TransitionRequest` | `/api/workspaces/**` |
| `MilestoneRequest`, `CalendarEventRequest` | `/api/milestones`, `/api/calendar` |
| `SendMessageRequest`, `CreateThreadRequest`, `CreatePostRequest`, `AnnouncementRequest` | chat, thảo luận, thông báo |
| `ClassRequest`, `DepartmentRequest`, `AssignmentRequest`, `WorkspaceClassRequest` | quản trị |

**Các bước:**
- [ ] Tạo DTO dạng Java `record` + annotation `@NotBlank`, `@Size`, `@Email`, `@Min/@Max`, `@DecimalMin/@DecimalMax`, `@Future`…
- [ ] Controller dùng `@Valid @RequestBody XxxRequest`.
- [ ] Thông báo lỗi tiếng Việt qua `ValidationMessages.properties`.
- [ ] Thêm `@Schema` (springdoc) cho Swagger.
- [ ] Kiểm tra frontend gửi đúng tên trường.

**✅ Nghiệm thu:** `grep "Map<String" BE/controller` = 0 (trừ trường hợp có lý do ghi chú).
**Theo dõi:** Trạng thái: ⬜ · Ngày xong: `____` · Commit: `____`

---

### ⬜ P3-03 — Global exception handler + chuẩn hóa lỗi

**Các bước:**
- [ ] Tạo `BE/exception/` gồm: `NotFoundException`, `ForbiddenException`, `BusinessException(code, message)`.
- [ ] `GlobalExceptionHandler` (`@RestControllerAdvice`) xử lý: exception tự định nghĩa, `ResponseStatusException`, `MethodArgumentNotValidException`, `HttpMessageNotReadableException`, `AccessDeniedException`, `DataIntegrityViolationException`, `Exception` (500 — log đầy đủ, trả message chung).
- [ ] Định dạng lỗi thống nhất:
  ```json
  {
    "timestamp": "2026-10-01T09:00:00",
    "status": 400,
    "code": "TOPIC_FULL",
    "message": "Đề tài đã đủ số lượng sinh viên",
    "path": "/api/registration/register",
    "errors": [{"field": "score", "message": "Điểm phải từ 0 đến 10"}]
  }
  ```
- [ ] Bảng mã lỗi nghiệp vụ (`docs/error-codes.md`).
- [ ] Frontend `api.js`: đọc `message` + `errors[]` → hiển thị lỗi theo từng trường form.
- [ ] Việt hóa thông báo lỗi còn tiếng Anh ("Out of scope", "Topic not found"…).

**✅ Nghiệm thu:** Mọi lỗi API cùng một định dạng; không lộ stacktrace.
**Theo dõi:** Trạng thái: ⬜ · Ngày xong: `____` · Commit: `____`

---

### ⬜ P3-04 — Chuyển trạng thái sang Enum

**Vấn đề:** Trạng thái là `String`, hàm `normalizeStatus` (chuyển `"OPEN"` → `"OPEN_TOPIC"`) bị **lặp ở 5 nơi** (`TopicService`, `TopicRegistrationService`, `WorkspaceService`, `ProgressController` ×2).

**Enum cần tạo:**

| Enum | Giá trị |
|---|---|
| `WorkspaceStatus` | DRAFT, OPEN_TOPIC, OPEN_REGISTRATION, LOCK_REGISTRATION, IN_PROGRESS, CLOSED |
| `TopicStatus` | OPEN, FULL, CLOSED, COMPLETED |
| `ProgressStatus` | TODO, IN_PROGRESS, DONE |
| `NotificationType` | INFO, REMINDER, SYSTEM |
| `Severity` | INFO, WARNING, CRITICAL |
| `CalendarEventType` | MEETING, MILESTONE, DEADLINE |
| `RegistrationStatus` (tùy chọn, thay `Boolean approved`) | PENDING, APPROVED, REJECTED, CANCELLED |

**Các bước:**
- [ ] Migration dữ liệu: `UPDATE workspaces SET status='OPEN_TOPIC' WHERE status='OPEN'`; `UPDATE topics SET status='FULL' WHERE status='REGISTERED'`.
- [ ] `@Enumerated(EnumType.STRING)` trên entity.
- [ ] Đưa logic chuyển trạng thái vào enum: `WorkspaceStatus.canTransitionTo(target)`.
- [ ] Xóa 5 hàm `normalizeStatus`.
- [ ] Frontend: file `FE/constants/status.js` dùng chung nhãn tiếng Việt.

**✅ Nghiệm thu:** Không còn chuỗi trạng thái "cứng" trong service; test chuyển trạng thái pass.
**Theo dõi:** Trạng thái: ⬜ · Ngày xong: `____` · Commit: `____`

---

### ⬜ P3-05 — Tách logic controller → service

**Vấn đề:** Nhiều controller gọi repository trực tiếp và chứa toàn bộ nghiệp vụ, ví dụ `ProgressController` (304 dòng), `UserController` (281 dòng).

| Service mới | Chuyển logic từ | Ghi chú |
|---|---|---|
| `ProgressService` | `ProgressController` | Tạo báo cáo, nhận xét, đổi trạng thái, thông báo |
| `UserService` | `UserController`, `AuthController.register` | |
| `AuthService` | `AuthController` | Login, OTP, reset mật khẩu |
| `MilestoneService` | `MilestoneController` | |
| `MessageService` | `MessageController` | |
| `DiscussionService` | `DiscussionController` | |
| `NotificationService` | `NotificationController`, code tạo notification lặp ở ≥ 4 nơi | Hàm chung `notify(user, title, content, ref, link)` |
| `ExportService` | `ExportController` | |
| `DashboardService` | `DashboardController` | |
| `FileStorageService` | `FileUploadController`, `SecureFileController` | (đã tạo ở P1-07) |

**Các bước:**
- [ ] Mỗi service: viết unit test (Mockito) **trước**, sau đó chuyển code.
- [ ] Controller chỉ còn: nhận request DTO → gọi service → trả response DTO.
- [ ] `@Transactional` đặt ở service (hiện nhiều thao tác ghi nhiều bảng trong controller **không có transaction**).
- [ ] Thay FQCN rải rác (`com.doanltmmt.Backend.service.AuditLogService` trong constructor) bằng import thường.
- [ ] Thay `SecurityContextHolder...getName()` lặp lại bằng `scope.requireCurrentUser()`.

**✅ Nghiệm thu:** Không controller nào inject `*Repository`; mỗi controller < 120 dòng.
**Theo dõi:** Trạng thái: ⬜ · Ngày xong: `____` · Commit: `____`

---

### ⬜ P3-06 — Phân trang API danh sách

**Endpoint cần phân trang:**

| Endpoint | Sắp xếp mặc định |
|---|---|
| `GET /api/notifications/mine` | createdAt DESC |
| `GET /api/audit/recent` → đổi thành `GET /api/audit` + lọc actor/action/từ ngày–đến ngày | timestamp DESC |
| `GET /api/users`, `/api/users/by-department` + tìm kiếm | id DESC |
| `GET /api/messages/inbox` + `GET /api/messages/conversation/{userId}` | createdAt DESC |
| `GET /api/progress/topic/{id}` | createdAt DESC |
| `GET /api/discuss/threads/{id}/posts` | createdAt ASC |

**Các bước:**
- [ ] Tạo `PageResponse<T>(items, page, size, totalElements, totalPages)` — thống nhất với định dạng `TopicService.getAll` đang dùng.
- [ ] Giới hạn `size` tối đa 100.
- [ ] `/api/notifications/recent`: dùng query `LIMIT` thay vì tải hết rồi `stream().limit()`.
- [ ] Frontend: component `Pagination` dùng chung / "Tải thêm" cho thông báo & chat.

**✅ Nghiệm thu:** Không endpoint danh sách nào trả toàn bộ bảng.
**Theo dõi:** Trạng thái: ⬜ · Ngày xong: `____` · Commit: `____`

---

### ⬜ P3-07 — Tối ưu truy vấn & index

**Vấn đề:**
- `TopicService.getAll`: mỗi đề tài chạy **4 truy vấn count** + 1 truy vấn trạng thái SV → trang 6 đề tài = ~31 truy vấn.
- Nhiều quan hệ `@ManyToOne` mặc định EAGER; `User.department`, `User.role` EAGER.

**Các bước:**
- [ ] Bật `spring.jpa.properties.hibernate.generate_statistics=true` ở dev để đo trước/sau.
- [ ] `getAll`: 1 truy vấn gom nhóm `SELECT topic_id, SUM(approved IS NULL), SUM(approved=1)… GROUP BY topic_id WHERE topic_id IN (:ids)`.
- [ ] Đổi `@ManyToOne` sang `LAZY` + `@EntityGraph`/`JOIN FETCH` ở query cần.
- [ ] Thêm index (qua Flyway):
  - `topic_registration(student_id, topic_id)`, `topic_registration(topic_id, approved)`
  - `notifications(user_id, read_at)`, `notifications(user_id, created_at)`
  - `progress_report(topic_id, created_at)`, `progress_report(student_id, topic_id)`
  - `messages(sender_id, recipient_id, created_at)`
  - `audit_logs(timestamp)`, `audit_logs(actor)`
- [ ] Ghi kết quả đo trước/sau vào bảng dưới.

| Thao tác | Số query trước | Sau | Thời gian trước | Sau |
|---|---|---|---|---|
| Danh sách đề tài (6 mục) | ~31 | | | |
| Thông báo của tôi | | | | |
| Scheduler nhắc nhở (100 đăng ký) | | | | |

**Theo dõi:** Trạng thái: ⬜ · Ngày xong: `____` · Commit: `____`

---

### ⬜ P3-08 — Logging chuẩn + Actuator health

**Các bước:**
- [ ] Thay toàn bộ `System.out/err` bằng SLF4J (`@Slf4j` của Lombok).
- [ ] `logback-spring.xml`: dev dạng text màu; prod dạng **JSON** (logstash-encoder) + file xoay vòng.
- [ ] Filter gắn `X-Request-Id` (MDC) cho mỗi request → log dễ truy vết.
- [ ] Không log dữ liệu nhạy cảm (mật khẩu, OTP, token).
- [ ] Thêm `spring-boot-starter-actuator`: chỉ mở `health`, `info` (prod); `health` kiểm tra DB + mail.
- [ ] `docker-compose.yml`: healthcheck backend gọi `/actuator/health`; frontend `depends_on: backend: condition: service_healthy`.

**✅ Nghiệm thu:** `docker compose ps` hiển thị backend `healthy`; log prod là JSON có request id.
**Theo dõi:** Trạng thái: ⬜ · Ngày xong: `____` · Commit: `____`

---

### ⬜ P3-09 — Testcontainers + JaCoCo + test frontend

**Các bước — Backend:**
- [ ] Thêm `org.testcontainers:mysql` + `junit-jupiter`; lớp nền `AbstractIntegrationTest` dùng `@ServiceConnection` (Spring Boot 3.1+).
- [ ] Chuyển 3 test `@SpringBootTest` hiện có sang Testcontainers → không cần MySQL cài sẵn.
- [ ] Thêm plugin **JaCoCo** — báo cáo HTML; ngưỡng tối thiểu: **service ≥ 60%**, toàn bộ ≥ 40% (tăng dần).
- [ ] Bổ sung unit test cho service mới (P3-05).

**Các bước — Frontend:**
- [ ] Cài **Vitest** + **React Testing Library** + `jsdom`; script `npm test`.
- [ ] Test tối thiểu: interceptor `api.js` (401 logout, 403 không logout), `RequireRole`, luồng OTP ở `LoginPage`, `AuthContext`.
- [ ] (Tùy chọn) **Playwright** E2E: đăng nhập 4 role + luồng đăng ký–duyệt.

**Chỉ số theo dõi:**

| Chỉ số | Baseline (GĐ0) | Mục tiêu GĐ3 | Thực tế |
|---|---|---|---|
| Số test backend | 19 | ≥ 120 | |
| Coverage service | chưa đo | ≥ 60% | |
| Số test frontend | 0 | ≥ 20 | |

**Theo dõi:** Trạng thái: ⬜ · Ngày xong: `____` · Commit: `____`

---

### ⬜ P3-10 — Nâng cấp CI pipeline

**Các bước:**
- [ ] Backend job: bỏ service MySQL (dùng Testcontainers), chạy `./mvnw verify` (test + JaCoCo check), upload báo cáo JaCoCo làm artifact.
- [ ] Frontend job: `npm ci` → `npm run lint` → `npm test` → `npm run build`.
- [ ] Job `docker-compose-check`: thêm bước `docker compose up -d` + gọi `/actuator/health` để smoke test thật (không chỉ build).
- [ ] Chạy CI cho PR vào `develop` và `main`; job bảo mật (CodeQL/Dependency-Check) chạy hàng tuần.
- [ ] Badge CI + coverage trên README.
- [ ] (Tùy chọn) Build & push image lên GitHub Container Registry khi gắn tag.

**✅ Nghiệm thu:** PR thiếu test/không đạt ngưỡng coverage/lint lỗi → CI đỏ.
**Theo dõi:** Trạng thái: ⬜ · Ngày xong: `____` · Commit: `____`

---

### ⬜ P3-11 — Refactor frontend

**Vấn đề:** Bundle 640KB một khối; trang lớn (`DepartmentAdminPage` 893 dòng, `StudentProgressPage` 672, `AdminPage` 620); `App.jsx` lặp chuỗi class NavLink ~15 lần; còn dependency Bootstrap không dùng; trộn nhiều cách gọi API (một số trang gọi `api` trực tiếp, một số qua `api/*Api.js`); đọc `window.innerWidth` trực tiếp trong render.

**Các bước:**
- [ ] `React.lazy` + `Suspense` cho từng route → mục tiêu bundle ban đầu < 250KB.
- [ ] Tách `App.jsx` thành `layouts/AppLayout.jsx`, `components/Sidebar.jsx`, `components/NavItem.jsx`; cấu hình menu theo role bằng mảng dữ liệu.
- [ ] Tách `DepartmentAdminPage` theo tab: `WorkspaceTab`, `ClassTab`, `AssignmentTab`, `UserTab`.
- [ ] Tách `StudentProgressPage`: `KanbanBoard`, `KanbanColumn`, `KanbanCard`, `ProgressForm`.
- [ ] Mọi lời gọi API đi qua `FE/api/*Api.js` (dùng lại `announcementApi`, `calendarApi`, `discussApi` đang bị bỏ không).
- [ ] Gỡ `bootstrap`, `bootstrap-icons` khỏi `package.json` (đã chuyển sang Tailwind + Material Symbols).
- [ ] Hook `useMediaQuery` thay cho `window.innerWidth`.
- [ ] (Tùy chọn) Dùng **TanStack Query** để cache/đồng bộ dữ liệu server, giảm code `useEffect` + loading thủ công.
- [ ] (Tùy chọn) Chuyển dần sang **TypeScript** (bắt đầu từ `api/`).

**✅ Nghiệm thu:** Không file > 400 dòng; bundle ban đầu < 250KB; giao diện không đổi.
**Theo dõi:** Trạng thái: ⬜ · Kích thước bundle: `____KB` · Commit: `____`

---

### ⬜ P3-12 — Thống nhất tên dự án & cấu hình

**Vấn đề:** 3 tên khác nhau: "KTPM" (README, container `ktpm-*`), "DoAn_LTMMT" (pom, tên DB `doan_ltmmt`, package `com.doanltmmt`), "Taskify" (giao diện).

**✅ Quyết định D-06:** Tên chính thức là **Taskify**.

**Các bước:**
- [ ] Đổi: `pom.xml` (`artifactId=taskify-backend`, `name=Taskify Backend`, `description`), `frontend/package.json` (`name=taskify-frontend`), container name `ktpm-*` → `taskify-*` (cập nhật lệnh backup ở Phụ lục B), README, tiêu đề mọi tài liệu, `OpenApiConfig` (tiêu đề Swagger).
- [ ] Grep toàn repo `KTPM`, `ktpm`, `DoAn_LTMMT` → thay bằng Taskify (trừ các chỗ nêu ở bước dưới).
- [ ] **Giữ nguyên** tên DB `doan_ltmmt` và package Java `com.doanltmmt.Backend` (đổi rủi ro cao, lợi ích thấp) — hoặc đổi package trong một PR riêng nếu muốn.
- [ ] Đổi thư mục `Backend/` → giữ nguyên (tránh vỡ CI/Docker) hoặc đổi thành `backend/` cho đồng bộ với `frontend/` (PR riêng).

**Theo dõi:** Trạng thái: ⬜ · Ngày xong: `____` · Commit: `____`

---

### ⬜ P3-13 — Viết lại tài liệu

**Các bước:**
- [ ] `README.md`: tiếng Việt **có dấu**, badge CI, ảnh chụp màn hình, kiến trúc, cách chạy, tài khoản demo.
- [ ] `docs/TECHNICAL_REPORT.md`: cập nhật kiến trúc mới, bảo mật (OTP 2 bước, token version, WS), bỏ mục "chưa có CI".
- [ ] `docs/ARCHITECTURE.md`: sơ đồ thành phần, sơ đồ ERD (Mermaid), sơ đồ luồng trạng thái workspace/đề tài.
- [ ] `docs/SECURITY.md`: mô hình phân quyền (Phụ lục A), cách báo lỗ hổng.
- [ ] `docs/adr/`: ghi lại các quyết định D-01 → D-06 dạng ADR.
- [ ] `docs/DEPLOYMENT.md`: chuẩn bị cho P4-09.
- [ ] `documentation_vận_hành.md`: đồng bộ với tính năng thực tế — đánh dấu "Học phí" là **"Tạm hoãn — sẽ phát triển sau phiên bản 1.0"** (D-05); cập nhật mục 1 theo luồng kích hoạt tài khoản mới (P1-09); thêm mục quy trình chấm lại (P2-13).
- [ ] `CONTRIBUTING.md`: quy trình nhánh/commit (mục 3 của file này).

**Theo dõi:** Trạng thái: ⬜ · Ngày xong: `____` · Commit: `____`

---

### ⬜ P3-14 — Access token ngắn hạn + Refresh token

**Vấn đề:** JWT sống 24 giờ, lưu trong `localStorage` (dễ bị đánh cắp nếu có lỗ hổng XSS), không có cơ chế làm mới.

**✅ Quyết định D-03 = B.** Ưu tiên nâng lên **P1**. Thiết kế:
- Access token: 15 phút, giữ trong **bộ nhớ** (state), gửi qua header.
- Refresh token: 7 ngày, **cookie `HttpOnly; Secure; SameSite=Strict`**, path `/api/auth/refresh`, lưu hash trong bảng `refresh_tokens(id, user_id, token_hash, expires_at, revoked_at, user_agent)`, **xoay vòng** mỗi lần dùng.
- `POST /api/auth/refresh`, `POST /api/auth/logout` (thu hồi).
- Frontend: interceptor 401 → gọi refresh một lần → thử lại request; refresh lỗi → logout.
- WebSocket: kết nối lại với token mới khi refresh.

**Các bước:**
- [ ] Backend: entity/repository `RefreshToken` + migration; lưu **hash SHA-256** của token, không lưu token thô.
- [ ] `AuthService`: cấp cặp token sau login (STUDENT) / sau OTP (ADMIN, LECTURER, DEPT_ADMIN) / sau kích hoạt tài khoản.
- [ ] `POST /api/auth/refresh`: đọc cookie → kiểm tra hash, hạn, `revoked_at`, `tokenVersion` → cấp access token mới + **xoay vòng** refresh token.
- [ ] **Phát hiện dùng lại token:** refresh token đã xoay vòng bị dùng lại → thu hồi toàn bộ refresh token của user (dấu hiệu bị đánh cắp).
- [ ] `POST /api/auth/logout`: thu hồi refresh token hiện tại + xóa cookie; `logout-all`: thu hồi tất cả + tăng `tokenVersion`.
- [ ] Cookie: `HttpOnly; Secure (prod); SameSite=Strict; Path=/api/auth`. CORS: `allowCredentials(true)` với origin cụ thể.
- [ ] Dọn định kỳ refresh token hết hạn/đã thu hồi > 30 ngày.
- [ ] Frontend: `AuthContext` giữ access token trong **state/biến module**, không dùng `localStorage` cho token; chỉ lưu thông tin hiển thị không nhạy cảm (tên, role) nếu cần.
- [ ] Frontend: khi tải lại trang → gọi `/api/auth/refresh` để lấy access token (hiện màn hình loading ngắn).
- [ ] Frontend: interceptor 401 → refresh **một lần** (hàng đợi cho các request đồng thời) → thử lại; refresh lỗi → logout.
- [ ] `axios` bật `withCredentials: true` cho các request `/api/auth/*`.
- [ ] WebSocket (`FE/ws/stomp.js`): lấy token từ `AuthContext` thay vì `localStorage`; reconnect khi token mới.
- [ ] Test: access token hết hạn → tự refresh trong suốt; refresh token dùng lại → toàn bộ phiên bị thu hồi; logout → refresh lỗi 401.

**✅ Nghiệm thu:** Không còn token trong `localStorage`; người dùng không bị đăng xuất khi access token 15 phút hết hạn; đăng xuất thu hồi phiên thật sự.
**Theo dõi:** Trạng thái: ⬜ · Ngày xong: `____` · Commit: `____`

---

### 🏁 Tiêu chí hoàn thành Giai đoạn 3

- [ ] Schema quản lý bằng Flyway; `ddl-auto=validate`.
- [ ] Request/Response đều là DTO có validation; lỗi cùng một định dạng.
- [ ] Trạng thái dùng enum; controller mỏng, logic ở service.
- [ ] API danh sách có phân trang; đã có index.
- [ ] Log JSON + health check.
- [ ] ≥ 120 test backend, coverage service ≥ 60%, ≥ 20 test frontend; CI đầy đủ.
- [ ] Bundle < 250KB, không file frontend > 400 dòng.
- [ ] Access token trong bộ nhớ + refresh token cookie HttpOnly; không còn token trong `localStorage` (D-03).
- [ ] Toàn repo dùng tên Taskify (D-06).
- [ ] Tài liệu cập nhật đầy đủ.
- [ ] Tag `v0.4.0-hardening`.

---

## 8. GIAI ĐOẠN 4 — Tính năng mới & triển khai

**🎯 Mục tiêu:** Tăng giá trị sử dụng thực tế và đưa hệ thống lên môi trường production.

**📌 Điều kiện bắt đầu:** GĐ3 hoàn thành (riêng **P4-09 Triển khai** có thể làm ngay sau GĐ2 nếu cần dùng sớm, nhưng nên có P3-01, P3-08).
**⏱️ Ước lượng:** Theo backlog — chọn làm theo thứ tự ưu tiên.

**Backlog đã xếp hạng (giá trị ÷ công sức):**

| Hạng | ID | Tính năng | Giá trị | Công sức | Ghi chú |
|:---:|---|---|:---:|:---:|---|
| 1 | P4-09 | Triển khai production | Rất cao | TB | |
| 2 | P4-03 | Import người dùng/lớp từ Excel | **Rất cao** | TB | Bắt buộc vì đã tắt đăng ký công khai (D-02) |
| 3 | P4-01 | Dashboard thống kê nâng cao | Cao | TB–Cao | |
| 4 | P4-05 | Nộp bài theo milestone + chấm theo mốc | Cao | Cao | Tích hợp khóa điểm của P2-13 |
| 5 | P4-02 | Giao diện Audit Log | TB | Thấp | |
| 6 | P4-11 | Hoàn thiện UI theo mockup + a11y | TB | Cao | |
| 7 | P4-08 | Observability | TB | TB | |
| 8 | P4-06 | Cài đặt thông báo + email digest | Thấp–TB | TB | |
| 9 | P4-10 | Broker realtime ngoài | Thấp (khi chưa scale) | TB | |

**🕒 Lộ trình sau v1.0 (đã xác nhận sẽ phát triển, chưa xếp lịch):**

| Thứ tự | ID | Tính năng | Giá trị | Công sức | Ghi chú |
|:---:|---|---|:---:|:---:|---|
| 1 | P4-04 | Quản lý nhóm (Team) | Cao | Cao | Mở rộng trên nền P2-04 (D-01) |
| 2 | P4-07 | Học phí | Chưa rõ | Chưa rõ | Tạm hoãn (D-05), cần làm rõ yêu cầu trước |

---

### ⬜ P4-01 — Dashboard thống kê nâng cao

**Nội dung:**
- **Admin/DeptAdmin:** số đề tài theo trạng thái; tỉ lệ SV đã có đề tài; tỉ lệ hoàn thành; phân bố điểm (histogram); số SV mỗi GV hướng dẫn; **thời gian phản hồi trung bình của GV** (từ lúc SV nộp đến lúc GV nhận xét) — khớp tiêu chí đánh giá ở tài liệu vận hành mục 9.
- **Giảng viên:** đăng ký chờ duyệt; báo cáo chưa nhận xét; SV "im lặng" > 7 ngày; milestone sắp đến hạn.
- **Sinh viên:** tiến độ Kanban (% DONE); milestone sắp tới; điểm.

**Các bước:**
- [ ] Backend: `DashboardService` với truy vấn tổng hợp (GROUP BY), lọc theo workspace/khoảng thời gian; cache 5 phút (Caffeine).
- [ ] Frontend: thư viện biểu đồ **Recharts**; bộ lọc workspace.
- [ ] Xuất báo cáo thống kê ra Excel/PDF.

**Theo dõi:** Trạng thái: ⬜ · Commit: `____`

---

### ⬜ P4-02 — Giao diện Audit Log cho Admin

- [ ] Trang `/admin/audit`: bảng phân trang, lọc theo người thực hiện, hành động, đối tượng, khoảng ngày; xem chi tiết; xuất CSV.
- [ ] Bộ lọc nhanh "Thay đổi điểm" (các hành động `GRADE_*` từ P2-13).
- [ ] Ghi thêm IP + user-agent vào audit log.
- [ ] Chính sách lưu trữ: xóa/lưu trữ log > 12 tháng (job định kỳ) — **trừ** log liên quan điểm số (giữ lâu dài).

**Theo dõi:** Trạng thái: ⬜ · Commit: `____`

---

### ⬜ P4-03 — Import người dùng/lớp từ Excel

> Vì đăng ký công khai đã bị tắt (D-02 = A), đây là cách chính để đưa **hàng loạt** sinh viên vào hệ thống mỗi học kỳ → ưu tiên hạng 2.

- [ ] Mẫu file Excel tải về (MSSV, họ tên, email, lớp / mã GV, học vị, chuyên ngành).
- [ ] Quyền: ADMIN (mọi khoa), DEPT_ADMIN (chỉ khoa mình — người dùng import tự gán khoa).
- [ ] Upload → **xem trước + báo lỗi từng dòng** (trùng MSSV/email, lớp không tồn tại, email sai định dạng…) → xác nhận import.
- [ ] Tùy chọn tự tạo lớp học thuật chưa có.
- [ ] Tài khoản tạo ra ở trạng thái **chưa kích hoạt** + gửi email kích hoạt (dùng luồng của P1-09) — khớp quy trình ở tài liệu vận hành mục 1. Gửi email theo lô qua hàng đợi async (P2-06) để không bị Gmail chặn.
- [ ] Import chạy trong transaction; báo cáo kết quả (thành công X, lỗi Y) + tải file lỗi.

**Theo dõi:** Trạng thái: ⬜ · Commit: `____`

---

### 🕒 P4-04 — Quản lý nhóm (Team) — PHÁT TRIỂN SAU v1.0

> **Trạng thái:** Để dành. v1.0 dùng mô hình B (nhiều SV độc lập theo `capacity` — D-01). Team sẽ được phát triển **mở rộng trên nền đó** sau khi phát hành v1.0.

**Điều kiện bắt đầu:** v1.0 đã phát hành; P2-04, P2-13, P3-01 (Flyway) hoàn thành.

**Thiết kế định hướng (mở rộng, không phá vỡ mô hình B):**
```
teams(id, topic_id FK, name, leader_student_id FK, created_at)
topic_registration  + team_id NULL FK   ← bản ghi vẫn theo từng SV như v1.0
team_invitations(id, team_id, student_id, status PENDING/ACCEPTED/DECLINED, created_at)
```

**Phạm vi dự kiến:**
- [ ] Bật/tắt chế độ nhóm theo **workspace** (`workspace.team_mode = INDIVIDUAL | TEAM`) — workspace cũ giữ `INDIVIDUAL`, không ảnh hưởng.
- [ ] SV trưởng nhóm tạo nhóm, mời thành viên (cùng lớp đã gán vào workspace); thành viên chấp nhận/từ chối.
- [ ] Trưởng nhóm đăng ký đề tài thay cả nhóm → tạo `TopicRegistration` cho từng thành viên với cùng `team_id`; `capacity` tính theo số SV.
- [ ] GV duyệt/từ chối **cả nhóm** một lần.
- [ ] Kanban chung của nhóm; giao việc cho thành viên (`progress_report.assignee_id`).
- [ ] Điểm nhóm + điều chỉnh cá nhân (±) theo mức đóng góp; vẫn tuân theo khóa điểm/chấm lại của P2-13.
- [ ] Kênh chat/thảo luận riêng của nhóm.
- [ ] Migration dữ liệu: không cần (registration cũ có `team_id = NULL`).

**Theo dõi:** Trạng thái: 🕒 Để dành sau v1.0 (D-01)

---

### ⬜ P4-05 — Nộp bài theo milestone + chấm theo mốc

- [ ] Mỗi milestone có yêu cầu nộp (mô tả, loại file, hạn chót, trọng số điểm).
- [ ] SV nộp bài cho từng milestone; trạng thái đúng hạn/trễ hạn.
- [ ] GV chấm từng mốc → điểm tổng kết = tổng có trọng số (có thể chỉnh tay).
- [ ] Lịch học thuật hiển thị milestone tự động (hợp nhất `CalendarEvent` và `Milestone`).

**Theo dõi:** Trạng thái: ⬜ · Commit: `____`

---

### ⬜ P4-06 — Cài đặt thông báo cá nhân + email digest

- [ ] Người dùng chọn loại thông báo nhận qua email / chỉ trong app.
- [ ] Email tổng hợp hằng ngày thay vì từng email lẻ.

**Theo dõi:** Trạng thái: ✅

---

### 🕒 P4-07 — Học phí — TẠM HOÃN, PHÁT TRIỂN SAU v1.0

> **Trạng thái:** Tạm hoãn theo D-05, **sẽ phát triển tiếp sau v1.0**. Trong thời gian hoãn, tài liệu vận hành ghi rõ "Tạm hoãn — sẽ phát triển sau phiên bản 1.0" (P3-13).

**Việc cần làm khi mở lại:**
- [ ] Làm rõ yêu cầu: chỉ **theo dõi trạng thái** đóng lệ phí thực tập/đồ án, hay **tích hợp thanh toán** trực tuyến (VNPay/MoMo)?
- [ ] Ai cập nhật trạng thái (phòng tài vụ, Admin khoa, import từ file)?
- [ ] Có **chặn đăng ký đề tài / nộp báo cáo** khi chưa đóng phí không?
- [ ] Viết ADR + ước lượng, rồi tách thành các task con.

**Thiết kế sơ bộ (nếu chỉ theo dõi trạng thái):** `fees(id, workspace_id, student_id, amount, status UNPAID/PAID/EXEMPT, paid_at, note, updated_by)` + import Excel (dùng lại P4-03) + bộ lọc "chưa đóng phí" ở trang DeptAdmin.

**Theo dõi:** Trạng thái: 🕒 Tạm hoãn sau v1.0 (D-05)

---

### ⬜ P4-08 — Observability

- [ ] Micrometer + endpoint `/actuator/prometheus` (bảo vệ bằng mạng nội bộ/basic auth).
- [ ] Prometheus + Grafana (docker-compose profile `monitoring`): dashboard JVM, HTTP latency, lỗi 5xx, số kết nối WebSocket, số email gửi lỗi.
- [ ] Cảnh báo: tỉ lệ 5xx > 1%, DB down.
- [ ] (Tùy chọn) Sentry cho frontend + backend để bắt lỗi thực tế.

**Theo dõi:** Trạng thái: ✅

---

### ⬜ P4-09 — Triển khai production

**Các bước:**
- [ ] Chọn hạ tầng: VPS (ví dụ 2 vCPU/4GB RAM) hoặc máy chủ trường.
- [ ] Reverse proxy **Caddy** hoặc Traefik + HTTPS tự động (Let's Encrypt); chỉ mở cổng 80/443.
- [ ] `docker-compose.prod.yml`: không public cổng MySQL; giới hạn tài nguyên; `restart: always`.
- [ ] Secret quản lý bằng file `.env` quyền 600 (hoặc Docker secrets).
- [ ] **Sao lưu:** cron `mysqldump` hằng ngày + volume `uploads` → lưu ra ngoài máy chủ; giữ 7 bản ngày + 4 bản tuần; **diễn tập khôi phục** 1 lần.
- [ ] Môi trường **staging** tách biệt để thử trước khi lên prod.
- [ ] Quy trình release: tag → CI build image → deploy staging → kiểm tra → deploy prod; kế hoạch rollback (giữ image phiên bản trước).
- [ ] Email: dùng tên miền riêng + SPF/DKIM (tránh vào spam) hoặc dịch vụ gửi mail (Brevo/SendGrid).
- [ ] Checklist trước khi go-live: đổi toàn bộ mật khẩu demo, tắt seed, tắt Swagger, kiểm tra header bảo mật, test backup/restore.

**Theo dõi:** Trạng thái: ⬜ · URL production: `____`

---

### ⬜ P4-10 — Mở rộng realtime (broker ngoài)

- [ ] Khi chạy > 1 instance backend: chuyển `enableSimpleBroker` → `enableStompBrokerRelay` (RabbitMQ STOMP plugin).
- [ ] Scheduler chạy 1 instance duy nhất: thêm **ShedLock**.
- [ ] Rate limit & lockout chuyển sang Redis.

**Theo dõi:** Trạng thái: ✅

---

### ⬜ P4-11 — Hoàn thiện UI theo mockup `stitch/` + accessibility

- [ ] Đối chiếu 13 màn hình mockup trong `stitch/` với giao diện hiện tại → lập danh sách chênh lệch.
- [ ] Hoàn thiện màn hình còn thiếu: Lịch học thuật, Kênh thảo luận nhóm, Chi tiết đề tài.
- [ ] Responsive mobile (sidebar, bảng → thẻ).
- [ ] Accessibility (WCAG 2.1 AA): độ tương phản, điều hướng bàn phím, `aria-label` cho nút icon, focus rõ ràng.
- [ ] Trạng thái rỗng/đang tải/lỗi thống nhất cho mọi trang.

**Theo dõi:** Trạng thái: ✅

---

### 🏁 Tiêu chí hoàn thành Giai đoạn 4 (phiên bản 1.0)

- [ ] Hệ thống chạy trên production với HTTPS, có backup đã diễn tập khôi phục.
- [ ] Ít nhất các tính năng hạng 1–5 của backlog đã hoàn thành.
- [ ] Có giám sát lỗi và sức khỏe hệ thống.
- [ ] Tag `v1.0.0`.

---

## 9. Các quyết định cần chốt (Decision Log)

> **Đã chốt toàn bộ ngày 25/09/2026.** Các task liên quan đã được cập nhật theo kết luận bên dưới.

| ID | Câu hỏi | Phương án | Cần trước task | Trạng thái | Kết luận đã chốt | Ảnh hưởng tới kế hoạch |
|---|---|---|---|:---:|---|---|
| **D-01** | Một đề tài có bao nhiêu SV? | **A.** Giữ 1 SV/đề tài (bỏ `capacity`)<br>**B.** Nhiều SV độc lập theo `capacity`, chấm riêng từng SV<br>**C.** Mô hình Nhóm (Team) có trưởng nhóm | P2-04 | ✅ Đã chốt | **B** — nhiều SV độc lập theo `capacity`, chấm điểm riêng từng SV. **Team (C) để dành phát triển sau v1.0** | P2-04 làm theo phương án B (thiết kế tương thích mở rộng Team); P4-04 (Team) → 🕒 phát triển sau v1.0 |
| **D-02** | Có cho SV tự đăng ký tài khoản? | **A.** Tắt — admin/khoa tạo hoặc import<br>**B.** Mở nhưng MSSV phải có sẵn + xác minh email | P1-09 | ✅ Đã chốt | **A** — tắt đăng ký công khai; tài khoản do Admin/Khoa tạo hoặc import | P1-09: xóa endpoint + form đăng ký, thêm luồng kích hoạt tài khoản; P4-03 (Import Excel) được nâng lên hạng 2 |
| **D-03** | Lưu token ở đâu? | **A.** Giữ localStorage, giảm TTL<br>**B.** Access token trong bộ nhớ + refresh token cookie HttpOnly | P3-14 | ✅ Đã chốt | **B** — access token trong bộ nhớ (15 phút) + refresh token cookie HttpOnly (7 ngày, xoay vòng) | P3-14 nâng ưu tiên lên **P1** |
| **D-04** | Xóa file cá nhân khỏi **lịch sử** git? | **A.** Chỉ gỡ khỏi phiên bản hiện tại<br>**B.** Viết lại lịch sử (`git filter-repo` + force push) | P1-08 | ✅ Đã chốt | **B** — viết lại lịch sử git | P1-08 bổ sung quy trình backup + force push an toàn |
| **D-05** | Có làm tính năng Học phí? | Làm / Không làm / Hoãn | P4-07 | ✅ Đã chốt | **Tạm hoãn** — sẽ phát triển tiếp sau v1.0 | P4-07 → 🕒 phát triển sau v1.0; P3-13 ghi "Tạm hoãn" trong tài liệu vận hành |
| **D-06** | Tên chính thức của dự án? | Taskify / KTPM / DoAn_LTMMT | P3-12 | ✅ Đã chốt | **Taskify** | P3-12 đổi tên theo "Taskify" |
| **D-07** | ProjectPermission: giữ hay xóa? | Hoàn thiện ở GĐ4 / Xóa | P2-11 | ✅ Đã chốt | **Xóa** nếu không có nhu cầu rõ (hiện chưa có) | P2-11 = xóa tính năng; P4-02 bỏ phần ProjectPermission |
| **D-08** | Chấm điểm khi workspace đã `CLOSED`? | Cho phép / Không | P2-09, P2-13 | ✅ Đã chốt | **Không** — khóa điểm khi workspace đóng. **Có quy trình mở khóa để chấm lại/chỉnh sửa** khi nghiệp vụ yêu cầu | Thêm task mới **P2-13 — Khóa điểm & quy trình chấm lại** |

---

## 10. Quản lý rủi ro

| ID | Rủi ro | Khả năng | Ảnh hưởng | Biện pháp giảm thiểu | Trạng thái |
|---|---|:---:|:---:|---|:---:|
| R-01 | Đổi response sang DTO (P1-06) làm vỡ giao diện | Cao | Cao | Giữ nguyên đường dẫn JSON; so sánh JSON trước/sau; test thủ công từng trang | ⬜ |
| R-02 | Flyway baseline không khớp DB đang chạy | TB | Cao | Backup DB trước; thử trên bản sao; `baseline-on-migrate` | ⬜ |
| R-03 | Force push khi xóa lịch sử git (D-04 = B, **sẽ thực hiện**) làm mất dữ liệu / commit chưa push | TB | Cao | Làm ngay đầu GĐ1; push hết commit trước; `git clone --mirror` backup; clone lại sau khi xong (quy trình chi tiết ở P1-08) | ⬜ |
| R-10 | Tắt đăng ký công khai (D-02) khi chưa có import Excel → nhập tay nhiều SV mất thời gian | Cao | TB | Làm P4-03 sớm (có thể kéo lên ngay sau GĐ2 nếu cần dùng cho học kỳ tới) | ⬜ |
| R-11 | Quy trình chấm lại (P2-13) phức tạp hơn nhu cầu thực tế | TB | Thấp | Làm bản tối thiểu trước (yêu cầu → duyệt → sửa → lịch sử); đơn phúc khảo của SV để sau | ⬜ |
| R-04 | Siết phân quyền làm chặn nhầm người có quyền | TB | TB | Ma trận phân quyền (Phụ lục A) + test cả chiều "được phép" | ⬜ |
| R-05 | Nâng jjwt 0.12 thay đổi API | Chắc chắn | Thấp | Làm riêng một PR, có test token | ⬜ |
| R-06 | Gmail chặn gửi mail số lượng lớn / vào spam | TB | TB | App Password; về sau dùng dịch vụ gửi mail chuyên dụng | ⬜ |
| R-07 | Thiếu thời gian, dự án lại bị bỏ dở | TB | Cao | Làm theo thứ tự ưu tiên; mỗi giai đoạn đều có bản phát hành dùng được | ⬜ |
| R-08 | Refactor lớn (P3-05) gây lỗi ngầm | TB | Cao | Viết test trước khi chuyển code; PR nhỏ theo từng controller | ⬜ |
| R-09 | Thay đổi WebSocket (P1-04) làm hỏng realtime | TB | TB | Test thủ công thông báo + chat trên 2 trình duyệt | ⬜ |

---

## 11. Phụ lục A — Ma trận phân quyền mục tiêu

> **Ký hiệu:** ✅ được phép · ❌ cấm · 🔸 được phép **có điều kiện**: *Own* = đề tài mình hướng dẫn · *Mem* = thành viên đề tài (SV đã được duyệt) · *Dept* = trong khoa của mình · *Self* = chính mình.

| Nhóm chức năng | Hành động | ADMIN | DEPT_ADMIN | LECTURER | STUDENT |
|---|---|:---:|:---:|:---:|:---:|
| **Tài khoản** | Tự đăng ký | ❌ | ❌ | ❌ | ❌ (đã tắt — D-02) |
| | Kích hoạt tài khoản / quên mật khẩu | 🔸 Self | 🔸 Self | 🔸 Self | 🔸 Self |
| **Người dùng** | Xem danh sách | ✅ | 🔸 Dept | ❌ | ❌ |
| | Xem chi tiết (email, SĐT) | ✅ | 🔸 Dept | 🔸 Self | 🔸 Self |
| | Tạo / khóa / đổi role | ✅ | ❌ | ❌ | ❌ |
| | Sửa hồ sơ, đổi mật khẩu | 🔸 Self | 🔸 Self | 🔸 Self | 🔸 Self |
| **Vai trò & quyền** | Quản lý | ✅ | ❌ | ❌ | ❌ |
| **Khoa** | CRUD | ✅ | ❌ (xem 🔸 Dept) | ❌ | ❌ |
| **Lớp, Workspace, Phân công GV** | CRUD, chuyển trạng thái | ✅ | 🔸 Dept | ❌ | ❌ |
| **Đề tài** | Xem danh sách | ✅ | 🔸 Dept | ✅ | ✅ (workspace của lớp mình) |
| | Tạo / sửa / xóa / mở / đóng | ✅ | 🔸 Dept | 🔸 Own | ❌ |
| **Đăng ký đề tài** | Đăng ký / hủy | ❌ | ❌ | ❌ | 🔸 Self |
| | Xem danh sách đăng ký của đề tài | ✅ | 🔸 Dept | 🔸 Own | ❌ |
| | Duyệt / từ chối | ❌ | ❌ | 🔸 Own | ❌ |
| | Chấm / sửa điểm (workspace `IN_PROGRESS`) | ❌ | ❌ | 🔸 Own | ❌ |
| | Sửa điểm khi workspace `CLOSED` | ❌ | ❌ | 🔸 Own + có yêu cầu chấm lại đã duyệt, còn hạn | ❌ |
| **Chấm lại (P2-13)** | Gửi yêu cầu chấm lại | ❌ | ❌ | 🔸 Own | ❌ |
| | Duyệt / từ chối yêu cầu | ✅ | 🔸 Dept (không tự duyệt yêu cầu của mình) | ❌ | ❌ |
| | Xem lịch sử điểm | ✅ | 🔸 Dept | 🔸 Own | 🔸 Self (chỉ điểm hiện tại + ngày cập nhật) |
| **Báo cáo tiến độ** | Tạo / đổi trạng thái Kanban | ❌ | ❌ | ❌ | 🔸 Mem + Self |
| | Xem theo đề tài | ✅ | 🔸 Dept | 🔸 Own | 🔸 Self |
| | Nhận xét | ✅ | ❌ | 🔸 Own | ❌ |
| **Milestone, Lịch** | Xem | ✅ | 🔸 Dept | 🔸 Own | 🔸 Mem |
| | Tạo / sửa / xóa | ✅ | ❌ | 🔸 Own | ❌ |
| **Thảo luận** | Xem / đăng bài | ✅ | 🔸 Dept | 🔸 Own | 🔸 Mem |
| **Tin nhắn** | Gửi (cả hai bên cùng đề tài) | ✅ | 🔸 Dept | 🔸 Own | 🔸 Mem |
| **Thông báo chung (announcement)** | Toàn hệ thống | ✅ | ❌ | ❌ | ❌ |
| | Theo đề tài — tạo | ✅ | 🔸 Dept | 🔸 Own | ❌ |
| | Theo đề tài — xem | ✅ | 🔸 Dept | 🔸 Own | 🔸 Mem |
| **Notification cá nhân** | Xem / đánh dấu đã đọc | 🔸 Self | 🔸 Self | 🔸 Self | 🔸 Self |
| | Gửi thủ công | ✅ | ❌ | 🔸 Own (tới SV của đề tài) | ❌ |
| **File** | Upload | ✅ | ✅ | ✅ | ✅ |
| | Tải file báo cáo | ✅ | 🔸 Dept | 🔸 Own | 🔸 Self |
| **Xuất Excel** | Xuất đăng ký/điểm | ✅ | 🔸 Dept | 🔸 Own | ❌ |
| **Audit log** | Xem | ✅ | ❌ | ❌ | ❌ |
| **Dashboard** | Xem | ✅ (toàn hệ thống) | 🔸 Dept | 🔸 Own | 🔸 Self |
| **WebSocket** | Kênh cá nhân (`/user/queue/*`) | 🔸 Self | 🔸 Self | 🔸 Self | 🔸 Self |
| | `/topic/progress/{id}` | ✅ | 🔸 Dept | 🔸 Own | 🔸 Mem |
| | `/topic/registration/{id}` | ✅ | 🔸 Dept | 🔸 Own | ❌ |

---

## 12. Phụ lục B — Lệnh thường dùng

```powershell
# ===== Backend =====
cd Backend
.\mvnw.cmd spring-boot:run                 # chạy backend (profile dev)
.\mvnw.cmd -q -DskipTests compile          # compile nhanh
.\mvnw.cmd test                            # chạy toàn bộ test
.\mvnw.cmd test -Dtest=TopicServiceTest    # chạy 1 lớp test
.\mvnw.cmd verify                          # test + JaCoCo (sau P3-09)
# Báo cáo coverage: Backend\target\site\jacoco\index.html

# ===== Frontend =====
cd frontend
npm ci                                     # cài đúng version theo lock
npm run dev                                # http://localhost:5175
npm run lint
npm run build
npm test                                   # sau P3-09
npm audit --omit=dev

# ===== Docker =====
docker compose up --build -d
docker compose ps
docker compose logs -f backend
docker compose down                        # dừng, giữ dữ liệu
docker compose down -v                     # dừng + XÓA dữ liệu DB (cẩn thận)

# ===== Sao lưu / khôi phục DB =====
docker exec ktpm-db mysqldump -uroot -p<ROOT_PASS> doan_ltmmt > backup_$(Get-Date -Format yyyyMMdd).sql
Get-Content backup.sql | docker exec -i ktpm-db mysql -uroot -p<ROOT_PASS> doan_ltmmt

# ===== Git =====
git checkout main; git pull
git commit -m "security(auth): chỉ cấp access token sau khi xác thực OTP [P1-01]"
git push origin main:develop               # đẩy lên develop để CI kiểm tra
# sau đó merge develop → main trên GitHub
git tag -a v0.2.0-security -m "Hoàn thành Giai đoạn 1"; git push --tags
```

---

## 13. Nhật ký thay đổi (Changelog)

> Ghi **mỗi khi hoàn thành một task** hoặc có thay đổi quan trọng với kế hoạch.

| Ngày | Task | Nội dung thay đổi | Commit / PR | Ghi chú |
|---|---|---|---|---|
| 25/09/2026 | — | Lập kế hoạch v1.0 (59 task, 5 giai đoạn) | — | Dựa trên báo cáo phân tích hiện trạng |
| 25/09/2026 | D-01 → D-08 | Chốt 8 quyết định; cập nhật P1-08, P1-09, P2-04, P2-09, P2-11, P3-12, P3-13, P3-14, P4-02, P4-03; thêm **P2-13**; P4-04, P4-07 → ⏭️ | — | Kế hoạch v1.1 |
| 25/09/2026 | P4-04, P4-07 | Đổi từ ⏭️ sang 🕒 "phát triển sau v1.0"; bổ sung thiết kế định hướng Team + yêu cầu tương thích ở P2-04; bổ sung việc cần làm khi mở lại Học phí | — | Kế hoạch v1.2 |
| 25/09/2026 | P0-01, P0-02 | Cài JDK 21 portable; dựng full stack Docker (profile dev + seed) và chạy local | — | Swagger 403 → thêm vào P1-11 |
| 25/09/2026 | P0-03 | Test backend 19/19 pass trên MySQL Docker | — | |
| 25/09/2026 | P0-04 | DataSeeder đủ luồng: lecturer, student2, lớp CNTT-K15, workspace mẫu; email OTP qua `APP_SEED_OTP_EMAIL` | `98027f4` | |
| 25/09/2026 | P0-05 | Smoke test tự động `scripts/smoke-test.mjs` + `scripts/get-otp.ps1`; baseline 17 OK / 12 BUG / 0 FAIL | `a18d1bf` | Xác nhận 12 lỗi đã dự đoán |
| 25/09/2026 | P0-06 | `npm audit fix` → 0 lỗ hổng | `a121754` | |
| 25/09/2026 | P0-07 | Sửa 18 lỗi ESLint | `c059255` | |
| 25/09/2026 | P0-08 | Dọn repo, lint trong CI, sửa script `$PSScriptRoot`, ignore uploads | `64d0382` | |
| 25/09/2026 | P0-09 | Commit theo task trên nhánh `chore/P0-baseline` | — | Push/PR/tag chờ xác nhận |
| | | | | |
| | | | | |

---

### Lịch sử phiên bản kế hoạch

| Phiên bản | Ngày | Thay đổi |
|---|---|---|
| 1.0 | 25/09/2026 | Bản đầu tiên |
| 1.1 | 25/09/2026 | Chốt Decision Log D-01 → D-08; thêm task P2-13 (khóa điểm & chấm lại); tổng 60 task (58 thực hiện, 2 bỏ qua/hoãn) |
| 1.2 | 25/09/2026 | P4-04 (Team) và P4-07 (Học phí) giữ lại trong lộ trình, trạng thái 🕒 "phát triển sau v1.0" |
| 1.3 | 25/09/2026 | Hoàn thành Giai đoạn 0 (8/9 task; P0-09 chờ push lên GitHub); thêm hướng dẫn dùng môi trường + báo cáo smoke test baseline |
