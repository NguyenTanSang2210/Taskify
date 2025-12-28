import api from "../api";

export const authApi = {
  login: async (username, password) => {
    const res = await api.post("/auth/login", { username, password });
    return res.data;
  },
  verifyOtp: async (username, code) => {
    const res = await api.post("/auth/otp/verify", { username, code });
    return res.data;
  },
  register: async (data) => {
    const res = await api.post("/auth/register", data);
    return res.data;
  }
};
