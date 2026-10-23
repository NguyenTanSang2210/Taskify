# BÁO CÁO ĐỐI CHIẾU KẾ HOẠCH & KIỂM THỬ TOÀN DIỆN — TASKIFY

> **Ngày:** 21/10/2026 · **Nhánh:** `develop` (đối chiếu với `origin/develop` = `a03e4e4`, PR #3 `develop` → `main`)
> **Căn cứ:** `Plan/plan1.md` (v1.3 → v1.4), `Plan/GĐ1.md`, `Plan/reports/GĐ1-smoke.md`, toàn bộ mã nguồn + lịch sử git.
> **Phạm vi báo cáo:** (1) xác định giai đoạn hiện tại; (2) các task còn dở và việc đã làm; (3) kiểm thử toàn diện (chức năng, xử lý lỗi, hiệu năng, tương thích, bảo mật); (4) vấn đề phát hiện, mức độ, đề xuất; (5) giải thích "claude" trong lịch sử commit.

---

## MỤC LỤC

1. [Tóm tắt điều hành](#1-tóm-tắt-điều-hành)
2. [Xác định giai đoạn hiện tại](#2-xác-định-giai-đoạn-hiện-tại)
3. [Việc đã thực hiện trong đợt này](#3-việc-đã-thực-hiện-trong-đợt-này)
4. [Kết quả kiểm thử](#4-kết-quả-kiểm-thử)
5. [Vì sao PR #3 / commit mới nhất có dấu ✗ đỏ](#5-vì-sao-pr-3--commit-mới-nhất-có-dấu--đỏ)
6. [Danh sách vấn đề — mức độ — đề xuất](#6-danh-sách-vấn-đề--mức-độ--đề-xuất)
7. ["claude" trong commit: nguyên nhân & cách gỡ](#7-claude-trong-commit-nguyên-nhân--cách-gỡ)
8. [Việc cần chủ dự án làm để đóng Giai đoạn 1](#8-việc-cần-chủ-dự-án-làm-để-đóng-giai-đoạn-1)
9. [Phụ lục — môi trường & cách chạy lại](#9-phụ-lục--môi-trường--cách-chạy-lại)
10. [Kiểm tra lại lần 2 — 22/10/2026](#10-kiểm-tra-lại-lần-2--22102026-rà-soát-code-sạch--xác-nhận-hoàn-thành)
11. [Đóng Giai đoạn 0 & 1 — 22/10/2026](#11-đóng-giai-đoạn-0--1--22102026)
12. [Giai đoạn 2 — Sửa lỗi nghiệp vụ & chức năng (hoàn thành 13/13)](#12-giai-đoạn-2--sửa-lỗi-nghiệp-vụ--chức-năng-hoàn-thành-1313)
13. [Rà soát giao diện, giờ Việt Nam, Flyway — 23/10/2026](#13-rà-soát-giao-diện-giờ-việt-nam-flyway--23102026)

---

## 1. Tóm tắt điều hành

| Hạng mục | Kết luận |
|---|---|
| **Giai đoạn hiện tại** | **Giai đoạn 1 — Vá bảo mật**, đang ở bước **đóng giai đoạn**: 12/13 task ✅; **P1-08** 🟨 (đã gỡ file khỏi phiên bản hiện tại, *chưa* xóa khỏi lịch sử git — thao tác phá hủy cần chủ dự án xác nhận). GĐ0 xong 8/9 (P0-09 chờ merge + tag). GĐ2–GĐ4 chưa bắt đầu. |
| **Task GĐ1 còn dở** | P1-08 bước B–D (viết lại lịch sử + force push); 2 tiêu chí đóng GĐ1: check bảo mật trên PR #3 xanh, merge `develop` → `main` + tag `v0.2.0-security`. Tất cả đều là thao tác trên GitHub cần chủ dự án quyết định — xem mục 8. |
| **Kiểm thử** | Backend **202/202** test · Frontend lint 0 lỗi, build OK, `npm audit` **0** lỗ hổng · Smoke toàn luồng **30 kịch bản, 0 FAIL** (6 BUG đã biết đều thuộc GĐ2) · WebSocket **12/12** · Giao diện **8/8** · Kiểm thử bổ sung (lỗi/bảo mật/hiệu năng) **28/28** sau khi vá · CodeQL: từ 6 cảnh báo (5 High) còn **4** (đều có lý do, xem mục 5). |
| **Lỗi mới phát hiện & đã vá** | 5 vấn đề: ReDoS ở kiểm tra email (CodeQL High), username dài → HTTP 500 **kèm câu SQL trong phản hồi**, `InvalidPathException` với tên file tiếng Việt cũ, 2 lỗ hổng npm (High), lộ phiên bản nginx. Mỗi lỗi có test hồi quy. |
| **"claude" trong commit** | Do dòng `Co-Authored-By: Claude … <noreply@anthropic.com>` mà Claude Code tự thêm vào **24 commit** (trên `develop`, chưa có trên `main`). Chỉ là chú thích trong *thông điệp commit* — không có mã nào khác biệt. Gỡ được bằng viết lại lịch sử (nên gộp chung với P1-08). Đã tắt cho các commit về sau. |

---

## 2. Xác định giai đoạn hiện tại

### 2.1. Đối chiếu kế hoạch ↔ thực tế

| Giai đoạn | Trạng thái trong `plan1.md` (trước) | Thực tế (mã nguồn + git + CI) | Kết luận |
|---|---|---|---|
| GĐ0 | ✅ 8/9, P0-09 "chờ push" | `develop` đã push, CI chạy; PR #3 mở từ 25/09, **chưa merge**, chưa tag `v0.1.0-baseline` | ✅ (P0-09 còn merge + tag) |
| GĐ1 | Bảng 2.3: 12 ✅ + P1-08 🟨 — **nhưng phần chi tiết task vẫn ⬜, tiêu chí GĐ1 chưa tick, Changelog thiếu toàn bộ P1** | 17 commit P1 trên `develop` (30/09–01/10), `Plan/GĐ1.md` ghi đủ nhật ký; 18/18 test bắt buộc có trong `Backend/src/test/.../security/` | **Đang đóng GĐ1** — tài liệu đã được đồng bộ trong đợt này |
| GĐ2 | ⬜ | Smoke test vẫn tái hiện đúng 6 lỗi thuộc GĐ2 (P2-01, P2-03×3, P2-04, P2-05) → chưa task nào được làm | Chưa bắt đầu |
| GĐ3, GĐ4 | ⬜ | Chưa có Flyway, refresh token, phân trang… | Chưa bắt đầu |

### 2.2. Đối chiếu từng task GĐ1 với mã nguồn

| Task | Bằng chứng trong mã | Commit | Kiểm chứng đợt này |
|---|---|---|---|
| P1-01 OTP pre-auth | `JwtTokenProvider.Scope`, `AuthController` trả `preAuthToken` | `850bad3` | `AuthFlowIT` 19 test; smoke 1a FIXED; UI: ADMIN dừng ở bước OTP, localStorage không có token |
| P1-02 Thu hồi token | `TokenAuthenticator`, `tokenVersion` | `2232628` | `TokenRevocationIT` 15; smoke 19 FIXED |
| P1-03 Secret bắt buộc | `jwt.secret=${JWT_SECRET:}` + kiểm tra khi khởi động; compose `:?` | `bf33cb5` | `JwtTokenProviderTest` 18; JWT `alg=none` / secret yếu → 401 |
| P1-04 WebSocket | `WebSocketAuthChannelInterceptor` | `5de08b0` | `WebSocketSecurityIT` 13; `ws-check.mjs` 12/12 |
| P1-05 IDOR | `TopicAccessService`, `SecurityScopeService` | `c946334` | `TopicAccessServiceTest` 16, `MilestoneCalendarAccessIT` 12, `TopicCollaborationAccessIT` 9; smoke S1–S3 FIXED |
| P1-06 DTO | `dto/response/*`, `open-in-view=false` | `fae6886` | `DtoNoLeakIT` 3 |
| P1-07 Upload | `FileStorageService`, `FileContentValidator`, `StoredFile` | `78b95f9` | `FileAccessIT` 16; .exe giả .pdf → 400; SVG → 400; 21 MB → 413; path traversal → 400 |
| P1-08 Gỡ file | `.gitignore` có `Backend/uploads/`; `git ls-files Backend/uploads` rỗng | `14a7894`, `246c3f6` | **Lịch sử vẫn còn** ở 3 commit (`59344f7`, `2426753`, `14a7894`) → 🟨 |
| P1-09 Kích hoạt | `AuthService`, `PasswordPolicy`, bỏ `/auth/register` | `7c80016` | `AccountActivationIT` 10, `PasswordPolicyTest` 7; smoke S4 FIXED; UI không còn "Đăng ký ngay" |
| P1-10 Brute-force | `LoginThrottleService` (bảng `login_throttle`), `RateLimiter` | `c67ad6f` | `LoginThrottleIT` 9, `RateLimitIT` 4; khóa **vẫn giữ sau khi khởi động lại backend** |
| P1-11 Header/Swagger | `nginx.conf` CSP…, `app.swagger.public` | `31cf02a`, `a621ab3`, `77b2c0e` | `SecurityEndpointsIT`, `SwaggerClosedIT`; header có đủ trên nginx + API |
| P1-12 Test hồi quy | `AccessMatrixIT` + bảng ánh xạ ở `GĐ1.md` 6.1 | `a03e4e4` | 18/18 pass (trong 202 test) |
| P1-13 Dependency | Boot 3.5.16, jjwt 0.12.7, POI 5.5.1, springdoc 2.8.17; `dependabot.yml`, `codeql.yml` | `428c374` | Build + test xanh |

---

## 3. Việc đã thực hiện trong đợt này

Mọi commit đứng tên **NguyenTanSang2210** (không còn trailer Claude):

| Commit | Nội dung | Test đi kèm |
|---|---|---|
| `9541d43` | **security(api):** vá ReDoS ở kiểm tra email khi sửa hồ sơ (`UserController.isValidEmail`) — regex cũ `^[^@\s]+@[^@\s]+\.[^@\s]+$` quay lui đa thức; regex mới tách tên miền theo dấu chấm + giới hạn 254 ký tự. Ghi chú lý do tắt CSRF. | `UserEmailValidationTest` (3 test, có test chuỗi độc hại < 1 giây) |
| `cfb55aa` | **security(api):** `LoginRequest` giới hạn 255 ký tự; `DataAccessErrorHandler` — lỗi CSDL trả thông báo chung (409/500), **không còn lộ câu SQL / tên bảng / tên cột** | `AuthFlowIT.overlongUsername_*`, `DataAccessErrorHandlerTest` (2) |
| `05c5df3` | **fix(files):** `FileStorageService.resolve` bắt `InvalidPathException` (tên file tiếng Việt cũ trên máy chủ locale không phải UTF-8) | Tái hiện bằng `FileAccessIT.backfill_*` khi `LANG=POSIX` |
| `23e94df` | **chore(security):** `npm audit fix` (follow-redirects, source-map-js — 2 High); `server_tokens off` cho nginx; escape `\` trong báo cáo smoke (CodeQL) | lint + build + audit |
| `985a121` | **chore(claude):** `.claude/settings.json` tắt `Co-Authored-By` / link phiên của Claude Code cho repo này | — |
| (commit tài liệu) | Đồng bộ `plan1.md` → v1.4 (trạng thái 13 task P1, tiêu chí GĐ1, P0-09, Changelog), thêm nhật ký vào `GĐ1.md`, báo cáo này | — |

**Không thực hiện** (cần chủ dự án xác nhận — thao tác công khai/không hoàn tác): viết lại lịch sử git (P1-08B + gỡ trailer Claude), force push, merge PR #3, gắn tag, đánh dấu cảnh báo CodeQL/GitGuardian trên GitHub.

---

## 4. Kết quả kiểm thử

### 4.1. Phương pháp & môi trường

- **Môi trường:** Linux (container), JDK 21.0.12, Node 22, MySQL 8.4 (Docker), Chromium (Playwright). Full stack dựng bằng `docker compose` với profile `dev` + seed (jar/dist build sẵn — do mạng sandbox không cho Maven tải bên trong `docker build`), cổng giống hướng dẫn: API `8081`, Web `5175`, DB `3307`.
- **Cấp độ kiểm thử:** đơn vị + tích hợp (JUnit/MockMvc trên MySQL thật) → API toàn luồng (`scripts/smoke-test.mjs`) → WebSocket thật (`ws-check.mjs`) → giao diện thật (`ui-check.mjs`, Chromium headless) → kiểm thử bổ sung tự viết cho đợt này: xử lý lỗi, bảo mật nâng cao, tải, tương thích → phân tích tĩnh (CodeQL 2.27.3, cùng bộ truy vấn `code-scanning` như GitHub) → quét secret trong toàn bộ lịch sử git.
- **Tiêu chí đạt:** không có lỗi 5xx ngoài dự kiến, không lộ thông tin nội bộ, mọi kiểm soát truy cập trả đúng 401/403, p95 < 1 s ở 50 người dùng đồng thời.

### 4.2. Tổng hợp

| # | Bộ kiểm thử | Trước khi vá | Sau khi vá | Ghi chú |
|---|---|:---:|:---:|---|
| 1 | Backend `mvnw clean test` | 195/196 (1 lỗi) | **202/202** | Lỗi: `FileAccessIT.backfill_*` khi locale POSIX (mục 6 #3); +6 test mới |
| 2 | Frontend `npm run lint` | 0 lỗi | 0 lỗi | |
| 3 | Frontend `npm run build` | OK | OK | Cảnh báo bundle 659 kB > 500 kB (→ P3-11) |
| 4 | `npm audit` | **2 High** | **0** | follow-redirects, source-map-js |
| 5 | Smoke toàn luồng (30 kịch bản) | OK 18 · FIXED 6 · BUG 6 · FAIL 0 | OK 18 · FIXED 6 · BUG 6 · **FAIL 0** | Giống hệt báo cáo GĐ1 01/10 → **không hồi quy** |
| 6 | WebSocket (`ws-check.mjs`) | 12/12 | 12/12 | |
| 7 | Giao diện (`ui-check.mjs`) | 8/8 | 8/8 | Không vi phạm CSP |
| 8 | Xử lý lỗi (8 ca) | 6/8 *(2 ca do kịch bản gọi sai endpoint, đã sửa kịch bản)* — **1 lỗi thật** | 8/8 | Username 10.000 ký tự → 500 + lộ SQL |
| 9 | Bảo mật nâng cao (16 ca) | 15/16 | 16/16 | Lộ phiên bản nginx |
| 10 | Hiệu năng (3 kịch bản × 1.000 request) | Đạt | Đạt | Số liệu 4.5 |
| 11 | Tương thích (3 kích thước màn hình) | Đạt | Đạt | Không tràn ngang |
| 12 | CodeQL | 6 cảnh báo (5 High) | 4 (xem mục 5) | |
| 13 | Quét secret toàn lịch sử git | Không có secret thật | — | 15 phát hiện GitGuardian đều là dữ liệu test |

### 4.3. Chức năng cốt lõi (smoke toàn luồng, 21/10/2026)

| Nhóm nghiệp vụ | Kịch bản | Kết quả |
|---|---|---|
| Xác thực | Admin đăng nhập + OTP; token trước OTP không dùng được (1a); khóa tài khoản thu hồi token (19); không còn đăng ký công khai (S4) | OK / FIXED |
| Quản trị | Tạo khoa + giảng viên (kích hoạt bằng mã); DeptAdmin tạo workspace, gán lớp, phân công GV, chuyển trạng thái OPEN_TOPIC → OPEN_REGISTRATION → LOCK → IN_PROGRESS | OK |
| Đề tài & đăng ký | DeptAdmin tạo đề tài; 2 SV đăng ký; GV duyệt SV 1 | OK |
| Tiến độ | Upload + nộp báo cáo; tải file có token; không token → 401; nhận xét → thông báo; đổi trạng thái Kanban | OK |
| Cộng tác | Chat GV ↔ SV; chấm điểm; xuất Excel | OK |
| Phân quyền | SV đọc audit log → 403; GV khác tạo milestone → 403; SV xem email người khác → bị ẩn | FIXED |
| **Lỗi đã biết (GĐ2)** | GV tự tạo đề tài 403 (**P2-01**); duyệt SV thứ 2 khi capacity=3 bị từ chối (**P2-04**); frontend gán object vào `fileUrl` + tải file bằng `<a href>` + nginx giới hạn 1 MB (**P2-03**); quên mật khẩu 404 (**P2-05**) | BUG (đúng dự kiến) |

### 4.4. Xử lý lỗi & bảo mật nâng cao (sau khi vá)

| ID | Ca kiểm thử | Kỳ vọng | Kết quả |
|---|---|---|---|
| E1 | JSON hỏng | 400, không stacktrace | ✅ 400 |
| E2 | ID sai kiểu `/workspaces/abc` | 400 | ✅ 400 *(thông điệp còn lộ tên kiểu Java — mục 6 #8)* |
| E3 | Tài nguyên không tồn tại | 404/403, không 500 | ✅ 404 |
| E4 / E5 | Endpoint không tồn tại / sai method | 404 / 401–405 | ✅ |
| E6 | Email độc hại 5.000 ký tự (ReDoS) | 400 nhanh | ✅ 400 trong ~35 ms |
| E7 | Họ tên rỗng | 400 | ✅ |
| E8 | Username 10.000 ký tự | 4xx | ✅ 400 *(trước: **500 + câu INSERT SQL** trong phản hồi)* |
| S1–S3 | JWT `alg=none` / sửa payload / ký bằng secret yếu | 401 | ✅ 401 cả 3 |
| S4 | SV gọi `/users` | 403 | ✅ |
| S5–S6 | CORS origin lạ / hợp lệ | Không cấp / cấp ACAO | ✅ |
| S7–S8 | SQL injection ở đăng nhập / query string | 401 / không 500 | ✅ |
| S9 | Path traversal tải file | 400/404 | ✅ 400 |
| S10–S12 | `.exe` đổi đuôi `.pdf` / SVG chứa script / file 21 MB | 400 / 400 / 413 | ✅ |
| S13–S14 | Header bảo mật nginx (CSP, XFO, XCTO, Referrer) / API (XCTO, XFO, no-store) | Có đủ | ✅ |
| S15 | Header `Server` không lộ phiên bản | `nginx` | ✅ *(trước: `nginx/1.27.5`)* |
| S16 | Sai mật khẩu liên tiếp | Khóa tạm (429), kể cả khi nhập đúng | ✅ 5 lần 401 → 429; khóa còn hiệu lực sau khởi động lại backend |

### 4.5. Hiệu năng (1.000 request, 50 kết nối đồng thời, dữ liệu demo)

| Endpoint | Thông lượng | p50 | p95 | p99 | Lỗi 5xx |
|---|---:|---:|---:|---:|:---:|
| `GET /api/topics` (SV) | 116–131 req/s | 373–414 ms | 598–719 ms | 719–855 ms | 0 |
| `GET /api/notifications` (SV) | 334–361 req/s | 125–136 ms | 282–290 ms | 357–381 ms | 0 |
| `GET /` (nginx, tĩnh) | 2.135–2.313 req/s | 17–20 ms | 44–52 ms | 59–76 ms | 0 |

Một request đơn lẻ `GET /api/topics` mất 17–39 ms, nhưng phát sinh **12 câu SELECT** cho chỉ 1 đề tài (đo bằng MySQL general log) → dạng N+1; với pool Hikari mặc định 10 kết nối, thông lượng bị giới hạn ở ~120 req/s. Đạt tiêu chí hiện tại, nhưng sẽ chậm tuyến tính theo số đề tài → đề xuất ở mục 6 #9 (P3-06, P3-07).

### 4.6. Tương thích

| Kích thước | Trang đăng nhập | Sau đăng nhập (SV) | Lỗi console |
|---|---|---|---|
| Mobile 375×812 | Không tràn ngang | Không tràn ngang, menu thu gọn | Chỉ lỗi tải Google Fonts (mạng sandbox chặn, không phải lỗi ứng dụng) |
| Tablet 768×1024 | Không tràn ngang | Không tràn ngang | như trên |
| Desktop 1366×768 | Không tràn ngang | Không tràn ngang | như trên |

Giới hạn: chỉ kiểm được Chromium. Firefox/Safari chưa kiểm (đề xuất thêm vào P4-11).

---

## 5. Vì sao PR #3 / commit mới nhất có dấu ✗ đỏ

CI chính của dự án (**backend-test-and-build, frontend-build, docker-compose-check**) đều **xanh**. Dấu ✗ đến từ 2 check bảo mật:

### 5.1. CodeQL — "5 new alerts including 5 high severity"

Tái hiện cục bộ bằng CodeQL CLI cùng bộ truy vấn:

| Cảnh báo | Vị trí | Mức | Xử lý |
|---|---|:---:|---|
| Polynomial ReDoS (`java/polynomial-redos`) | `UserController.java` (regex email) | High | ✅ **Đã vá** (`9541d43`) |
| Incomplete string escaping | `scripts/smoke-test.mjs` | Medium | ✅ **Đã vá** (`23e94df`) |
| Disabled Spring CSRF protection | `SecurityConfig.java` | High | **Dương tính giả**: API xác thực bằng header `Authorization: Bearer`, không dùng cookie/session → CSRF không áp dụng. Đã ghi chú trong code. → Trên GitHub: *Security → Code scanning → alert → Dismiss → "False positive"*. Khi làm **P3-14** (refresh token trong cookie) phải bật bảo vệ CSRF cho endpoint đó. |
| Clear-text storage of sensitive information ×3 | `frontend/src/context/AuthContext.jsx` (token trong `localStorage`) | High | **Đúng — đã có kế hoạch**: quyết định D-03 = B, làm ở **P3-14** (access token trong bộ nhớ + refresh token cookie HttpOnly). → Dismiss "Won't fix" kèm ghi chú "P3-14", hoặc làm sớm P3-14. |

Sau khi push các commit đợt này, CodeQL sẽ chạy lại: còn 4 cảnh báo ở bảng trên — check chỉ xanh khi 4 cảnh báo đó được đánh dấu (dismiss) trên GitHub hoặc P3-14 được làm.

### 5.2. GitGuardian — "15 secrets"

Đã rà toàn bộ lịch sử git: **không có secret thật** (mật khẩu Gmail/App Password, JWT secret production…) nào bị commit. 15 phát hiện đều là:

- Mật khẩu **trong test** (`PasswordPolicyTest`, `AccountActivationIT`, `UserControllerTest`) — chính là các chuỗi dùng để kiểm tra chính sách mật khẩu.
- Chuỗi base64 giả trong `JwtTokenProviderTest` (để kiểm tra việc *từ chối* secret công khai).
- Mật khẩu demo `123456` của dữ liệu seed trong `scripts/smoke-test.mjs`.
- Dòng `docker-compose.yml` ở commit `bf33cb5` (biến bắt buộc `:?`, không có giá trị).

→ Trên dashboard GitGuardian: đánh dấu các incident là **"Test credential / False positive"**. Không cần đổi mã.

---

## 6. Danh sách vấn đề — mức độ — đề xuất

Thang mức độ: 🔴 Nghiêm trọng · 🟠 Cao · 🟡 Trung bình · 🔵 Thấp.

### 6.1. Phát hiện trong đợt này — đã khắc phục

| # | Vấn đề | Mức | Ảnh hưởng | Khắc phục |
|:-:|---|:-:|---|---|
| 1 | **Username/password rất dài ở `/api/auth/login` → HTTP 500 và phản hồi chứa nguyên câu `INSERT INTO audit_logs …`** (do `server.error.include-message=always`) | 🟠 | Lộ cấu trúc CSDL cho người chưa đăng nhập; tạo log lỗi hàng loạt (DoS log) | `@Size(max=255)` + `DataAccessErrorHandler` che mọi lỗi CSDL (`cfb55aa`) |
| 2 | ReDoS ở regex email khi cập nhật hồ sơ | 🟠 | Người dùng đã đăng nhập gửi chuỗi đặc biệt làm treo luồng xử lý (CodeQL High) | Regex tuyến tính + giới hạn độ dài (`9541d43`) |
| 3 | `FileStorageService.resolve` ném `InvalidPathException` với tên file tiếng Việt cũ khi máy chủ không chạy locale UTF-8 | 🟡 | Backfill hồ sơ file dừng giữa chừng; tải file cũ trả 500. (Image Docker `eclipse-temurin` mặc định UTF-8 nên production hiện không bị, nhưng chạy `java -jar` trên máy Linux locale POSIX thì bị) | Bắt ngoại lệ, coi như tên không hợp lệ (`05c5df3`) |
| 4 | 2 lỗ hổng npm mức High (follow-redirects, source-map-js) | 🟡 | Phụ thuộc bắc cầu; follow-redirects có thể làm lộ header xác thực khi chuyển hướng | `npm audit fix` (`23e94df`) |
| 5 | nginx trả `Server: nginx/1.27.5` | 🔵 | Lộ phiên bản giúp dò lỗ hổng | `server_tokens off` (`23e94df`) |

### 6.2. Còn tồn tại — đề xuất xử lý

| # | Vấn đề | Mức | Đề xuất | Task |
|:-:|---|:-:|---|---|
| 6 | 3 file tài liệu thật của sinh viên vẫn còn trong **lịch sử git** (public repo) | 🟠 | Thực hiện P1-08 bước B–D, **gộp chung** với việc gỡ trailer Claude (mục 7) để chỉ force push một lần | P1-08 |
| 7 | Token lưu trong `localStorage` (CodeQL High ×3) — XSS bất kỳ đọc được token | 🟠 | Làm P3-14 (đã nâng P1 theo D-03); có thể kéo lên đầu GĐ3 | P3-14 |
| 8 | `server.error.include-message=always` + thông điệp mặc định của Spring lộ chi tiết nội bộ (vd. `Failed to convert value of type 'java.lang.String' to required type 'java.lang.Long'`) | 🟡 | Global exception handler chuẩn hóa lỗi; production đặt `include-message=never` cho lỗi không phải `ResponseStatusException` | P3-03 |
| 9 | `GET /api/topics` 12 truy vấn/request (N+1), thông lượng ~120 req/s ở 50 đồng thời | 🟡 | `@EntityGraph`/`JOIN FETCH`, phân trang, index; cân nhắc tăng pool Hikari | P3-06, P3-07 |
| 10 | Bundle JS 659 kB (một chunk) | 🔵 | Lazy route + code-split | P3-11 |
| 11 | Nhãn "Học kỳ 1 • 2024-2025" viết cứng ở `frontend/src/pages/StudentTopicPage.jsx:89` | 🔵 | Lấy theo workspace/học kỳ hiện hành | P2 (UI) / P4-11 |
| 12 | 6 lỗi nghiệp vụ đã biết (P2-01, P2-03×3, P2-04, P2-05) | 🔴/🟠 | Bắt đầu GĐ2 theo thứ tự P2-01 → P2-02 → P2-03 (đều chặn luồng chính) | GĐ2 |
| 13 | `.env` mẫu/hướng dẫn dùng `SPRING_PROFILES_ACTIVE=dev`; khi triển khai thật phải là `prod` | 🟡 | Giữ cảnh báo trong tài liệu; P4-09 kiểm tra trước khi triển khai | P4-09 |
| 14 | Chưa kiểm Firefox/Safari; chưa có test frontend tự động | 🔵 | P3-09 (Vitest) + mở rộng `ui-check.mjs` đa trình duyệt | P3-09, P4-11 |

---

## 7. "claude" trong commit: nguyên nhân & cách gỡ

> ✅ **Đã xử lý 22/10/2026** — lịch sử đã viết lại theo quy trình 7.4 (xem mục 11). Các mã commit trong báo cáo này đã được cập nhật theo lịch sử mới.

### 7.1. Vì sao GitHub hiện "NguyenTanSang2210 **and claude**"

- Các phiên **Claude Code** (trên máy `DESKTOP-52AO7BE`) mặc định thêm dòng cuối vào thông điệp commit:
  `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>` (7 commit GĐ0) và `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>` (17 commit GĐ1).
- GitHub đọc trailer `Co-Authored-By`, thấy email `noreply@anthropic.com` gắn với tài khoản GitHub **claude** nên hiển thị đồng tác giả. Tổng **24 commit**, từ `9d5145a` đến `a03e4e4` — **chỉ trên `develop`** (chưa vào `main`).
- **Tác giả thật vẫn là bạn** (email `tansangnts2004@gmail.com`). Tên tác giả hiển thị `DESKTOP-52AO7BE\ADMIN` vì máy đó chưa đặt `git config user.name` — GitHub vẫn liên kết đúng tài khoản nhờ email.

### 7.2. Gỡ có ảnh hưởng đến dự án không?

- Trailer chỉ nằm trong **thông điệp commit**; nội dung file của 24 commit này giống hệt nhau dù có hay không có trailer → **mã nguồn, cấu hình, test không thay đổi**.
- Nhưng git không sửa được thông điệp commit cũ mà không **viết lại lịch sử**: mọi mã băm commit từ `9d5145a` trở đi đổi → cần **force push** `develop`; mọi bản clone cũ phải **clone lại**; các mã commit ghi trong `Plan/*.md` (Changelog, "Theo dõi") sẽ lệch và cần cập nhật theo bảng ánh xạ `filter-repo` sinh ra (`.git/filter-repo/commit-map`).
- P1-08 (D-04 = B) **vốn đã bắt buộc viết lại lịch sử** (xóa `Backend/uploads` từ commit thứ 2 `59344f7`) → nên làm **cả hai trong một lần**: chỉ một lần force push, một lần clone lại.
- Tôi **không tự thực hiện**: thao tác viết lại lịch sử (kể cả chạy thử trên bản clone tạm) bị chế độ phân quyền của phiên này chặn, và force push lên GitHub là thao tác không hoàn tác được — cần bạn quyết định.

### 7.3. Ngăn tái diễn (đã làm)

- Thêm `.claude/settings.json` vào repo: `"attribution": { "commit": "", "pr": "", "sessionUrl": false }` → mọi phiên Claude Code mở repo này không thêm trailer/link phiên nữa.
- Các commit đợt này đứng tên `NguyenTanSang2210 <tansangnts2004@gmail.com>`, không có trailer.
- Nên đặt tên git trên máy Windows: `git config --global user.name "NguyenTanSang2210"`.

### 7.4. Quy trình gỡ (bạn tự chạy, PowerShell) — gộp với P1-08 bước B–D

> ⚠️ Chỉ chạy khi: mọi thay đổi đã push, không ai đang làm trên repo. **Sau khi chạy, bạn phải clone lại.**
> Quy trình này **chưa được chạy thử** trong phiên làm báo cáo (bị chặn quyền) — bước 4 (kiểm chứng) là bắt buộc trước khi push; nếu bất kỳ kiểm chứng nào sai thì dừng, không push.

```powershell
# 0) Chuẩn bị
pip install git-filter-repo
$day = Get-Date -Format yyyyMMdd
git clone --mirror https://github.com/NguyenTanSang2210/Taskify.git "D:\backup\taskify-mirror-$day.git"   # backup, KHÔNG đẩy đi đâu

# 1) Bản clone sạch có đủ nhánh
git clone https://github.com/NguyenTanSang2210/Taskify.git taskify-clean; cd taskify-clean
git branch develop origin/develop

# 2) Quy tắc sửa thông điệp: xóa dòng Co-Authored-By của Claude + dòng trống thừa ở cuối
@'
regex:(?mi)^[ \t]*Co-Authored-By:[ \t]*Claude[^\n]*\n?==>
regex:\n+\Z==>\n
'@ | Set-Content -Encoding ascii ..\claude-trailer.txt

# (tùy chọn) chuẩn hóa tên tác giả DESKTOP-52AO7BE\ADMIN -> NguyenTanSang2210
'NguyenTanSang2210 <tansangnts2004@gmail.com> DESKTOP-52AO7BE\ADMIN <tansangnts2004@gmail.com>' | Set-Content -Encoding ascii ..\mailmap.txt

# 3) Viết lại lịch sử: xóa Backend/uploads (P1-08) + gỡ trailer (+ mailmap nếu muốn)
# --force: đây là bản clone dùng riêng cho việc này (đã tạo thêm nhánh develop nên filter-repo không coi là "fresh clone")
git filter-repo --force --path Backend/uploads --invert-paths --replace-message ..\claude-trailer.txt --mailmap ..\mailmap.txt

# 4) KIỂM CHỨNG trước khi push
git log --all --format=%B | Select-String -Pattern "Co-Authored-By: Claude"     # phải RỖNG
git log --all --oneline -- Backend/uploads                                      # phải RỖNG
git fetch "D:\backup\taskify-mirror-$day.git" "+refs/heads/*:refs/backup/*"
git diff --stat refs/backup/develop develop                                     # phải RỖNG (mã nguồn y hệt)
git diff --stat refs/backup/main main                                           # chỉ khác Backend/uploads
(git rev-list --count refs/backup/develop) -eq (git rev-list --count develop)   # True (không mất commit)
Get-Content .git\filter-repo\commit-map | Select-Object -First 5                # bảng mã cũ -> mã mới

# 5) Đẩy lên (tạm tắt Branch protection của main nếu đã bật)
git remote add origin https://github.com/NguyenTanSang2210/Taskify.git
git push --force origin main develop
git push --force --tags origin

# 6) Hoàn tất: bật lại Branch protection; XÓA bản clone cũ trên mọi máy và clone lại (không `git pull` vào bản cũ)
```

Lưu ý: các PR cũ đã merge (#1, #2) giữ tham chiếu `refs/pull/*` tới commit cũ có `Backend/uploads` — GitHub không cho tự xóa; muốn xóa hẳn phải liên hệ GitHub Support (bước tùy chọn của P1-08). Trailer Claude chỉ nằm trên PR #3 (đang mở) nên sẽ biến mất sau force push `develop`.

**Nếu không muốn viết lại lịch sử:** cứ để nguyên — trailer vô hại với dự án và từ giờ không phát sinh thêm. Khi đó P1-08 phải đổi quyết định D-04 sang A (chỉ gỡ khỏi phiên bản hiện tại).

---

## 8. Việc cần chủ dự án làm để đóng Giai đoạn 1

| # | Việc | Ở đâu | Ghi chú |
|:-:|---|---|---|
| 1 | Quyết định & chạy quy trình mục 7.4 (P1-08B + gỡ trailer Claude) | Máy của bạn | Hoặc giao cho phiên Claude Code có quyền viết lại lịch sử |
| 2 | Dismiss 4 cảnh báo CodeQL còn lại (CSRF: *False positive*; localStorage: *Won't fix — P3-14*) | GitHub → Security → Code scanning | Sau đó check CodeQL trên PR #3 xanh |
| 3 | Đánh dấu 15 incident GitGuardian là *Test credential / False positive* | dashboard.gitguardian.com | |
| 4 | Merge PR #3 `develop` → `main` | GitHub | Sau bước 1 (nếu làm) |
| 5 | Tag `v0.1.0-baseline` (tùy chọn, gắn vào commit P0) và `v0.2.0-security` | `git tag -a … ; git push --tags` | |
| 6 | Bật Branch protection cho `main` | Settings → Branches | |
| 7 | Bắt đầu **GĐ2**: P2-01 → P2-02 → P2-03 | | 6 lỗi đã biết đang chặn luồng chính |

---

## 9. Phụ lục — môi trường & cách chạy lại

```powershell
# Backend (cần MySQL; script dùng DB test riêng)
.\scripts\run-tests.ps1                                   # 202 test
# Full stack + kiểm thử toàn luồng
docker compose up -d --build
$env:REPORT_FILE="Plan/reports/GĐ1-smoke.md"; node scripts/smoke-test.mjs
node scripts/ws-check.mjs
$env:PW_MODULE="$env:TEMP\pw\node_modules\playwright-core"; node scripts/ui-check.mjs
# Frontend
cd frontend; npm ci; npm run lint; npm run build; npm audit
```

Lưu ý khi chạy lại nhiều lần: kiểm thử chống dò mật khẩu khóa tài khoản demo 10 phút (bảng `login_throttle`, giữ qua lần khởi động lại) — nếu smoke test báo `429` hàng loạt thì chờ hết khóa hoặc xóa bảng `login_throttle` trên DB **dev**.

Kiểm thử bổ sung đợt này (xử lý lỗi/bảo mật/hiệu năng/tương thích) là kịch bản tạm, không đưa vào repo; các lỗi tìm được đều đã có test hồi quy trong `Backend/src/test`.

---

## 10. Kiểm tra lại lần 2 — 22/10/2026 (rà soát "code sạch" + xác nhận hoàn thành)

Chạy lại toàn bộ trên máy sạch, code tại commit `faf48ad`:

| Hạng mục | Kết quả |
|---|---|
| Backend `mvnw clean verify` | **202/202** test đạt |
| Cảnh báo biên dịch Java (javac, bật deprecation) | **0** (trước: 5 — API lỗi thời) |
| Phân tích code thừa (Eclipse ECJ: import/biến/field/hàm không dùng, code chết) | **0** (trước: 5) |
| Cảnh báo khi khởi động backend | **0** ở profile `prod` (trước: 2 — dialect thừa, cấu hình AuthenticationProvider trùng) |
| Frontend ESLint (`--max-warnings=0`) / build / `npm audit` | 0 lỗi · 0 cảnh báo / OK / 0 lỗ hổng |
| Smoke toàn luồng | OK 18 · FIXED 6 · BUG đã biết (GĐ2) 6 · **FAIL 0** |
| WebSocket / Giao diện / Kiểm thử lỗi-bảo mật-hiệu năng | 12/12 · 8/8 · 28/28 |
| Profile `prod` | Swagger đóng; secret JWT yếu → **từ chối khởi động**; không seed dữ liệu demo |
| CodeQL (chạy cục bộ) | Không có cảnh báo mới; 4 cảnh báo cũ đã được dismiss trên GitHub (CSRF: dương tính giả; localStorage: P3-14) |
| CI trên PR #3 | **10/10 xanh** trên `1316b7a` (GitGuardian xanh sau khi Ignore 15 dữ liệu test); PR mergeable |

**Dọn code (commit `faf48ad`):** bỏ API lỗi thời (`DaoAuthenticationProvider()`, `Specification.where`, `@MockBean`), tạo `AuthenticationManager` trực tiếp, bỏ `hibernate.dialect` thừa, xóa field `EventPublisher` không dùng ở 2 controller và 3 import thừa. Không đổi hành vi (toàn bộ test + smoke giữ nguyên kết quả).

**Ghi nhận, không xóa:** `frontend/src/api/discussApi.js`, `calendarApi.js`, `announcementApi.js` chưa được trang nào dùng — backend của Thảo luận / Lịch / Thông báo chung đã có và đã bảo mật, nhưng **chưa có giao diện** (thuộc P4-11). Xóa đi sẽ mất phần nối API sẵn có, nên giữ lại.

**Trạng thái Giai đoạn 1 tại thời điểm này:** toàn bộ phần **mã nguồn** đã xong và sạch. Còn 2 việc **chỉ chủ dự án làm được** (không phải lỗi code): (1) P1-08B viết lại lịch sử git, (2) merge PR #3 + tag `v0.2.0-security`.

---

## 11. Đóng Giai đoạn 0 & 1 — 22/10/2026

### 11.1. Kiểm tra repo sau khi chủ dự án hoàn tất

| Hạng mục | Kết quả kiểm tra trên GitHub |
|---|---|
| Lịch sử git | Viết lại thành công: **0** commit chứa `Backend/uploads`, **0** trailer `Co-Authored-By: Claude`, **0** tác giả `DESKTOP-52AO7BE\ADMIN`; 51/51 commit giữ nguyên nội dung (đối chiếu theo tiêu đề + thời gian) |
| Mã nguồn | `develop` sau viết lại giống hệt trước khi viết lại (diff rỗng) |
| PR #3 `develop` → `main` | **Đã merge** bằng merge commit `d7e9307`; CI 10/10 xanh trước khi merge |
| Tag | `v0.1.0-baseline` → `f52e07d` (P0-09) · `v0.2.0-security` → `d7e9307` — đều là annotated tag |
| Bảo vệ nhánh `main` | Có rule: bắt buộc qua PR + 3 status check |
| Dependabot | Hoạt động — đã tự mở **10 PR** nâng cấp (PR #4 → #13) ngay sau khi `dependabot.yml` vào `main` → nghiệm thu P1-13 đạt |
| Repo trên máy chủ dự án | Clone lại sạch ở `D:\Taskify`; bản cũ `D:\Taskify-old`, backup `D:\taskify-rewrite\backup.git` (xóa sau ~30 ngày) |

**Kết luận:** Giai đoạn 0 (9/9) và Giai đoạn 1 (13/13) **đã hoàn thành**. Mọi mã commit trong `plan1.md`, `GĐ1.md` và báo cáo này đã cập nhật theo lịch sử mới.

### 11.2. Việc tiếp theo

**Bước kế tiếp: Giai đoạn 2 — Sửa lỗi nghiệp vụ & chức năng** (13 task, ước lượng 7–9 ngày công).

1. **Lập `Plan/GĐ2.md`** (kế hoạch chi tiết như đã làm với GĐ1): phân tích lại mã nguồn hiện tại, thiết kế, thứ tự, test cho từng task.
2. **Thứ tự đề xuất** — ưu tiên 3 lỗi đang chặn luồng chính (smoke test vẫn báo BUG):
   - **P2-01** Giảng viên tự tạo đề tài (thiếu quyền `TOPIC_MANAGE`) — smoke #5
   - **P2-02** Tách 401/403 + toast lỗi (GĐ1 đã làm phần tối thiểu: chỉ 401 mới đăng xuất)
   - **P2-03** Upload/tải file trên giao diện (gán object vào `fileUrl`, `<a href>` không gửi token, nginx giới hạn 1 MB) — smoke #11, #13a, #21
   - **P2-04** Nhiều sinh viên/đề tài theo `capacity` (D-01 = B) — smoke #9b
   - **P2-05** Quên mật khẩu — smoke #20
   - Sau đó P2-06 → P2-11, **P2-13** (khóa điểm & chấm lại, D-08), cuối cùng **P2-12** kiểm thử hồi quy toàn luồng.
3. **Mục tiêu đo được khi đóng GĐ2:** smoke test **0 BUG** (6 mục BUG hiện tại chuyển FIXED), test hồi quy nghiệp vụ trong CI, tag `v0.3.0-business`.

**Việc phụ nên xử lý sớm (không chặn GĐ2):**
- **10 PR của Dependabot** (#4–#13) đang nhắm vào `main`, trái quy trình `develop` → `main`. Đề xuất: thêm `target-branch: develop` vào `.github/dependabot.yml`; gộp các bản vá nhỏ (nhóm *minor-and-patch*, `actions/*`); **không** gộp vội các bản nâng **major** — `springdoc-openapi` 3.x (yêu cầu Spring Boot 4), `eslint` 10, `@vitejs/plugin-react` 6, `globals` 17 — cần thử riêng.
- Xóa `D:\Taskify-old` sau 1–2 tuần và backup `D:\taskify-rewrite` sau ~30 ngày.

---

## 12. Giai đoạn 2 — Sửa lỗi nghiệp vụ & chức năng (hoàn thành 13/13)

> Thực hiện 22/10/2026 trên nhánh `develop` (`a60693b` → `ea56b02`, đã push). Chờ chủ dự án merge `develop` → `main` và tạo tag `v0.3.0-business`.

### 12.1. Thứ tự ưu tiên: 10 PR Dependabot hay P2-01 trước?

| Tiêu chí | 10 PR Dependabot (#4–#13) | P2-01 |
|---|---|---|
| Có lỗ hổng bảo mật đang mở? | **Không** — đều là nâng phiên bản thường kỳ (`npm audit` = 0, không có Dependabot security alert) | — |
| Chặn người dùng? | Không | **Có** — giảng viên không tạo được đề tài (smoke #5) |
| Rủi ro khi gộp | 4 bản **major** có thể làm hỏng build | Thấp |

**Quyết định:** làm **P2-01 trước** (`e465e30`), sau đó xử lý Dependabot (`a60693b`) trước khi sửa tiếp frontend — để mọi task sau được kiểm thử trên thư viện mới.

| PR | Nâng cấp | Kết quả |
|---|---|---|
| #4, #5, #6 | `actions/checkout` 7, `setup-java` 6, `setup-node` 7 | ✅ Áp dụng |
| #8 | Maven wrapper 3.10.0 | ✅ Áp dụng |
| #9 | `jjwt` 0.13.0 | ✅ Áp dụng — 0 thay đổi API, test JWT đạt |
| #10, #11, #12 | Nhóm npm *minor/patch*, `eslint` 10 + `@eslint/js` 10, `globals` 17 | ✅ Áp dụng — sửa 1 lỗi lint mới (`preserve-caught-error`); 34 cảnh báo mới của React Compiler để mức *warn*, xử lý ở **P3-11** |
| #7 | `springdoc-openapi` 3.x | ❌ Từ chối — yêu cầu Spring Boot 4 (dự án dùng 3.5) |
| #13 | `@vitejs/plugin-react` 6 | ❌ Từ chối — yêu cầu Vite 8 (dự án dùng rolldown-vite 7) |

Đồng thời `.github/dependabot.yml`: thêm `target-branch: develop` cho cả 3 hệ sinh thái và bỏ qua bản major của 2 gói trên → PR sau này đi đúng quy trình `develop` → `main`.

### 12.2. Kết quả từng task

| Task | Đã làm | Commit | Kiểm thử |
|---|---|---|---|
| **P2-01** | Quyền `TOPIC_MANAGE` cho giảng viên; initializer chỉ gán quyền mặc định khi tạo vai trò lần đầu (không ghi đè chỉnh sửa của Admin); bước dữ liệu một lần cho DB cũ; người hướng dẫn phải là tài khoản giảng viên | `e465e30` | `TopicManagementIT` (8) |
| **P2-02** | 401 → về đăng nhập kèm "phiên hết hạn"; 403 → chỉ thông báo, giữ đăng nhập; toast lỗi toàn cục (mạng, 5xx); tự đăng xuất đúng lúc JWT hết hạn | `e9fe4a9` | `ui-check` mục 6–7 |
| **P2-03** | Nộp báo cáo kèm file đúng `fileUrl`; tải file bằng nút có token (thay 5 thẻ `<a href>`); nginx nhận file tới 25 MB; bỏ proxy `/uploads` | `33d2cac` | smoke #11, #12, #13, #21; `ui-check` mục 8 |
| **P2-04** | Nhiều SV/đề tài theo `capacity` (D-01 = B): khóa dòng đề tài khi duyệt (không vượt chỗ khi duyệt đồng thời), `OPEN ⇄ FULL` tự cập nhật, `COMPLETED` khi mọi SV có điểm, unique `(student_id, topic_id)`; thẻ đề tài hiện "2/3 chỗ" | `90ba967` | `TopicCapacityIT` (6), smoke #9b |
| **P2-05** | Quên mật khẩu 3 bước, mã 10 phút qua email, không dò được tài khoản, giới hạn 3 lần/15 phút/tài khoản + 10 lần/IP; đặt lại xong thu hồi mọi phiên cũ; audit | `50024ce`, `65bbbf0` | `PasswordResetIT` (7), smoke #20 |
| **P2-06** | Email gửi **sau khi giao dịch commit**, bất đồng bộ (pool `mail-`), thử lại 3 lần; mẫu HTML tiếng Việt cho mọi email (escape dữ liệu người dùng) + bản văn bản thuần | `f812ea1` | `EmailDispatchIT` (5), `EmailTemplatesTest` (4); đăng nhập vẫn 0,13–0,39 s khi SMTP treo |
| **P2-07** | Nhắc tiến độ bằng **một** truy vấn (thay `findAll` + N+1), chống trùng 7 ngày, giờ Việt Nam; **mới:** nhắc hạn mốc còn 2 ngày (WARNING) và ngày cuối (CRITICAL), mỗi mức 1 lần; kèm email | `1668ce9` | `ReminderServiceIT` (4) |
| **P2-08** | So trùng lặp giữ chữ có dấu (NFC, `\p{L}`), bỏ hư từ, tính cả cặp tiếng; so với mọi báo cáo trong cùng đợt; lưu `similarity_score`, `similar_to_report_id`; GV thấy badge "Trùng lặp X% với báo cáo #…", SV không thấy | `5cc8832` | `SimilarityServiceTest` (6), `ProgressSimilarityIT` |
| **P2-09** | 12 ràng buộc: điểm 0–10 lẻ 0,25; không ghi đè `status` khi sửa đề tài; `capacity` 1–10 (không nhỏ hơn số SV đã duyệt); deadline sai → 400 (trước đây bị bỏ qua); milestone/đợt kiểm tra ngày; tin nhắn/thảo luận/lịch/thông báo kiểm tra id + độ dài; form frontend kiểm tra sớm | `7ef3e3f` | `BusinessValidationIT` (10) |
| **P2-10** | Route `/admin/roles` + mục sidebar; không xóa 4 vai trò hệ thống; vai trò còn người dùng → 400 kèm số người | `5936d04`, `78dad5e` | `RoleServiceTest` (+4), `ui-check-gd2` |
| **P2-11** | Xóa ProjectPermission (D-07): entity/repo/controller/DTO; bảng `project_permissions` tự xóa nếu rỗng (DB dev + Docker đều 0 dòng) | `16aa680` | `RemovedFeaturesIT` (2) — `/api/permissions` → 404 |
| **P2-13** | Khóa điểm khi đợt `CLOSED` (D-08); yêu cầu chấm lại → quản trị khoa/Admin duyệt (mở khóa 1–168 giờ) → GV sửa **một lần** (bắt buộc lý do) → khóa lại; quá hạn → `EXPIRED` (job mỗi giờ); lịch sử điểm; thông báo + email + audit; Excel thêm cột "Đã chấm lại"; giao diện: badge 🔒, nút yêu cầu, tab duyệt, modal lịch sử | `d2c279b` | `GradeRevisionIT` (7), `ui-check-gd2` (11 mục) |
| **P2-12** | `FullLifecycleIT`: toàn bộ vòng đời qua API (tạo đợt → … → chấm lại); chặn 500 ở các API quản trị còn lại | `ea56b02` | 1 test end-to-end, `BusinessValidationIT` (+1) |

### 12.3. Lỗi phát hiện thêm khi kiểm thử giao diện — đã sửa

| # | Lỗi | Mức độ | Sửa trong |
|---|---|---|---|
| 1 | Form tạo đề tài của giảng viên **không gửi đợt (workspace)** → luôn 400; cũng không có ô số SV | 🔴 Chặn chức năng | P2-09: thêm chọn đợt (`GET /api/workspaces/open-for-topics`) + số SV |
| 2 | Tạo đề tài nhận nguyên entity từ body: gửi kèm `id` sẽ **ghi đè đề tài khác** | 🟠 Toàn vẹn dữ liệu | P2-09: luôn tạo mới |
| 3 | Trang Vai trò dùng `axios` trần — không gửi token → luôn 401 | 🔴 | `78dad5e` |
| 4 | Trang Duyệt đăng ký không tự tải dữ liệu (phải bấm "Làm mới"), ô chọn đề tài là ô nhập mã mặc định "1"; tên SV / người duyệt hiển thị trống, tìm kiếm không hoạt động (đọc sai trường DTO) | 🟠 | P2-13 |
| 5 | Trang Đăng ký của tôi (SV): tên GV trống, **không hiển thị điểm** | 🟡 | P2-13 |
| 6 | Gán lớp, phân công GV, tạo user/lớp: id thiếu/sai → 500 (`Long.valueOf("null")`) | 🟡 | P2-12 |
| 7 | Thông báo tiến độ/điểm viết không dấu ("Sinh vien gui bao cao") | 🟢 | P2-08, P2-09 |

### 12.4. Kết quả kiểm thử cuối (code `ea56b02`)

| Hạng mục | Kết quả |
|---|---|
| Test backend | **267/267** đạt (đầu GĐ2: 202) — 0 cảnh báo `javac` |
| Phân tích mã thừa (ECJ: import/biến/hàm không dùng, dead code) | **0** cảnh báo (main + test) |
| Smoke test toàn luồng (`scripts/smoke-test.mjs`) | **OK=17 · FIXED=12 · BUG=0 · FAIL=0** (29 kịch bản; 6 BUG của GĐ2 → FIXED) |
| Giao diện (`ui-check.mjs`) | 13/13 đạt |
| Giao diện GĐ2 (`ui-check-gd2.mjs`, mới) | **18/18** đạt — tạo đề tài, trang vai trò, toàn bộ luồng khóa điểm/chấm lại |
| WebSocket (`ws-check.mjs`) | Đạt |
| ESLint | 0 lỗi · 34 cảnh báo (quy tắc mới React Compiler — P3-11) · build Vite đạt |

Ảnh chụp: `scripts/ui-check-gd2.mjs` lưu `Plan/reports/ui-gd2-*.png` khi chạy trên máy bạn (cần chạy `smoke-test.mjs` trước).

### 12.5. Còn tồn tại — đề xuất

| # | Vấn đề | Mức độ | Đề xuất |
|---|---|---|---|
| 1 | Container backend chạy múi giờ **UTC**: thời gian lưu và hiển thị lệch 7 giờ so với giờ Việt Nam (ví dụ lịch sử điểm ghi 09:38 thay vì 16:38). Lịch nhắc việc đã chạy đúng giờ Việt Nam | 🟡 | Quyết định trước P3-01: đặt `TZ=Asia/Ho_Chi_Minh` cho backend (+ chuyển dữ liệu cũ) **hoặc** trả thời gian kèm múi giờ cho frontend. Đề xuất đưa vào P3-08/P3-12 |
| 2 | Bảng/cột mới tạo bằng `ddl-auto` (`grade_revision_requests`, `grade_history`, `data_migrations`, 2 cột trùng lặp) và 3 bước dữ liệu trong `DataMigrationRunner` | 🟡 | Chuyển hết sang Flyway ở **P3-01** |
| 3 | 34 cảnh báo lint React Compiler | 🟢 | **P3-11** (tách trang lớn) |
| 4 | Chưa có giao diện tạo/sửa milestone (chỉ có API) | 🟢 | **P4-05** |
| 5 | Tab "Báo cáo số liệu" của quản trị khoa còn số liệu cứng (84,2%, "06") | 🟢 | **P4-01** |
| 6 | PR Dependabot #7, #13 (major không tương thích) và các PR cũ nhắm `main` | 🟢 | Sau khi merge `develop` → `main`: PR đã có bản mới hơn sẽ tự đóng; PR nào còn mở thì đóng thủ công kèm lý do ở 12.1 |

### 12.6. Việc chủ dự án cần làm để đóng Giai đoạn 2

1. Trên GitHub: tạo PR `develop` → `main`, chờ CI xanh (backend test, frontend lint/build, CodeQL), **merge bằng merge commit**.
2. Tạo tag: `git fetch origin && git tag -a v0.3.0-business origin/main -m "Giai đoạn 2: sửa lỗi nghiệp vụ & chức năng" && git push origin v0.3.0-business`.
3. Trên máy: `git checkout develop && git pull` — DB cũ được tự nâng cấp khi khởi động (thêm bảng mới, xóa `project_permissions` nếu rỗng, tính lại trạng thái đề tài).
4. Đóng các PR Dependabot còn mở (mục 12.5 #6).

**Bước tiếp theo sau đó: Giai đoạn 3 — Củng cố kỹ thuật**, bắt đầu bằng **P3-01 Flyway** (gom mọi thay đổi schema của GĐ1–GĐ2 vào migration, bỏ `ddl-auto`).

---

## 13. Rà soát giao diện, giờ Việt Nam, Flyway — 23/10/2026

> Theo yêu cầu: kiểm tra và sửa triệt để các lỗi giao diện ở mục 12.3, xử lý 2 vấn đề còn tồn tại ở mục 12.5 (#1 múi giờ, #2 `ddl-auto`).
> Commit trên `develop`: `d73b1e7` → `69a2f2e`.

### 13.1. Rà soát lại 6 lỗi đã sửa + tìm lỗi cùng loại

Cách làm: đối chiếu **mọi trường dữ liệu** mà từng trang/thành phần đọc với DTO backend thật sự trả về; tìm mọi chỗ gọi API không qua instance chung; mọi trang không tự tải dữ liệu; mọi `@RequestBody` bind thẳng entity; mọi chỗ đọc/ép kiểu dữ liệu thô từ request.

| # | Lỗi đã sửa ở mục 12.3 | Kiểm tra lại |
|---|---|---|
| 1 | Form tạo đề tài thiếu chọn đợt | ✅ Đạt — `ui-check-gd2` tạo đề tài thật qua giao diện |
| 2 | Gửi kèm `id` ghi đè đề tài khác | ✅ Triệt để hơn: tạo/sửa đề tài nhận `TopicRequest` (chỉ tên, mô tả, số SV, đợt) thay vì bind thẳng entity |
| 3 | Trang Vai trò không gửi token | ✅ Không còn chỗ nào gọi API ngoài instance chung (`fetch`/`axios` trần) |
| 4 | Trang Duyệt đăng ký không tự tải, tên SV trống | ✅ Mọi trang đều tự tải khi mở |
| 5 | SV không thấy điểm | ✅ Đạt |
| 6 | API quản trị trả 500 khi thiếu id | ✅ Triệt để hơn: thêm bộ xử lý lỗi chung (`InputErrorHandler`) cho mọi lỗi sai kiểu/định dạng chưa kiểm tra riêng → 400 tiếng Việt; lỗi lập trình → 500 thông báo chung, không lộ thông điệp nội bộ Java |

**Lỗi cùng loại tìm thêm và đã sửa:**

| # | Lỗi | Mức độ |
|---|---|---|
| 7 | Tab "Duyệt Đề tài" (quản trị khoa): cột giảng viên luôn trống (đọc `t.lecturer.user`, API trả `lecturerName`) | 🟠 |
| 8 | Sửa đề tài: ô "Đợt đồ án" luôn hiện "—" (danh sách đề tài không trả tên đợt) | 🟡 |
| 9 | Kênh trao đổi: tin nhắn của đề tài khác lẫn vào (API tin nhắn không trả đề tài nên bộ lọc luôn cho qua) | 🟠 |
| 10 | Chuông thông báo: số chưa đọc luôn là 0 cho tới khi bấm mở | 🟡 |
| 11 | Liên kết thông báo "Sinh viên gửi báo cáo" mở trang tiến độ nhưng không chọn đúng đề tài | 🟡 |
| 12 | "Báo cáo số liệu" của khoa: **số liệu cứng** (84,2%, "06", "OK") → nay là tỉ lệ lấp đầy thật và số GV chưa được phân công | 🟠 |
| 13 | API thống kê khoa trả **500** khi tài khoản chưa thuộc khoa (`Map.of` không nhận `null`) | 🟡 |
| 14 | Hộp thư nuốt mọi lỗi CSDL và trả danh sách rỗng (che sự cố) | 🟢 |
| 15 | Gửi JSON `null` cho tên/mã → lưu thành chữ `"null"`; `privilegeIds` sai kiểu → 500 | 🟢 |

Test mới: `UiDataContractIT` (5), `InputErrorHandlerTest` (2); `ui-check-gd2.mjs` +8 kiểm tra.

### 13.2. Đồng bộ giờ Việt Nam

**Nguyên nhân thật:** chuỗi kết nối DB có `serverTimezone=UTC` nên driver MySQL **luôn lưu thời gian theo UTC**. Điều này đúng cho cả khi chạy bằng Docker lẫn chạy trên Windows bằng `run-backend.ps1`. Chỉ có container Docker chạy JVM theo UTC, nên giờ trả cho giao diện là giờ UTC (lệch 7 tiếng).

**Cách sửa:**
- `AppTimeZone`: JVM chạy `Asia/Ho_Chi_Minh` ngay khi khởi động. Đổi được bằng `APP_TIME_ZONE`.
- Docker: thêm `APP_TIME_ZONE` + `TZ` cho backend, để log cũng theo giờ Việt Nam. Jackson và test cũng chạy cùng múi giờ.
- Hộp thư chuyển từ SQL thuần sang JPQL, vì SQL thuần bỏ qua bước quy đổi giờ.
- **Không cần chuyển dữ liệu cũ** (vốn đã lưu UTC). **Giữ nguyên** `serverTimezone=UTC` trong `DB_URL`.

**Kiểm chứng:**
- `TimeZoneIT`: 16:38 giờ Việt Nam được lưu thành 09:38 UTC và API trả lại đúng 16:38.
- `ui-check-gd2`: giờ sửa điểm hiển thị đúng giờ Việt Nam.
- Container: `date` → `+07`.

### 13.3. P3-01 — Flyway thay `ddl-auto=update` (task đầu tiên của Giai đoạn 3)

| Migration | Nội dung |
|---|---|
| `V1__baseline_schema.sql` | 28 bảng, sinh từ schema Hibernate của mã hiện tại. Viết dạng "chỉ tạo/thêm khi còn thiếu": DB trống thì tạo đủ; DB cũ thì bổ sung cột, chỉ mục, khóa ngoại, giá trị ENUM còn thiếu. Thay luôn `SchemaFixer` |
| `V2__seed_roles_privileges.sql` | Quyền + 4 vai trò; quyền mặc định chỉ gán cho vai trò mới tạo (thay `RolePrivilegeInitializer`) |
| `V3__data_fixes_gd2.sql` | 3 bước dữ liệu của GĐ2 (thay `DataMigrationRunner`). Bước đã chạy trước đây được bỏ qua nhờ mốc trong `data_migrations` |

- Hibernate nay chỉ **kiểm tra** schema (`ddl-auto=validate`). DB chưa có Flyway được đánh dấu version 0 rồi chạy V1 → V3.
- Đã thử nâng cấp thật DB tạo bởi **`v0.1.0-baseline`**, **`v0.2.0-security`** (bản đang ở `main`) và DB Docker có dữ liệu. Ở cả 3, schema sau nâng cấp **giống hệt** DB tạo mới. Các thay đổi dữ liệu đi kèm:
  - đề tài `REGISTERED` được tính lại theo số chỗ;
  - giảng viên được thêm quyền `TOPIC_MANAGE`;
  - bảng ProjectPermission rỗng bị xóa;
  - tài khoản cũ được kích hoạt sẵn.
- Test: `FlywayMigrationIT` (3), chạy trên CSDL thử nghiệm riêng. CI dùng tài khoản root để tạo CSDL này.
- **Quy tắc từ nay:** muốn thay đổi schema thì viết migration mới (V4, V5…). Không sửa V1–V3, không bật lại `ddl-auto=update`.

### 13.4. Kết quả kiểm thử cuối (code `69a2f2e`)

| Hạng mục | Kết quả |
|---|---|
| Test backend | **278/278** — 0 cảnh báo `javac`, ECJ 0 cảnh báo (main + test) |
| Smoke test | **OK=17 · FIXED=12 · BUG=0 · FAIL=0** (29 kịch bản) |
| Giao diện | `ui-check` 13/13 · `ui-check-gd2` **26/26** · `ws-check` đạt |
| ESLint / build | 0 lỗi · 34 cảnh báo (P3-11) · build đạt |

### 13.5. Còn lại

| # | Vấn đề | Đề xuất |
|---|---|---|
| 1 | Flyway 11.7 (theo Spring Boot 3.5) cảnh báo "MySQL 8.4 mới hơn phiên bản Flyway hỗ trợ" — chỉ là cảnh báo, migration chạy đúng | Nâng khi Spring Boot đưa Flyway mới (Dependabot sẽ báo) |
| 2 | MySQL cảnh báo `tinyint(1)` (độ rộng hiển thị) khi chạy V1 lần đầu — cố ý giữ để cột `approved` vẫn là kiểu boolean | Không cần xử lý |
| 3 | 34 cảnh báo lint React Compiler; milestone chưa có giao diện tạo | P3-11, P4-05 |

**Việc của bạn** (thay mục 12.6):
1. Merge `develop` → `main`, tạo tag `v0.3.0-business`.
2. Lần đầu chạy code mới trên máy, Flyway tự nâng cấp DB; nên **sao lưu DB trước**:
   `docker exec ktpm-db sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" doan_ltmmt' > backup.sql`
3. Nếu `Backend/.env` có `DB_URL` thì giữ nguyên `serverTimezone=UTC` (mặc định của `init-env.ps1`).
