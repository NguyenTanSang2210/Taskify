import api from '../api';
const userApi = {
    getAll: async () => {
        const response = await api.get('/users');
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
    updateRole: async (id, roleId) => {
        const response = await api.put(`/users/${id}/role`, null, { params: { roleId } });
        return response.data;
    }
};

export default userApi;
