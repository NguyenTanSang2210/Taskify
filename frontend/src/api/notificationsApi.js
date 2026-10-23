import api from "../api";

export const notificationsApi = {
  // Trang { items, page, size, totalElements, totalPages }, mới nhất trước
  listMine: async (userId, { page = 0, size = 20 } = {}) => {
    const res = await api.get(`/notifications/mine`, { params: { userId, page, size } });
    return res.data;
  },
  // N thông báo mới nhất (LIMIT ở SQL) — cho chuông thông báo
  recent: async (userId, limit = 6) => {
    const res = await api.get(`/notifications/recent`, { params: { userId, limit } });
    return res.data;
  },
  countUnread: async (userId) => {
    const res = await api.get(`/notifications/count`, { params: { userId } });
    return res.data;
  },
  markRead: async (id) => {
    const res = await api.post(`/notifications/read/${id}`);
    return res.data;
  },
  markAllRead: async (userId) => {
    await api.post(`/notifications/read-all`, null, { params: { userId } });
  },
};
