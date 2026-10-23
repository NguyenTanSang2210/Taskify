import api from '../api';
const userApi = {
    get: async (id) => {
        const response = await api.get(`/users/${id}`);
        return response.data;
    },
    updateProfile: async (id, payload) => {
        const response = await api.put(`/users/${id}/profile`, payload);
        return response.data;
    },
    // Server trả token mới cho phiên hiện tại (các phiên khác bị thu hồi)
    changePassword: async (id, payload) => {
        const response = await api.put(`/users/${id}/password`, payload);
        return response.data;
    },
    // Trả về trang { items, page, size, totalElements, totalPages } — params: { role, q, page, size }
    getAll: async (params = {}) => {
        const response = await api.get('/users', { params });
        return response.data;
    },
    create: async (payload) => {
        try {
            const response = await api.post('/users', payload);
            return response.data;
        } catch (error) {
            if (error?.status === 404 || error?.status === 405) {
                const response = await api.post('/users/create', payload);
                return response.data;
            }
            throw error;
        }
    },
    // Trả về trang — params: { departmentId, role, q, page, size }
    getByDepartment: async (params = {}) => {
        const response = await api.get('/users/by-department', { params });
        return response.data;
    },
    updateStatus: async (id, active) => {
        const response = await api.put(`/users/${id}/status`, null, { params: { active } });
        return response.data;
    },
    getRoles: async () => {
        const response = await api.get('/users/roles');
        return response.data;
    },
    resendActivation: async (id) => {
        const response = await api.post(`/users/${id}/resend-activation`);
        return response.data;
    },
    updateRole: async (id, roleId) => {
        const response = await api.put(`/users/${id}/role`, null, { params: { roleId } });
        return response.data;
    }
};

export default userApi;
