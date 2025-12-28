import api from "../api";

export const messageApi = {
  inbox: async () => {
    const res = await api.get("/messages/inbox");
    return res.data;
  },
  send: async (payload) => {
    const res = await api.post("/messages/send", payload);
    return res.data;
  },
};
