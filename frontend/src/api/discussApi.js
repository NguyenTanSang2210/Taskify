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
  posts: async (threadId) => {
    const res = await api.get(`/discuss/threads/${threadId}/posts`);
    return res.data;
  },
  addPost: async (threadId, payload) => {
    const res = await api.post(`/discuss/threads/${threadId}/posts`, payload);
    return res.data;
  },
};
