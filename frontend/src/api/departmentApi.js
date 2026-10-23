import api from '../api';

// Khoa và lớp học thuật
export const departmentApi = {
    list: async () => (await api.get('/departments')).data,
    create: async (payload) => (await api.post('/departments', payload)).data,
    // departmentId bỏ trống: lớp của khoa người đang đăng nhập
    listClasses: async (departmentId) =>
        (await api.get('/classes', { params: departmentId == null ? undefined : { departmentId } })).data,
    createClass: async (payload) => (await api.post('/classes', payload)).data,
};
