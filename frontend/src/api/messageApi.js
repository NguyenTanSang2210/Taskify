import api from "../api";

export const messageApi = {
  // Trang tin nhắn gửi/nhận của tôi, mới nhất trước
  inbox: async ({ page = 0, size = 50 } = {}) => {
    const res = await api.get("/messages/inbox", { params: { page, size } });
    return res.data;
  },
  // Hội thoại với một người (tùy chọn trong một đề tài), mới nhất trước
  conversation: async (peerId, { topicId, page = 0, size = 50 } = {}) => {
    const res = await api.get(`/messages/conversation/${peerId}`, { params: { topicId: topicId || undefined, page, size } });
    return res.data;
  },
  send: async (payload) => {
    const res = await api.post("/messages/send", payload);
    return res.data;
  },
};
