import api from '../api';
import { downloadFrom } from './download';

const clean = (params) => Object.fromEntries(Object.entries(params || {}).filter(([, v]) => v !== '' && v != null));

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
    },
    /** P4-01 — thống kê tổng hợp cho ADMIN / quản trị khoa; params: { departmentId, workspaceId }. */
    getOverview: async (params) => {
        const res = await api.get('/dashboard/overview', { params: clean(params) });
        return res.data;
    },
    exportOverview: (params) => downloadFrom('/dashboard/overview/export', { params: clean(params), fallbackName: 'thong-ke.xlsx' }),
    getStudentStats: async () => {
        const res = await api.get('/dashboard/student');
        return res.data;
    },
};
