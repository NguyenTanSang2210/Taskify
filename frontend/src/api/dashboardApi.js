import api from '../api';

export const dashboardApi = {
    getAdminStats: async () => {
        const res = await api.get('/dashboard/admin');
        return res.data;
    },
    getLecturerStats: async () => {
        const res = await api.get('/dashboard/lecturer');
        return res.data;
    },
    getDepartmentAdminStats: async () => {
        const res = await api.get('/dashboard/department-admin');
        return res.data;
    }
};
