import api from "../api";

// Dùng instance chung để có header Authorization và xử lý 401/403 thống nhất
export const roleApi = {
  getAllRoles: async () => {
    const res = await api.get("/roles");
    return res.data;
  },
  getAllPrivileges: async () => {
    const res = await api.get("/roles/privileges");
    return res.data;
  },
  createRole: async (data) => {
    const res = await api.post("/roles", data);
    return res.data;
  },
  updateRole: async (id, data) => {
    const res = await api.put(`/roles/${id}`, data);
    return res.data;
  },
  deleteRole: async (id) => {
    await api.delete(`/roles/${id}`);
  },
  createPrivilege: async (data) => {
    const res = await api.post("/roles/privileges", data);
    return res.data;
  },
};
