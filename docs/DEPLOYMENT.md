# Triển khai Taskify

Hướng dẫn chạy Taskify cho người dùng thật trên một máy chủ Linux bằng Docker Compose. Đây là bản chuẩn bị cho P4-09: tự động sao lưu, giám sát và CD sẽ được bổ sung ở task đó.

## 1. Yêu cầu

| Thành phần | Tối thiểu |
|---|---|
| Máy chủ | 2 vCPU, 4 GB RAM, 20 GB ổ đĩa (dữ liệu + file tải lên + log) |
| Phần mềm | Docker Engine 24+ với Compose v2 (`docker compose`), `git` |
| Tên miền | Một tên miền trỏ về máy chủ, ví dụ `taskify.example.edu.vn` |
| HTTPS | **Bắt buộc** ở profile `prod`: cookie phiên đăng nhập có cờ `Secure` nên trình duyệt không gửi nó qua HTTP |
| Email | Tài khoản SMTP gửi được OTP và thông báo (Gmail cần *App Password*) |

## 2. Cài đặt lần đầu

```bash
git clone https://github.com/NguyenTanSang2210/Taskify.git && cd Taskify
git checkout v0.4.0-hardening          # luôn triển khai từ một tag, không từ nhánh đang phát triển

cp .env.docker.example .env
chmod 600 .env
```

Sửa `.env` (hoặc trên Windows: `.\scripts\init-env.ps1 -Profile prod` để tự sinh secret):

| Biến | Giá trị production |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `APP_SEED_ENABLED` | `false` (không tạo tài khoản demo) |
| `JWT_SECRET` | `openssl rand -base64 48` |
| `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD` | `openssl rand -base64 24` — đặt **trước** lần chạy đầu tiên; đổi sau khi volume đã tạo không có tác dụng |
| `APP_CORS_ALLOWED_ORIGINS` | `https://taskify.example.edu.vn` (đúng địa chỉ người dùng mở) |
| `APP_AUTH_REFRESH_COOKIE_SECURE` | `true` |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | thông tin SMTP thật |
| `MYSQL_HOST_PORT`, `BACKEND_HOST_PORT` | `127.0.0.1:3307`, `127.0.0.1:8081` — chỉ mở trên máy chủ, **không ra Internet** |
| `FRONTEND_HOST_PORT` | `127.0.0.1:5175` nếu dùng reverse proxy HTTPS ở mục 3 |

```bash
docker compose up -d --build --wait
docker compose ps                       # cả 3 container "healthy"/"running"
curl -fsS http://127.0.0.1:8081/actuator/health/readiness   # {"status":"UP"}
```

Flyway tự tạo schema ở lần khởi động đầu.

**Tài khoản Admin đầu tiên** (dữ liệu demo chỉ có ở profile `dev`): trước lần chạy đầu, đặt trong `.env`

```bash
APP_BOOTSTRAP_ADMIN_USERNAME=quantri
APP_BOOTSTRAP_ADMIN_EMAIL=quantri@truong.edu.vn
```

Khi backend khởi động mà chưa có tài khoản ADMIN nào, nó tạo tài khoản này ở trạng thái **chưa kích hoạt** và gửi mã kích hoạt tới email trên. Mở trang đăng nhập → *Kích hoạt tài khoản* → nhập mã và tự đặt mật khẩu. Không có mật khẩu nào nằm trong `.env`. Khi đã có Admin, hai biến này bị bỏ qua — xóa chúng đi sau lần chạy đầu. Kiểm tra trong log: `docker compose logs backend | grep "ADMIN đầu tiên"`.

## 3. HTTPS

