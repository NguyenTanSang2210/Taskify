import api from '../api';

// Đợt đồ án (workspace), lớp gán vào đợt và phân công giảng viên theo đợt
export const workspaceApi = {
    list: async () => (await api.get('/workspaces')).data,
    create: async (payload) => (await api.post('/workspaces', payload)).data,
    transition: async (workspaceId, to) =>
        (await api.post(`/workspaces/${workspaceId}/transition`, null, { params: { to } })).data,
    listClasses: async (workspaceId) => (await api.get('/workspace-classes', { params: { workspaceId } })).data,
    assignClass: async (workspaceId, classId) =>
        (await api.post('/workspace-classes/assign', { workspaceId, classId })).data,
    unassignClass: async (workspaceId, classId) =>
        (await api.post('/workspace-classes/unassign', { workspaceId, classId })).data,
    listAssignments: async (workspaceId) => (await api.get('/assignments', { params: { workspaceId } })).data,
    assignLecturer: async (payload) => (await api.post('/assignments/assign', payload)).data,
    revokeAssignment: async (assignmentId, reason) =>
        (await api.post('/assignments/revoke', { assignmentId, reason })).data,
};
