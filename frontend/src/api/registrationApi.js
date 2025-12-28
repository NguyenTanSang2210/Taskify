import api from "../api";

export const registrationApi = {
  registerTopic: async (studentId, topicId) => {
    const res = await api.post("/registration/register", null, {
      params: { studentId, topicId },
    });
    return res.data;
  },

  getByTopic: async (topicId) => {
    const res = await api.get(`/registration/topic/${topicId}`);
    return res.data;
  },

  approve: async (regId) => {
    const res = await api.post(`/registration/approve/${regId}`);
    return res.data;
  },

  reject: async (regId, reason) => {
    const res = await api.post(`/registration/reject/${regId}`, null, {
      params: { reason },
    });
    return res.data;
  },

  getMine: async (studentId) => {
    const res = await api.get(`/registration/mine`, {
      params: { studentId },
    });
    return res.data;
  },

  cancel: async (regId) => {
    const res = await api.post(`/registration/cancel/${regId}`);
    return res.data;
  },

  grade: async (regId, score, feedback) => {
    const res = await api.post(`/registration/grade/${regId}`, { score, feedback });
    return res.data;
  },

  exportExcel: async (topicId) => {
    const res = await api.get("/export/excel", {
      params: { topicId },
      responseType: "blob",
    });
    return res.data;
  },
};
