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
  /** P4-05: bảng nộp bài + điểm theo mốc { milestones, weightTotal, canGrade, students[{ cells[], weightedScore, complete }] } */
  board: async (topicId) => {
    const res = await api.get("/milestones/board", { params: { topicId } });
    return res.data;
  },
  grade: async (milestoneId, studentId, payload) => {
    const res = await api.put(`/milestones/${milestoneId}/grades/${studentId}`, payload);
    return res.data;
  },
};
