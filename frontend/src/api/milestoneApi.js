import api from "../api";

export const milestoneApi = {
  get: async (id) => {
    const res = await api.get(`/milestones/${id}`);
    return res.data;
  },
  list: async (topicId) => {
    const res = await api.get("/milestones", { params: { topicId } });
    return res.data;
  },
  create: async (payload) => {
    const res = await api.post("/milestones", payload);
    return res.data;
  },
  update: async (id, payload) => {
    const res = await api.put(`/milestones/${id}`, payload);
    return res.data;
  },
  delete: async (id) => {
    await api.delete(`/milestones/${id}`);
  },
};
