import api from "../api";

export const discussApi = {
  threads: async (topicId) => {
    const res = await api.get("/discuss/threads", { params: { topicId } });
    return res.data;
  },
  createThread: async (payload) => {
    const res = await api.post("/discuss/threads", payload);
    return res.data;
  },
  // Trang bài viết, cũ → mới
  posts: async (threadId, { page = 0, size = 50 } = {}) => {
    const res = await api.get(`/discuss/threads/${threadId}/posts`, { params: { page, size } });
    return res.data;
  },
  addPost: async (threadId, payload) => {
    const res = await api.post(`/discuss/threads/${threadId}/posts`, payload);
    return res.data;
  },
};
