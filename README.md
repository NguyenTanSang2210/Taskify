# KTPM - He thong quan ly de tai va cong viec nhom

## 1. Gioi thieu
Day la he thong ho tro quan ly de tai hoc thuat va cong viec nhom theo vai tro:
- ADMIN
- DEPARTMENT_ADMIN
- LECTURER
- STUDENT

He thong tap trung vao 4 nhom chuc nang:
- Quan ly workspace theo vong doi.
- Quan ly de tai va dang ky de tai.
- Theo doi tien do, milestone, nhan xet giang vien.
- Cong tac nhom realtime (thong bao, thao luan, tin nhan).

## 2. Kien truc tong quan
- Backend: Spring Boot 3.4.12, Java 21, Spring Security + JWT, Spring Data JPA, WebSocket STOMP/SockJS, MySQL.
- Frontend: React 19 + Vite, React Router, Axios, Bootstrap.

So do luong co ban:
1. Frontend gui request den backend qua REST API /api/*.
2. Backend xac thuc bang JWT va phan quyen theo role.
3. Cac su kien nghiep vu duoc day realtime qua STOMP endpoint /ws.

## 3. Cau truc thu muc
```
KTPM/
  Backend/        # Spring Boot API + WebSocket
  frontend/       # React Vite UI
  docs/           # Tai lieu ky thuat phuc vu bao cao
```

## 4. Chuc nang chinh theo vai tro
### ADMIN
- Quan tri nguoi dung, role, trang thai tai khoan.
- Quan sat dashboard tong quan.

### DEPARTMENT_ADMIN
- Tao workspace, chuyen trang thai workspace.
- Quan ly lop hoc thuat, gan lop vao workspace.
- Phan cong giang vien vao workspace.

### LECTURER
- Tao/cap nhat/trang thai de tai.
- Duyet, tu choi, cham diem dang ky de tai.
- Nhan xet tien do sinh vien.

### STUDENT
- Dang ky de tai.
- Nop bao cao tien do theo de tai/milestone.
- Nhan thong bao va cap nhat realtime.

## 5. Chuan bao mat cau hinh
Backend da duoc tach profile va cau hinh theo bien moi truong:
- `application.properties`: cau hinh chung.
- `application-dev.properties`: profile local development.
- `application-prod.properties`: profile production.
- `Backend/.env.example`: mau bien moi truong.

Luu y:
- Khong commit file `.env` that.
- Khong hardcode secret DB/JWT/mail trong source.
- Seed du lieu demo chi chay khi profile `dev` va bat co `APP_SEED_ENABLED=true`.

## 6. Huong dan cai dat va chay
### 6.1. Yeu cau
- Java 21
- Maven Wrapper (da co trong repo)
- Node.js 20+ (khuyen nghi LTS)
- MySQL 8+

### 6.2. Backend
Tu thu muc `Backend/`:

1. Tao bien moi truong theo mau trong `Backend/.env.example`.
2. Chay:
```powershell
.\mvnw.cmd spring-boot:run
```

Kiem tra nhanh:
```powershell
.\mvnw.cmd -q -DskipTests test-compile
.\mvnw.cmd -q test
```

### 6.3. Frontend
Tu thu muc `frontend/`:

```powershell
npm install
npm run dev
```

Mac dinh frontend chay tai `http://localhost:5173`.

### 6.4. Script nhanh cho demo
Tu thu muc goc `KTPM/`:

```powershell
# Kiem tra toan bo backend + frontend truoc demo
.\scripts\pre-demo-check.ps1

# Chay backend
.\scripts\run-backend.ps1

# Chay frontend
.\scripts\run-frontend.ps1
```

## 7. Tai lieu ky thuat
- Tai lieu bao cao chi tiet: `docs/TECHNICAL_REPORT.md`.
- Huong dan frontend module: `frontend/README.md`.

## 8. Kiem thu nhanh truoc demo
1. Backend compile + test pass.
2. Frontend build pass (`npm run build`).
3. Login theo cac vai tro hoat dong.
4. Dang ky de tai, cap nhat tien do, thong bao realtime hoat dong.

## 9. Gioi han hien tai
- Chua co pipeline CI/CD day du.
- Chua tich hop bo do bao phu test (coverage gate) vao quy trinh release.
- README hien mo ta local deployment, chua mo ta cloud deployment chi tiet.
