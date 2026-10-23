# Taskify — Frontend

React 19 + Vite + Tailwind CSS. Kiến trúc: [`../docs/ARCHITECTURE.md`](../docs/ARCHITECTURE.md#frontend).

## Cấu trúc `src/`

| Thư mục | Nội dung |
|---|---|
| `routes/AppRoutes.jsx` | Khai báo route; mọi trang tải lười (`React.lazy`) |
| `layouts/`, `components/Sidebar.jsx`, `config/navigation.js` | Khung giao diện, menu theo vai trò |
| `pages/` | Mỗi màn hình một file; trang lớn có thư mục con chứa hook dữ liệu + component từng tab |
| `api/` | Mọi lời gọi HTTP theo domain (`topicApi`, `workspaceApi`, …) — trang không gọi axios trực tiếp |
| `api.js` | axios instance: gắn access token, làm mới phiên một lần khi gặp 401, chuẩn hóa lỗi (`code`, `message`, `fieldErrors`) |
| `auth/session.js`, `context/AuthContext.jsx` | Access token trong bộ nhớ, khôi phục phiên qua cookie khi tải lại trang, đồng bộ đăng xuất giữa các tab |
| `ws/stomp.js` | Kết nối STOMP/SockJS (tải lười) |
| `hooks/` | `useLoadEffect` (tải dữ liệu sau render), `useMediaQuery` |
| `constants/status.js` | Nhãn tiếng Việt cho các trạng thái |

## Lệnh

```powershell
npm ci
npm run dev        # http://localhost:5175, proxy /api và /ws tới http://localhost:8080
                   # dùng backend trong Docker: $env:VITE_PROXY_TARGET = "http://localhost:8081"
npm run check      # lint (0 cảnh báo) → test (Vitest) → build
npm run test:watch
```

Yêu cầu Node.js ≥ 22.12. Bản build (`dist/`) được nginx phục vụ trong image Docker (`Dockerfile`, `nginx.conf` — header bảo mật, CSP, proxy `/api`, `/ws`).

## Quy ước

- Tải dữ liệu: `useCallback` cho hàm tải + `useLoadEffect(load)`; giá trị suy ra được thì tính trong render, không đồng bộ bằng `useEffect`.
- Lỗi API: hiển thị `err.message` (tiếng Việt từ backend), lỗi từng ô nhập lấy từ `err.fieldErrors`.
- Trang mới: thêm route ở `AppRoutes.jsx` (bọc `RequireRole`) và mục menu ở `config/navigation.js`.
