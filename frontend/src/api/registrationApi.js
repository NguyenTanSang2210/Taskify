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

  // reason: bắt buộc khi sửa điểm đã có (P2-13)
  grade: async (regId, score, feedback, reason) => {
    const res = await api.post(`/registration/grade/${regId}`, { score, feedback, reason: reason || undefined });
    return res.data;
  },

  // --- Chấm lại (P2-13) ---
  requestRevision: async (regId, reason) => {
    const res = await api.post(`/registration/${regId}/grade-revisions`, { reason });
    return res.data;
  },

  gradeHistory: async (regId) => {
    const res = await api.get(`/registration/${regId}/grade-history`);
    return res.data;
  },

  listRevisions: async (params = {}) => {
    const res = await api.get("/grade-revisions", { params });
    return res.data;
  },

  approveRevision: async (id, note, unlockHours) => {
    const res = await api.post(`/grade-revisions/${id}/approve`, { note, unlockHours });
    return res.data;
  },

  rejectRevision: async (id, note) => {
    const res = await api.post(`/grade-revisions/${id}/reject`, { note });
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
