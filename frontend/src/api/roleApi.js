import axios from "axios";

const API_URL = "/api/roles";

export const roleApi = {
  getAllRoles: async () => {
    const res = await axios.get(API_URL);
    return res.data;
  },
  getAllPrivileges: async () => {
    const res = await axios.get(`${API_URL}/privileges`);
    return res.data;
  },
  createRole: async (data) => {
    const res = await axios.post(API_URL, data);
    return res.data;
  },
  updateRole: async (id, data) => {
    const res = await axios.put(`${API_URL}/${id}`, data);
    return res.data;
  },
  deleteRole: async (id) => {
    await axios.delete(`${API_URL}/${id}`);
  },
  createPrivilege: async (data) => {
    const res = await axios.post(`${API_URL}/privileges`, data);
    return res.data;
  }
};
