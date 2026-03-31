# Frontend - KTPM

## 1. Tong quan
Frontend la ung dung React + Vite, cung cap giao dien cho 4 vai tro:
- ADMIN
- DEPARTMENT_ADMIN
- LECTURER
- STUDENT

Muc tieu cua frontend:
- Hien thi du lieu nghiep vu theo role.
- Goi REST API den backend qua `/api/*`.
- Nhan cap nhat realtime qua WebSocket `/ws`.

## 2. Cong nghe chinh
- React 19
- Vite
- React Router
- Axios
- Bootstrap + Bootstrap Icons
- STOMP/SockJS (realtime)

## 3. Cau truc va trach nhiem module
- `src/pages`: cac man hinh nghiep vu theo role va quy trinh.
- `src/components`: thanh phan dung lai (navbar, bang, notice, progress row, ...).
- `src/api`: lop giao tiep backend theo domain (`topicApi`, `registrationApi`, ...).
- `src/context`: quan ly auth state va thong tin user dang dang nhap.
- `src/ws`: ket noi STOMP/SockJS va subscribe thong diep realtime.
- `src/App.jsx`: khai bao route va role guard cap ung dung.

## 4. Luong hoat dong so luoc
1. User dang nhap tu trang login.
2. Frontend goi API auth, luu `token` + `user` vao local storage/context.
3. Axios interceptor gan `Authorization` cho request can bao mat.
4. Router dieu huong man hinh theo role.
5. Cac man hinh goi API theo module nghiep vu.
6. Neu co cap nhat realtime, frontend nhan qua `/ws` va refresh UI.

## 5. Chay frontend local
Tu thu muc `frontend/`:

```powershell
npm ci
npm run dev
```

Mac dinh chay tai: `http://localhost:5173`

## 6. Build, lint, preview
```powershell
npm run lint
npm run build
npm run preview
```

## 7. Quy uoc phat trien frontend
- Uu tien de logic nghiep vu o lop `api`/`service`, component tap trung render.
- Moi route moi phai kiem tra role guard va quyen truy cap.
- API file dat ten theo domain de de truy vet va bao tri.

## 8. Tham khao
- Tong quan du an: `../README.md`
- Tai lieu backend: `../Backend/README.md`
- Bao cao ky thuat: `../docs/TECHNICAL_REPORT.md`