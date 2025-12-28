import api from "../api";

export const notificationsApi = {
  listMine: async (userId) => {
    const res = await api.get(`/notifications/mine`, { params: { userId } });
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
