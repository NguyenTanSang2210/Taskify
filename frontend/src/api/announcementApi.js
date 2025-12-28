import api from "../api";

export const announcementApi = {
  list: async (topicId) => {
    const res = await api.get("/announcements", { params: { topicId } });
    return res.data;
  },
  create: async (payload) => {
    const res = await api.post("/announcements", payload);
    return res.data;
  },
};
