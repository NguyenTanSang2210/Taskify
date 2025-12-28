import api from "../api";

export const calendarApi = {
  list: async (topicId) => {
    const res = await api.get("/calendar", { params: { topicId } });
    return res.data;
  },
  create: async (payload) => {
    const res = await api.post("/calendar", payload);
    return res.data;
  },
  delete: async (id) => {
    await api.delete(`/calendar/${id}`);
  },
};
