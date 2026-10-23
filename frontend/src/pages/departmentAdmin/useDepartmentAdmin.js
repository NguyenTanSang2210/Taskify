import { useCallback, useEffect, useMemo, useState } from 'react';
import api from '../../api';
import userApi from '../../api/userApi';
import { topicApi } from '../../api/topicApi';
import { useLocation } from 'react-router-dom';
import { getWorkspaceStatusLabel, NEXT_WORKSPACE_STATUS } from './helpers';

/** Trạng thái + xử lý của DepartmentAdminPage (tách ở P3-11). */
export function useDepartmentAdmin() {
    const location = useLocation();
    // ?tab=grade-revisions: mở thẳng tab duyệt chấm lại (liên kết từ thông báo)
    const [activeTab, setActiveTab] = useState(() =>
        new URLSearchParams(location.search).get('tab') === 'grade-revisions' ? 'gradeRevisions' : 'workspaces');
    const [loading, setLoading] = useState(false);
    const [notice, setNotice] = useState(null);

    const [topics, setTopics] = useState([]);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);

    const [workspaces, setWorkspaces] = useState([]);
    const [classes, setClasses] = useState([]);

    const [wsCreate, setWsCreate] = useState({ name: '', type: '', semester: '', startAt: '', endAt: '' });
    const [classCreate, setClassCreate] = useState({ code: '', name: '' });

    const [selectedWorkspaceId, setSelectedWorkspaceId] = useState('');
    const [workspaceClasses, setWorkspaceClasses] = useState([]);

    const [lecturers, setLecturers] = useState([]);
    const [assignments, setAssignments] = useState([]);
    const [assignForm, setAssignForm] = useState({ lecturerId: '', type: 'MAIN', quotaMaxGroups: 0, reason: '' });
    const [deptStats, setDeptStats] = useState(null);

    const selectedWorkspace = useMemo(() => {
        const id = Number(selectedWorkspaceId);
        return workspaces.find(w => w.id === id) || null;
    }, [selectedWorkspaceId, workspaces]);

    const selectedWorkspaceStatus = selectedWorkspace?.status ?? '';

    const canConfigureSelectedWorkspace = useMemo(() => {
        return selectedWorkspaceStatus === 'DRAFT' || selectedWorkspaceStatus === 'OPEN_TOPIC';
    }, [selectedWorkspaceStatus]);

    const loadTopics = useCallback(async () => {
        setLoading(true);
        try {
            const data = await topicApi.getAllTopics({ page, size: 10 });
            setTopics(data.items);
            setTotalPages(data.totalPages);
        } catch (error) {
            console.error('Failed to load topics', error);
        } finally {
            setLoading(false);
        }
    }, [page]);

    const toggleTopicStatus = async (t) => {
        // Đề tài còn mở hoặc đã đủ SV → đóng; đã đóng → mở lại (backend tự chuyển FULL nếu vẫn đủ SV)
        const nextAction = t.status === 'OPEN' || t.status === 'FULL' ? 'CLOSED' : 'OPEN';
        if (!window.confirm(`Xác nhận ${nextAction === 'OPEN' ? 'mở' : 'đóng'} đề tài này?`)) return;
        try {
            if (nextAction === 'OPEN') {
                await topicApi.open(t.id, null);
            } else {
                await topicApi.close(t.id, null);
            }
            setNotice({ type: 'success', message: 'Cập nhật trạng thái đề tài thành công.' });
            loadTopics();
        } catch {
            setNotice({ type: 'danger', message: 'Không thể cập nhật đề tài.' });
        }
    };

    const loadWorkspaces = useCallback(async () => {
        setLoading(true);
        try {
            const res = await api.get('/workspaces');
            setWorkspaces(res.data || []);
        } catch (error) {
            console.error('Failed to load workspaces', error);
        } finally {
            setLoading(false);
        }
    }, []);

    const loadClasses = useCallback(async () => {
        setLoading(true);
        try {
            const res = await api.get('/classes');
            setClasses(res.data || []);
        } catch (error) {
            console.error('Failed to load classes', error);
        } finally {
            setLoading(false);
        }
    }, []);

    const createWorkspace = async () => {
        const name = wsCreate.name.trim();
        if (!name) {
            setNotice({ type: 'warning', message: 'Vui lòng nhập tên cho Không gian làm việc.' });
            return;
        }
        if (wsCreate.startAt && wsCreate.endAt && wsCreate.endAt < wsCreate.startAt) {
            setNotice({ type: 'warning', message: 'Ngày kết thúc phải sau ngày bắt đầu.' });
            return;
        }
        setLoading(true);
        try {
            const payload = {
                name,
                type: wsCreate.type.trim() || null,
                semester: wsCreate.semester.trim() || null,
                startAt: wsCreate.startAt || null,
                endAt: wsCreate.endAt || null
            };
            await api.post('/workspaces', payload);
            setNotice({ type: 'success', message: 'Đã khởi tạo Không gian làm việc mới.' });
            setWsCreate({ name: '', type: '', semester: '', startAt: '', endAt: '' });
            await loadWorkspaces();
        } catch (error) {
            setNotice({ type: 'danger', message: error.message || 'Lỗi tạo workspace' });
        } finally {
            setLoading(false);
        }
    };

    const transitionWorkspace = async (workspaceId, to) => {
        if (!window.confirm(`Xác nhận chuyển trạng thái sang ${getWorkspaceStatusLabel(to)}?`)) return;
        setLoading(true);
        try {
            await api.post(`/workspaces/${workspaceId}/transition`, null, { params: { to } });
            setNotice({ type: 'success', message: 'Cập nhật trạng thái chu kỳ thành công.' });
            await loadWorkspaces();
        } catch (error) {
            setNotice({ type: 'danger', message: error.message || 'Lỗi chuyển đổi trạng thái' });
        } finally {
            setLoading(false);
        }
    };

    const createClass = async () => {
        const code = classCreate.code.trim();
        const name = classCreate.name.trim();
        if (!code || !name) {
            setNotice({ type: 'warning', message: 'Mã lớp và tên lớp là bắt buộc.' });
            return;
        }
        setLoading(true);
        try {
            await api.post('/classes', { code, name });
            setNotice({ type: 'success', message: 'Lớp học học thuật mới đã được thêm vào hệ thống.' });
            setClassCreate({ code: '', name: '' });
            await loadClasses();
        } catch {
            setNotice({ type: 'danger', message: 'Không thể khởi tạo lớp học.' });
        } finally {
            setLoading(false);
        }
    };

    const loadWorkspaceClasses = useCallback(async (workspaceId) => {
        if (!workspaceId) {
            setWorkspaceClasses([]);
            return;
        }
        setLoading(true);
        try {
            const res = await api.get('/workspace-classes', { params: { workspaceId } });
            setWorkspaceClasses(res.data || []);
        } catch (error) {
            console.error('Failed to load workspace classes', error);
        } finally {
            setLoading(false);
        }
    }, []);

    const assignClassToWorkspace = async (workspaceId, classId) => {
        setLoading(true);
        try {
            await api.post('/workspace-classes/assign', { workspaceId, classId });
            await loadWorkspaceClasses(workspaceId);
        } catch {
            setNotice({ type: 'danger', message: 'Lỗi gán lớp.' });
        } finally {
            setLoading(false);
        }
    };

    const unassignClassFromWorkspace = async (workspaceId, classId) => {
        setLoading(true);
        try {
            await api.post('/workspace-classes/unassign', { workspaceId, classId });
            await loadWorkspaceClasses(workspaceId);
        } catch {
            setNotice({ type: 'danger', message: 'Lỗi gỡ lớp.' });
        } finally {
            setLoading(false);
        }
    };

    const loadLecturers = useCallback(async () => {
        setLoading(true);
        try {
            // size tối đa của API (100): đủ cho danh sách chọn giảng viên của một khoa
            const data = await userApi.getByDepartment({ role: 'LECTURER', size: 100 });
            setLecturers(data?.items || []);
        } catch (error) {
            console.error('Failed to load lecturers', error);
        } finally {
            setLoading(false);
        }
    }, []);

    const loadAssignments = useCallback(async (workspaceId) => {
        if (!workspaceId) {
            setAssignments([]);
            return;
        }
        setLoading(true);
        try {
            const res = await api.get('/assignments', { params: { workspaceId } });
            setAssignments(res.data || []);
        } catch (error) {
            console.error('Failed to load assignments', error);
        } finally {
            setLoading(false);
        }
    }, []);

    const loadDeptStats = useCallback(async () => {
        setLoading(true);
        try {
            const res = await api.get('/dashboard/department-admin');
            setDeptStats(res.data || null);
        } catch (error) {
            console.error('Failed to load department stats', error);
        } finally {
            setLoading(false);
        }
    }, []);

    const assignLecturer = async () => {
        const workspaceId = Number(selectedWorkspaceId);
        const lecturerId = Number(assignForm.lecturerId);
        if (!workspaceId || !lecturerId) {
            setNotice({ type: 'warning', message: 'Vui lòng chọn Giảng viên.' });
            return;
        }
        setLoading(true);
        try {
            await api.post('/assignments/assign', {
                workspaceId,
                lecturerId,
                type: assignForm.type,
                quotaMaxGroups: Number(assignForm.quotaMaxGroups) || 0,
                reason: assignForm.reason?.trim() || null
            });
            setNotice({ type: 'success', message: 'Phân công và đăng ký quota thành công.' });
            setAssignForm({ lecturerId: '', type: 'MAIN', quotaMaxGroups: 0, reason: '' });
            await loadAssignments(workspaceId);
        } catch (error) {
            setNotice({ type: 'danger', message: error.message || 'Lỗi gán giảng viên' });
        } finally {
            setLoading(false);
        }
    };

    const revokeAssignment = async (assignmentId) => {
        const reason = window.prompt('Lý do thu hồi (tuỳ chọn):') || '';
        setLoading(true);
        try {
            await api.post('/assignments/revoke', { assignmentId, reason: reason.trim() || null });
            setNotice({ type: 'success', message: 'Đã thu hồi quyền tham gia của giảng viên.' });
            await loadAssignments(Number(selectedWorkspaceId));
        } catch {
            setNotice({ type: 'danger', message: 'Lỗi thu hồi phân công.' });
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        if (activeTab === 'workspaces') {
            loadWorkspaces();
        } else if (activeTab === 'classes') {
            loadClasses();
        } else if (activeTab === 'workspaceClasses') {
            loadWorkspaces();
            loadClasses();
            if (selectedWorkspaceId) loadWorkspaceClasses(Number(selectedWorkspaceId));
        } else if (activeTab === 'assignments') {
            loadWorkspaces();
            loadLecturers();
            if (selectedWorkspaceId) loadAssignments(Number(selectedWorkspaceId));
        } else if (activeTab === 'topics') {
            loadTopics();
        } else if (activeTab === 'stats') {
            loadDeptStats();
        }
    }, [activeTab, loadWorkspaces, loadClasses, loadWorkspaceClasses, selectedWorkspaceId, loadLecturers, loadAssignments, loadTopics, loadDeptStats]);

    useEffect(() => {
        if (!selectedWorkspaceId) {
            setWorkspaceClasses([]);
            setAssignments([]);
        }
    }, [selectedWorkspaceId]);

    const activeClassIds = useMemo(() => {
        return new Set((workspaceClasses || []).filter(x => x.active).map(x => x.academicClass?.id));
    }, [workspaceClasses]);

    // Bước kế tiếp trong vòng đời đợt (khớp WorkspaceStatus.canTransitionTo ở backend)
    const nextWorkspaceStatus = (status) => NEXT_WORKSPACE_STATUS[status] ?? null;

    const availableClassesToAssign = useMemo(() => {
        return classes.filter(c => c.active && !activeClassIds.has(c.id));
    }, [classes, activeClassIds]);

    const fillRateLabel = deptStats?.fillRate == null ? '—' : `${String(deptStats.fillRate).replace('.', ',')}%`;

    return { activeTab, setActiveTab, loading, setLoading, notice, setNotice, topics, setTopics, page, setPage, totalPages, setTotalPages, workspaces, setWorkspaces, classes, setClasses, wsCreate, setWsCreate, classCreate, setClassCreate, selectedWorkspaceId, setSelectedWorkspaceId, workspaceClasses, setWorkspaceClasses, lecturers, setLecturers, assignments, setAssignments, assignForm, setAssignForm, deptStats, setDeptStats, location, selectedWorkspace, selectedWorkspaceStatus, canConfigureSelectedWorkspace, loadTopics, toggleTopicStatus, loadWorkspaces, loadClasses, createWorkspace, transitionWorkspace, createClass, loadWorkspaceClasses, assignClassToWorkspace, unassignClassFromWorkspace, loadLecturers, loadAssignments, loadDeptStats, assignLecturer, revokeAssignment, activeClassIds, nextWorkspaceStatus, availableClassesToAssign, fillRateLabel };
}