Container `taskify-frontend` (nginx) phục vụ giao diện và chuyển `/api`, `/ws` vào backend. Đặt một reverse proxy có chứng chỉ phía trước nó. Ví dụ với [Caddy](https://caddyserver.com) (tự xin và gia hạn chứng chỉ Let's Encrypt):

```caddyfile
taskify.example.edu.vn {
    encode gzip
    reverse_proxy 127.0.0.1:5175
}
```

Yêu cầu với proxy bất kỳ (nginx, Traefik…):
- chuyển tiếp cả WebSocket (`Upgrade`/`Connection`) cho đường dẫn `/ws`;
- gửi `X-Forwarded-For` và `X-Forwarded-Proto: https` (backend dùng `forward-headers-strategy=native` để biết IP thật cho giới hạn tần suất);
- chỉ mở cổng 80/443 trên tường lửa.

## 4. Sao lưu và khôi phục

Dữ liệu cần sao lưu: database (volume `mysql_data`) và file tải lên (`Backend/uploads/`).

```bash
# Sao lưu (chạy hằng ngày bằng cron; giữ tối thiểu 7 bản, chép một bản ra ngoài máy chủ)
ts=$(date +%Y%m%d-%H%M)
docker exec taskify-db sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump -uroot --single-transaction --routines doan_ltmmt' \
  | gzip > backup/taskify-db-$ts.sql.gz
tar -czf backup/taskify-uploads-$ts.tar.gz -C Backend uploads

# Khôi phục (dừng backend trước để không có ghi mới)
docker compose stop backend
gunzip -c backup/taskify-db-<ts>.sql.gz | docker exec -i taskify-db sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot doan_ltmmt'
tar -xzf backup/taskify-uploads-<ts>.tar.gz -C Backend
docker compose start backend
```

Thử khôi phục định kỳ trên một máy khác — bản sao lưu chưa từng khôi phục thử thì chưa chắc dùng được.

## 5. Nâng cấp

```bash
# 1. Sao lưu (mục 4) — bắt buộc: migration Flyway chạy tự động khi backend khởi động
# 2. Lấy phiên bản mới
git fetch --tags && git checkout v<phiên-bản-mới>
# 3. Đọc ghi chú phát hành / Plan/reports: biến môi trường mới? migration mới?
diff <(grep -o '^[A-Z_]*' .env.docker.example | sort) <(grep -o '^[A-Z_]*' .env | sort)
# 4. Build và khởi động lại
docker compose up -d --build --wait
docker compose logs --tail=100 backend      # tìm "Successfully applied N migrations"
```

**Quay lui:** migration Flyway không tự đảo ngược. Muốn về phiên bản cũ: `git checkout v<cũ>`, khôi phục bản sao lưu vừa tạo ở bước 1 (mục 4), rồi `docker compose up -d --build`.

## 6. Vận hành hằng ngày

| Việc | Lệnh / nơi xem |
|---|---|
| Trạng thái | `docker compose ps`, `GET /actuator/health/readiness` (`/actuator/health` tổng còn gồm kết nối SMTP: DOWN ở đó mà readiness UP nghĩa là email đang lỗi) |
| Phiên bản đang chạy | `GET /actuator/info` |
| Log backend (JSON, có `requestId`) | `docker compose logs -f backend`; file xoay vòng trong volume `backend_logs` |
| Tra một lỗi người dùng báo | Lấy `requestId` trong body lỗi (hoặc header `X-Request-Id`) rồi `docker compose logs backend \| grep <requestId>` |
| Mở khóa tài khoản bị khóa do nhập sai mật khẩu | Tự mở sau thời gian khóa; hoặc Admin đặt lại mật khẩu |
| Lấy OTP khi email lỗi (khẩn cấp) | `.\scripts\get-otp.ps1 <username>` (đọc bảng `otp_tokens`) |

## 7. Checklist trước khi mở cho người dùng

- [ ] Truy cập qua `https://`; `http://` chuyển hướng sang HTTPS.
- [ ] Cổng 3307/8081 không truy cập được từ bên ngoài (`nmap` từ máy khác).
- [ ] `.env` quyền `600`, không nằm trong git; `JWT_SECRET` và mật khẩu DB là giá trị ngẫu nhiên mới.
- [ ] `APP_SEED_ENABLED=false`; không còn tài khoản demo (`student`, `lecturer`…).
- [ ] Gửi thử OTP và email quên mật khẩu tới hộp thư thật.
- [ ] Sao lưu chạy theo lịch và đã khôi phục thử thành công.
- [ ] Đăng nhập 4 vai trò, tải lại trang không bị đăng xuất, thông báo realtime hoạt động.
