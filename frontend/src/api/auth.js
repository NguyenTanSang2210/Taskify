import api from "../api";

export const authApi = {
  // Bước 1. Trả về { nextStep: "NONE" | "OTP", token?, preAuthToken?, maskedEmail?, ... }
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
  register: async (data) => {
    const res = await api.post("/auth/register", data);
    return res.data;
  }
};
