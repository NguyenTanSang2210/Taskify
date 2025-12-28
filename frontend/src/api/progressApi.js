import api from "../api";

export const progressApi = {
  create: async (studentId, topicId, payload) => {
    const res = await api.post("/progress/create", payload, {
      params: { studentId, topicId },
    });
    return res.data;
  },
  listByStudent: async (studentId, topicId) => {
    const res = await api.get(`/progress/student/${studentId}`, {
      params: { topicId },
    });
    return res.data;
  },
  listByTopic: async (topicId) => {
    const res = await api.get(`/progress/topic/${topicId}`);
    return res.data;
  },
  comment: async (id, { lecturerComment, status, deadline }) => {
    const res = await api.put(`/progress/${id}/comment`, { lecturerComment, status, deadline });
    return res.data;
  },
};
