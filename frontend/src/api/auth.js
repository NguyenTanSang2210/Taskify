import api from "../api";

export const authApi = {
  // Bước 1. Trả về { nextStep: "NONE" | "OTP" | "CHANGE_PASSWORD", token?, preAuthToken?, maskedEmail?, ... }
  login: async (username, password) => {
    const res = await api.post("/auth/login", { username, password });
    return res.data;
  },
  // Bước 2 (vai trò cần OTP). Người dùng được xác định từ preAuthToken, KHÔNG gửi username.
  verifyOtp: async (preAuthToken, code) => {
    const res = await api.post("/auth/otp/verify", { preAuthToken, code });
    return res.data;
  },
  resendOtp: async (preAuthToken) => {
    const res = await api.post("/auth/otp/resend", { preAuthToken });
    return res.data;
  },
  // Đổi mật khẩu bắt buộc lần đầu (token phạm vi PWD_CHANGE). Trả về bước kế tiếp như login.
  changeInitialPassword: async (preAuthToken, newPassword) => {
    const res = await api.post("/auth/password/initial", { preAuthToken, newPassword });
    return res.data;
  },
  // Kích hoạt tài khoản do quản trị tạo: identifier = tên đăng nhập hoặc email.
  activate: async (identifier, code, newPassword) => {
    const res = await api.post("/auth/activate", { identifier, code, newPassword });
    return res.data;
  },
  // Quên mật khẩu: gửi mã về email (luôn trả cùng một thông báo, kể cả khi tài khoản không tồn tại)
  forgotPassword: async (identifier) => {
    const res = await api.post("/auth/password/forgot", { identifier });
    return res.data;
  },
  resetPassword: async (identifier, code, newPassword) => {
    const res = await api.post("/auth/password/reset", { identifier, code, newPassword });
    return res.data;
  },
};
