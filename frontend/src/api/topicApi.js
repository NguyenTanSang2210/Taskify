import api from "../api";

export const topicApi = {
  // params: { studentId, query, status, lecturerId, page, size }
  getAllTopics: async (params = {}) => {
    const res = await api.get("/topics", { params });
    return res.data;
  },
  // P4-11: chi tiết một đề tài (thành viên đề tài thấy thêm danh sách sinh viên)
  getDetail: async (id) => {
    const res = await api.get(`/topics/${id}`);
    return res.data;
  },
  create: async (lecturerId, payload) => {
    const res = await api.post("/topics/create", payload, { params: { lecturerId } });
    return res.data;
  },
  update: async (id, lecturerId, payload) => {
    const res = await api.put(`/topics/${id}`, payload, { params: { lecturerId } });
    return res.data;
  },
  remove: async (id, lecturerId) => {
    const res = await api.delete(`/topics/${id}`, { params: { lecturerId } });
    return res.data;
  },
  open: async (id, lecturerId) => {
    const res = await api.post(`/topics/${id}/open`, null, { params: { lecturerId } });
    return res.data;
  },
  // Các đợt đang mở nhận đề tài (để chọn khi tạo đề tài)
  workspacesOpenForTopics: async () => {
    const res = await api.get("/workspaces/open-for-topics");
    return res.data;
  },
  close: async (id, lecturerId) => {
    const res = await api.post(`/topics/${id}/close`, null, { params: { lecturerId } });
    return res.data;
  },
};
