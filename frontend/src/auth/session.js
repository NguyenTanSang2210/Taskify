// P3-14 — phiên đăng nhập phía trình duyệt.
// Access token (15 phút) CHỈ nằm trong bộ nhớ (biến module) — không còn trong localStorage, nên mã độc chèn vào
// trang (XSS) không đọc được token đã lưu. Refresh token nằm trong cookie HttpOnly do máy chủ quản lý (JavaScript
// không đọc được); gọi /api/auth/refresh để lấy access token mới khi hết hạn hoặc khi tải lại trang.
import axios from "axios";

let accessToken = null;
const listeners = new Set();
let inFlight = null;

export const getAccessToken = () => accessToken;

export function setAccessToken(token) {
  accessToken = token || null;
  listeners.forEach((fn) => fn(accessToken));
}

/** Đăng ký nghe đổi token (AuthContext, WebSocket). Trả về hàm hủy đăng ký. */
export function onAccessTokenChange(fn) {
  listeners.add(fn);
  return () => listeners.delete(fn);
}

/**
 * Làm mới phiên: một lần duy nhất dù nhiều request cùng gặp 401 (các request chờ chung một promise).
 * Dùng axios "trần" (không qua interceptor của api.js) để không lặp vô hạn khi chính refresh bị 401.
 * Trả về { token, userId, username, fullName, role } hoặc ném lỗi nếu phiên đã hết.
 */
export function refreshSession() {
  if (!inFlight) {
    inFlight = axios
      .post("/api/auth/refresh", null, { withCredentials: true })
      .then((res) => {
        setAccessToken(res.data?.token);
        return res.data;
      })
      .catch((err) => {
        setAccessToken(null);
        throw err;
      })
      .finally(() => {
        inFlight = null;
      });
  }
  return inFlight;
}

/** Đăng xuất phía máy chủ (thu hồi refresh token, xóa cookie); lỗi mạng không chặn việc đăng xuất cục bộ. */
export async function revokeSession() {
  try {
    await axios.post("/api/auth/logout", null, { withCredentials: true });
  } catch {
    /* vẫn đăng xuất cục bộ */
  }
  setAccessToken(null);
}

/** Thời điểm hết hạn (ms) đọc từ claim `exp` của JWT; null nếu không đọc được. */
export function tokenExpiresAt(jwt) {
  try {
    const payload = jwt.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    const exp = JSON.parse(atob(payload)).exp;
    return typeof exp === "number" ? exp * 1000 : null;
  } catch {
    return null;
  }
}
