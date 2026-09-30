// src/api.js
import axios from "axios";

// Luôn gọi API bằng đường dẫn tương đối "/api".
//  - Chạy dev (npm run dev): Vite proxy /api và /ws sang backend (xem vite.config.js).
//  - Chạy Docker: nginx proxy /api và /ws sang container backend (xem frontend/nginx.conf).
// Nhờ vậy trình duyệt không cần biết địa chỉ/cổng backend (trước đây gắn cứng localhost:8080
// nên frontend trong Docker — backend ở cổng 8081 — không đăng nhập được).
const api = axios.create({
  baseURL: "/api",
});

// Chuẩn hóa xử lý lỗi
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status;
    const url = error?.config?.url || "";

    // Không redirect đối với các endpoint auth (đặc biệt /auth/login)
    const isAuthEndpoint = url.includes("/auth/");

    if (!isAuthEndpoint && (status === 401 || status === 403)) {
      try {
        localStorage.removeItem("token");
        localStorage.removeItem("user");
      } catch { void 0; }
      if (window.location?.pathname !== "/login") {
        window.location.assign("/login");
      }
    }

    const message =
      error?.response?.data?.message ||
      error?.response?.data?.error ||
      error?.message ||
      "Yêu cầu thất bại";
    const normalizedError = new Error(message);
    normalizedError.status = status;
    normalizedError.url = (error?.config?.baseURL || "") + (error?.config?.url ?? url);
    normalizedError.data = error?.response?.data;
    return Promise.reject(normalizedError);
  }
);

// Đính kèm Authorization nếu có token
api.interceptors.request.use((config) => {
  let token = null;
  try {
    token = localStorage.getItem("token");
  } catch { void 0; }
  if (token) {
    config.headers = config.headers || {};
    config.headers["Authorization"] = `Bearer ${token}`;
  }
  return config;
});

export default api;
