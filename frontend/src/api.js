// src/api.js
import axios from "axios";
import { showToast } from "./toast";
import { getAccessToken, refreshSession, revokeSession } from "./auth/session";

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
  withCredentials: true, // cookie refresh token (P3-14) — cùng origin qua proxy nên luôn được gửi
});

// Máy chủ vẫn từ chối sau khi đã làm mới phiên → kết thúc phiên ở CẢ hai phía (thu hồi cookie refresh) rồi về
// trang đăng nhập; nếu không, tải lại trang sẽ tự đăng nhập lại bằng cookie cũ và người dùng không thấy lý do.
let expiring = false;
const sessionExpired = () => {
  if (expiring) return;
  expiring = true;
  try {
    localStorage.removeItem("user");
  } catch { void 0; }
  revokeSession().finally(() => {
    if (window.location?.pathname !== "/login") window.location.assign("/login?reason=expired");
    else expiring = false;
  });
};

// Chuẩn hóa xử lý lỗi
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const status = error?.response?.status;
    const config = error?.config;
    const url = config?.url || "";

    // Không redirect đối với các endpoint auth (đặc biệt /auth/login)
    const isAuthEndpoint = url.includes("/auth/");

    // 401 (access token hết hạn / bị thu hồi): làm mới phiên MỘT lần rồi gửi lại request; vẫn lỗi → đăng xuất.
    // 403 = đã đăng nhập nhưng không đủ quyền: chỉ báo lỗi, KHÔNG đá người dùng ra màn hình đăng nhập.
    if (!isAuthEndpoint && status === 401) {
      if (config && !config._retried) {
        config._retried = true;
        try {
          await refreshSession();
          return api(config);
        } catch { /* phiên đã hết → đăng xuất bên dưới */ }
      }
      sessionExpired();
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

// Đính kèm Authorization nếu có access token (chỉ giữ trong bộ nhớ, xem auth/session.js)
api.interceptors.request.use((config) => {
  const token = getAccessToken();
  if (token) {
    config.headers = config.headers || {};
    config.headers["Authorization"] = `Bearer ${token}`;
  }
  return config;
});

export default api;
