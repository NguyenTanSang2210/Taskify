// src/api.js
import axios from "axios";
import { showToast } from "./toast";

const FORBIDDEN_MESSAGE = "Bạn không có quyền thực hiện thao tác này.";
// Thông báo lỗi tiếng Việt từ backend thì giữ nguyên; thông báo tiếng Anh chung chung thì thay bằng câu dễ hiểu
const isVietnamese = (text) => /[À-ỹđĐ]/.test(text || "");

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

    // Chỉ 401 (chưa đăng nhập / token hết hạn / bị thu hồi) mới đăng xuất.
    // 403 = đã đăng nhập nhưng không đủ quyền: chỉ báo lỗi, KHÔNG đá người dùng ra màn hình đăng nhập.
    if (!isAuthEndpoint && status === 401) {
      try {
        localStorage.removeItem("token");
        localStorage.removeItem("user");
      } catch { void 0; }
      if (window.location?.pathname !== "/login") {
        window.location.assign("/login?reason=expired");
      }
    }

    let message =
      error?.response?.data?.message ||
      error?.response?.data?.error ||
      error?.message ||
      "Yêu cầu thất bại";

    if (status === 403) {
      if (!isVietnamese(message)) message = FORBIDDEN_MESSAGE;
      showToast("warning", message);
    } else if (!isAuthEndpoint && !axios.isCancel(error) && (!status || status >= 500)) {
      // Mất kết nối / lỗi máy chủ: báo chung cho mọi trang (trang có thể hiển thị thêm thông báo riêng)
      showToast("danger", status ? "Máy chủ gặp lỗi, vui lòng thử lại sau." : "Không kết nối được máy chủ. Kiểm tra mạng và thử lại.");
    }
    const normalizedError = new Error(message);
    normalizedError.status = status;
    normalizedError.url = (error?.config?.baseURL || "") + (error?.config?.url ?? url);
    normalizedError.data = error?.response?.data;
    // P3-03: mã lỗi ổn định (vd. TOPIC_FULL) và lỗi theo trường {field: message} để form hiển thị cạnh ô nhập
    normalizedError.code = error?.response?.data?.code;
    normalizedError.fieldErrors = Object.fromEntries(
      (error?.response?.data?.errors || []).map((e) => [e.field, e.message])
    );
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
