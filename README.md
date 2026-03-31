# KTPM - He thong quan ly de tai va cong viec nhom

## 1. Tong quan du an
KTPM la he thong web ho tro quan ly de tai hoc thuat va cong viec nhom theo 4 vai tro:
- ADMIN
- DEPARTMENT_ADMIN
- LECTURER
- STUDENT

He thong gom 2 khoi chinh:
- Backend (`Backend/`): REST API + WebSocket + xu ly nghiep vu + phan quyen.
- Frontend (`frontend/`): giao dien nguoi dung theo role, ket noi API realtime.

Tai lieu module chi tiet:
- Backend: `Backend/README.md`
- Frontend: `frontend/README.md`

## 2. Cach he thong hoat dong
Luong tong quat:
1. Nguoi dung dang nhap tren frontend.
2. Frontend goi `POST /api/auth/login`, nhan JWT.
3. Frontend gan JWT vao header `Authorization: Bearer <token>` cho cac request tiep theo.
4. Backend xac thuc JWT va phan quyen theo role.
5. Du lieu nghiep vu duoc luu trong MySQL.
6. Su kien thong bao/tien do/thao luan duoc day realtime qua endpoint `/ws`.

## 3. Cau truc thu muc
```text
KTPM/
  Backend/                 # Spring Boot API + WebSocket
  frontend/                # React + Vite UI
  docs/                    # Tai lieu ky thuat
  scripts/                 # Script chay nhanh
  docker-compose.yml       # Chay full stack bang Docker
  .env.docker.example      # Mau bien moi truong cho Docker
```

## 4. Yeu cau moi truong (chay local khong Docker)
- Java 21
- Node.js 20+
- MySQL 8+

Luu y dong bo giua cac may:
- Dung `mvnw`/`mvnw.cmd` (Maven Wrapper), khong phu thuoc Maven global.
- Dung `npm ci` de cai dung version theo `package-lock.json`.
- Khong commit `.env` that, chi commit file mau `.env.example`.

## 5. Chay du an

### Cach 1: Docker Compose (khuyen nghi)
Tu thu muc goc `KTPM/`:

```powershell
# Tao file bien moi truong tu file mau
Copy-Item .env.docker.example .env

# Build va chay toan bo stack
docker compose up --build -d
```

Mac dinh theo file mau:
- Frontend: `http://localhost:5173`
- Backend API: `http://localhost:8081/api`
- MySQL: `localhost:3307`

Neu trung cong, sua cac bien trong `.env`:
- `FRONTEND_HOST_PORT`
- `BACKEND_HOST_PORT`
- `MYSQL_HOST_PORT`

Lenh quan ly:
```powershell
docker compose ps
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f db
docker compose down
docker compose down -v
```

### Cach 2: Chay local tung phan

Backend:
```powershell
cd Backend
.\mvnw.cmd spring-boot:run
```

Frontend:
```powershell
cd frontend
npm ci
npm run dev
```

Kiem tra nhanh:
```powershell
cd Backend
.\mvnw.cmd -q -DskipTests test-compile
.\mvnw.cmd -q test

cd ..\frontend
npm run build
```

### Script nhanh co san
Tu thu muc goc `KTPM/`:
```powershell
.\scripts\pre-demo-check.ps1
.\scripts\run-backend.ps1
.\scripts\run-frontend.ps1
```

## 6. Chuc nang nghiep vu chinh
- Quan ly workspace theo vong doi hoc ky.
- Quan ly de tai va dang ky de tai.
- Theo doi milestone, bao cao tien do.
- Thao luan nhom, tin nhan va thong bao realtime.

## 7. Tham khao
- `Backend/README.md`
- `frontend/README.md`
- `docs/TECHNICAL_REPORT.md`
