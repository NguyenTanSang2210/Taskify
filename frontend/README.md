# Frontend - KTPM

## 1. Tong quan
Frontend duoc xay dung bang React + Vite, cung cap giao dien theo vai tro:
- ADMIN
- DEPARTMENT_ADMIN
- LECTURER
- STUDENT

Frontend ket noi backend qua REST API (`/api/*`) va realtime qua STOMP/SockJS (`/ws`).

## 2. Cau truc module
- `src/pages`: man hinh theo nghiep vu va role.
- `src/components`: component dung lai (role guard, table, notice, ...).
- `src/api`: adapter goi backend theo module.
- `src/context`: quan ly auth state.
- `src/ws`: ket noi realtime.

## 3. Chay local
Tu thu muc `frontend/`:

```powershell
npm install
npm run dev
```

Mac dinh Vite chay tai `http://localhost:5173`.

## 4. Build va lint
```powershell
npm run lint
npm run build
npm run preview
```

## 5. Quan ly auth tren frontend
- Luu `token` va `user` trong localStorage qua `AuthContext`.
- Tu dong them header `Authorization: Bearer <token>` qua axios interceptor.
- Tu dong chuyen ve trang login khi gap 401/403 (ngoai tru endpoint auth).

## 6. Realtime
Frontend subscribe cac kenh STOMP theo ngu canh:
- Progress theo topic.
- Registration theo topic.
- Notification theo user.

## 7. Luong nghiep vu chinh tren UI
1. Dang nhap va phan nhanh vao dashboard theo role.
2. Quan tri workspace/lop/phan cong (DEPARTMENT_ADMIN).
3. Quan ly de tai + duyet dang ky (LECTURER).
4. Dang ky de tai + nop bao cao tien do (STUDENT).

## 8. Quy uoc phat trien
- Dat ten API theo module nghiep vu (`topicApi`, `registrationApi`, ...).
- Han che logic nghiep vu nang trong component, uu tien tach ra API/service layer.
- Moi thay doi route can kiem tra role guard trong `App.jsx`.

## 9. Tham khao
- Tai lieu tong quan project: `../README.md`
- Tai lieu ky thuat chi tiet: `../docs/TECHNICAL_REPORT.md`