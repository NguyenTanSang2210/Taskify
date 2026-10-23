# Bảo mật Taskify

## 1. Báo cáo lỗ hổng

**Không** mở issue công khai cho lỗ hổng bảo mật. Hãy gửi qua [GitHub Security Advisories](https://github.com/NguyenTanSang2210/Taskify/security/advisories/new) (tab *Security → Report a vulnerability*), gồm:

- mô tả lỗ hổng và ảnh hưởng (ai khai thác được, lấy được gì);
- các bước tái hiện (request mẫu, tài khoản/vai trò dùng để thử);
- phiên bản hoặc commit đã thử.

Chủ dự án phản hồi trong vòng 7 ngày, thống nhất thời điểm công bố sau khi có bản sửa. Chỉ thử trên môi trường của chính bạn (stack Docker cục bộ với dữ liệu demo), không thử trên hệ thống đang phục vụ người dùng thật.

Phiên bản được vá: nhánh `main` mới nhất.

## 2. Xác thực và phiên đăng nhập

| Cơ chế | Chi tiết |
|---|---|
| Mật khẩu | BCrypt; tối thiểu 8 ký tự, có chữ và số, không chứa tên đăng nhập, giới hạn độ dài theo byte (`PasswordPolicy`) |
| OTP 2 bước | Bắt buộc với `ADMIN`, `DEPARTMENT_ADMIN`, `LECTURER`. Sau mật khẩu chỉ nhận **preAuthToken** (10 phút, không gọi được API nghiệp vụ); access token chỉ cấp sau khi OTP đúng. Mỗi mã tối đa 5 lần nhập sai |
| Tài khoản mới | Không có đăng ký công khai ([ADR-0002](adr/0002-tat-dang-ky-cong-khai.md)). Tài khoản do Admin tạo, kích hoạt bằng mã gửi qua email; quên mật khẩu dùng mã một lần |
| Access token | JWT HS256, sống **15 phút**, chỉ nằm trong bộ nhớ của trang (không `localStorage`) ([ADR-0003](adr/0003-access-token-ngan-han-refresh-cookie.md)) |
| Refresh token | 256 bit ngẫu nhiên, CSDL chỉ lưu **hash SHA-256**; cookie `taskify_refresh` `HttpOnly; SameSite=Strict; Path=/api/auth; Secure` (prod); sống 7 ngày; **xoay vòng** mỗi lần dùng |
| Phát hiện đánh cắp | Dùng lại refresh token đã xoay vòng (quá 30 giây ân hạn) ⇒ thu hồi **mọi** phiên của tài khoản và tăng `tokenVersion` (access token cũ mất hiệu lực ngay) |
| Thu hồi | Đăng xuất thu hồi phiên hiện tại; "đăng xuất mọi thiết bị", đổi mật khẩu, khóa tài khoản, đổi vai trò đều tăng `tokenVersion` |
| CSRF | API stateless dùng header `Authorization`; riêng `/api/auth/refresh` và `/logout` (dùng cookie) kiểm tra header `Origin` thuộc danh sách CORS và cookie `SameSite=Strict` |
| Chống dò mật khẩu | Khóa theo cặp (tên đăng nhập, IP) sau nhiều lần sai, lưu trong CSDL (`login_throttle`) nên không mất khi khởi động lại |
| Giới hạn tần suất (mặc định, đổi qua biến `APP_RATE_*`) | đăng nhập 10/phút, xác thực OTP 10/phút, gửi lại OTP 5/10 phút, kích hoạt 10/10 phút, upload 30/10 phút, làm mới phiên 300/phút. Vượt ⇒ `429` + `Retry-After` |

`JWT_SECRET` bắt buộc là chuỗi base64 của ≥ 32 byte ngẫu nhiên; profile `prod` từ chối khởi động nếu thiếu hoặc dùng secret mẫu trong repo.

## 3. Phân quyền

Hai lớp kiểm tra:

1. **Vai trò/quyền** ở `SecurityConfig` và `@PreAuthorize` (quyền chi tiết như `TOPIC_MANAGE`, quản lý ở trang *Vai trò & quyền*).
2. **Phạm vi dữ liệu** ở service: `SecurityScopeService` (chỉ dữ liệu trong khoa của mình) và `TopicAccessService` (giảng viên hướng dẫn / sinh viên đã được duyệt của đề tài). Mọi truy cập theo id đều qua lớp này để chặn IDOR; WebSocket kiểm tra cùng quy tắc khi `SUBSCRIBE`.

Response là DTO (không trả entity), nên không lộ hash mật khẩu hay trường nội bộ; request là record có validate, nên không gán được trường ngoài ý muốn (mass assignment).

### Ma trận phân quyền

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

Các quy tắc quan trọng của ma trận (IDOR theo id, phạm vi khoa, thành viên đề tài, kênh WebSocket, endpoint công khai) có test tích hợp ở `Backend/src/test/java/.../security/` (`*AccessIT`, `SecurityEndpointsIT`…), kiểm tra cả chiều "được phép" lẫn "bị chặn".

## 4. Dữ liệu và file

- **File tải lên**: tối đa 20 MB; chỉ 13 đuôi được phép (pdf, doc(x), xls(x), ppt(x), zip, rar, png, jpg/jpeg, txt) **và** nội dung (magic bytes) phải khớp đuôi; tên lưu là UUID, ngoài thư mục web; chỉ tải được qua API có kiểm tra quyền (`Content-Disposition: attachment`). File mồ côi được dọn định kỳ.
- **Lỗi API** không chứa stacktrace, câu SQL hay thông điệp nội bộ — chỉ `code` + thông báo tiếng Việt + `requestId` để tra log ([`error-codes.md`](error-codes.md)).
- **Log** không ghi mật khẩu, OTP, token; mọi thao tác quan trọng được ghi `audit_logs` kèm IP + trình duyệt (chỉ Admin xem, trang *Nhật ký hệ thống*). Bản ghi cũ hơn 12 tháng bị xóa tự động, trừ nhật ký điểm số (`GRADE_*`). File CSV xuất ra vô hiệu hóa công thức Excel (CSV injection).
- **Secret** (DB, JWT, mail) chỉ đọc từ biến môi trường; `.env` không được commit (đã có trong `.gitignore`). Lịch sử git đã được làm sạch file cá nhân ([ADR-0004](adr/0004-viet-lai-lich-su-git.md)).

## 5. Lớp web

- nginx gắn header: `Content-Security-Policy` (chỉ `'self'` + Google Fonts), `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Referrer-Policy`, `Permissions-Policy`.
- CORS chỉ chấp nhận origin trong `APP_CORS_ALLOWED_ORIGINS` (có `allowCredentials` cho cookie refresh).
- Actuator chỉ mở `health` và `info`; chi tiết health không công khai.
- Khi triển khai: chỉ mở cổng nginx (HTTPS) ra ngoài, không mở cổng backend/MySQL — xem [`DEPLOYMENT.md`](DEPLOYMENT.md).

## 6. Quy trình bảo trì

- Dependabot mở PR cập nhật dependency hằng tuần (vào `develop`); CodeQL quét mã nguồn mỗi tuần và trên mọi PR.
- Trước mỗi bản phát hành: `npm audit --omit=dev` = 0 lỗ hổng, CI xanh (bao gồm test phân quyền).
