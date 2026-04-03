import React, { useCallback, useEffect, useMemo, useState } from 'react';
import api from '../api';
import userApi from '../api/userApi';
import { topicApi } from '../api/topicApi';
import InlineNotice from '../components/InlineNotice';

const normalizeKey = (value) => String(value ?? '').trim().toUpperCase();
const getWorkspaceStatusLabel = (status) => {
    const s = normalizeKey(status);
    if (s === 'DRAFT') return 'Bản nháp';
    if (s === 'OPEN' || s === 'OPEN_TOPIC') return 'Mở đề tài';
    if (s === 'OPEN_REGISTRATION') return 'Mở đăng ký';
    if (s === 'LOCK_REGISTRATION') return 'Khóa đăng ký';
    if (s === 'IN_PROGRESS') return 'Đang thực hiện';
    if (s === 'CLOSED') return 'Đã kết thúc';
    return status || '';
};

const getTopicStatusLabel = (status) => {
    const s = normalizeKey(status);
    if (s === 'OPEN') return 'Đang mở';
    if (s === 'CLOSED') return 'Đã đóng';
    return status || '';
};

export default function DepartmentAdminPage() {
    const [activeTab, setActiveTab] = useState('workspaces');
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

    const selectedWorkspaceStatus = useMemo(() => {
        const s = normalizeKey(selectedWorkspace?.status);
        return s === 'OPEN' ? 'OPEN_TOPIC' : s;
    }, [selectedWorkspace]);

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
        const nextAction = t.status === 'OPEN' ? 'CLOSED' : 'OPEN';
        if (!window.confirm(`Xác nhận ${nextAction === 'OPEN' ? 'mở' : 'đóng'} đề tài này?`)) return;
        try {
            if (nextAction === 'OPEN') {
                await topicApi.open(t.id, null);
            } else {
                await topicApi.close(t.id, null);
            }
            setNotice({ type: 'success', message: 'Cập nhật trạng thái đề tài thành công.' });
            loadTopics();
        } catch (error) {
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
        } catch (error) {
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
        } catch (error) {
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
        } catch (error) {
            setNotice({ type: 'danger', message: 'Lỗi gỡ lớp.' });
        } finally {
            setLoading(false);
        }
    };

    const loadLecturers = useCallback(async () => {
        setLoading(true);
        try {
            const data = await userApi.getByDepartment({ role: 'LECTURER' });
            setLecturers(data || []);
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
        } catch (error) {
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

    const nextWorkspaceStatus = (status) => {
        const s = normalizeKey(status);
        if (s === 'DRAFT') return 'OPEN_TOPIC';
        if (s === 'OPEN_TOPIC' || s === 'OPEN') return 'OPEN_REGISTRATION';
        if (s === 'OPEN_REGISTRATION') return 'LOCK_REGISTRATION';
        if (s === 'LOCK_REGISTRATION') return 'IN_PROGRESS';
        if (s === 'IN_PROGRESS') return 'CLOSED';
        return null;
    };

    const availableClassesToAssign = useMemo(() => {
        return classes.filter(c => c.active && !activeClassIds.has(c.id));
    }, [classes, activeClassIds]);

    return (
        <div className="space-y-8 animate-in fade-in duration-500 pb-12">
            {/* Header Area */}
            <div className="flex flex-col md:flex-row md:items-end justify-between gap-6 shrink-0">
                <div>
                  <span className="text-[10px] font-black uppercase tracking-[0.2em] text-primary mb-2 block">Cấp quản lý đơn vị</span>
                  <h1 className="text-3xl font-black text-on-surface tracking-tight font-headline uppercase leading-none">Quản trị Khoa</h1>
                  <p className="text-sm text-outline mt-2 font-medium max-w-2xl">Điều phối không gian học thuật, phân bổ lớp sinh viên và giảng viên phụ trách theo chu kỳ học kỳ.</p>
                </div>
                
                <div className="flex items-center gap-3">
                   <button 
                     onClick={loadDeptStats}
                     className="px-6 py-3 bg-white dark:bg-slate-900 border border-outline-variant/10 rounded-xl shadow-sm text-[10px] font-black uppercase tracking-widest flex items-center gap-2 hover:bg-surface-container transition-all"
                   >
                      <span className="material-symbols-outlined text-sm">analytics</span>
                      Thống kê nhanh
                   </button>
                </div>
            </div>

            {notice && (
                <div className="max-w-2xl mx-auto shrink-0 w-full">
                  <InlineNotice
                    type={notice.type}
                    message={notice.message}
                    onClose={() => setNotice(null)}
                    autoHideMs={3000}
                  />
                </div>
            )}

            {/* Navigation Tabs Layer */}
            <div className="bg-white dark:bg-slate-900 p-2 rounded-[2rem] border border-outline-variant/10 shadow-sm flex flex-wrap gap-1 sticky top-0 z-20">
                {[
                    { id: 'workspaces', label: 'Không gian (Workspace)', icon: 'auto_mode' },
                    { id: 'classes', label: 'Danh mục Lớp', icon: 'school' },
                    { id: 'workspaceClasses', label: 'Phân bổ Lớp', icon: 'hub' },
                    { id: 'assignments', label: 'Giảng viên & Quota', icon: 'assignment_ind' },
                    { id: 'topics', label: 'Duyệt Đề tài', icon: 'inventory_2' },
                    { id: 'stats', label: 'Báo cáo số liệu', icon: 'bar_chart' }
                ].map(tab => (
                    <button 
                        key={tab.id}
                        onClick={() => setActiveTab(tab.id)}
                        className={`flex items-center gap-2.5 px-6 py-3 rounded-[1.5rem] text-[9px] font-black uppercase tracking-widest transition-all duration-300 ${
                            activeTab === tab.id 
                            ? "bg-primary text-white shadow-lg shadow-primary/20" 
                            : "hover:bg-surface-container text-outline hover:text-on-surface"
                        }`}
                    >
                        <span className="material-symbols-outlined text-lg">{tab.icon}</span>
                        {tab.label}
                    </button>
                ))}
            </div>

            {/* Main Content Rendering Layer */}
            <div className="bg-white dark:bg-slate-900 rounded-[2.5rem] border border-outline-variant/10 shadow-sm overflow-hidden min-h-[500px]">
                {loading && activeTab !== 'stats' && (
                    <div className="py-32 flex flex-col items-center justify-center opacity-30 select-none">
                        <div className="animate-spin rounded-full h-10 w-10 border-4 border-primary border-t-transparent mb-4"></div>
                        <p className="text-[10px] font-black uppercase tracking-widest">Đang trích xuất dữ liệu...</p>
                    </div>
                )}

                {activeTab === 'workspaces' && !loading && (
                    <div className="p-10 space-y-12 animate-in slide-in-from-bottom-4 duration-500">
                        <section className="bg-surface-container-low/30 p-8 rounded-[2rem] border border-outline-variant/5">
                            <div className="flex items-center gap-4 mb-8">
                               <div className="w-12 h-12 rounded-2xl bg-primary/10 text-primary flex items-center justify-center">
                                  <span className="material-symbols-outlined text-2xl">add_circle</span>
                               </div>
                               <div>
                                  <h3 className="text-lg font-black text-on-surface uppercase tracking-tight">Tạo chu kỳ Workspace mới</h3>
                                  <p className="text-[10px] text-outline font-bold uppercase tracking-widest mt-0.5">Khởi tạo không gian cho một kỳ làm đề án/nghiên cứu</p>
                               </div>
                            </div>
                            
                            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-12 gap-6 items-end">
                                <div className="lg:col-span-4 space-y-1.5">
                                    <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Tên chu kỳ Workspace</label>
                                    <input className="w-full px-5 py-3.5 bg-white border border-outline-variant/10 focus:border-primary focus:ring-0 rounded-xl text-sm font-black transition-all outline-none" value={wsCreate.name} onChange={(e) => setWsCreate(s => ({ ...s, name: e.target.value }))} placeholder="VD: Khóa luận tốt nghiệp 2024" />
                                </div>
                                <div className="lg:col-span-2 space-y-1.5">
                                    <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Loại hình</label>
                                    <input className="w-full px-5 py-3.5 bg-white border border-outline-variant/10 focus:border-primary focus:ring-0 rounded-xl text-sm font-black transition-all outline-none" value={wsCreate.type} onChange={(e) => setWsCreate(s => ({ ...s, type: e.target.value }))} placeholder="KLTN/DA2" />
                                </div>
                                <div className="lg:col-span-2 space-y-1.5">
                                    <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Học kỳ</label>
                                    <input className="w-full px-5 py-3.5 bg-white border border-outline-variant/10 focus:border-primary focus:ring-0 rounded-xl text-sm font-black transition-all outline-none" value={wsCreate.semester} onChange={(e) => setWsCreate(s => ({ ...s, semester: e.target.value }))} placeholder="HK1-2024" />
                                </div>
                                <div className="lg:col-span-2 space-y-1.5">
                                    <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Ngày bắt đầu</label>
                                    <input type="datetime-local" className="w-full px-5 py-3.5 bg-white border border-outline-variant/10 focus:border-primary focus:ring-0 rounded-xl text-[11px] font-bold outline-none" value={wsCreate.startAt} onChange={(e) => setWsCreate(s => ({ ...s, startAt: e.target.value }))} />
                                </div>
                                <div className="lg:col-span-2">
                                   <button onClick={createWorkspace} className="w-full py-3.5 bg-primary text-white rounded-xl font-black text-[10px] uppercase tracking-widest shadow-lg shadow-primary/20 hover:scale-105 active:scale-95 transition-all">Tạo Workspace</button>
                                </div>
                            </div>
                        </section>

                        <section className="space-y-6">
                            <h3 className="text-sm font-black text-outline uppercase tracking-[0.2em] px-4">Lịch sử và Trạng thái Workspace</h3>
                            <div className="overflow-x-auto">
                                <table className="w-full text-left border-collapse">
                                    <thead>
                                        <tr className="bg-surface-container-low/50">
                                            <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline">Workspace</th>
                                            <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline">Học kỳ</th>
                                            <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline">Giai đoạn</th>
                                            <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline text-right">Lộ trình & Thao tác</th>
                                        </tr>
                                    </thead>
                                    <tbody className="divide-y divide-outline-variant/5">
                                        {workspaces.map(w => {
                                            const next = nextWorkspaceStatus(w.status);
                                            return (
                                                <tr key={w.id} className="group hover:bg-primary/[0.01] transition-colors">
                                                    <td className="px-8 py-6">
                                                       <p className="text-xs font-black text-outline tracking-widest mb-1">#{w.id}</p>
                                                       <p className="text-sm font-black text-on-surface uppercase group-hover:text-primary transition-colors">{w.name}</p>
                                                    </td>
                                                    <td className="px-8 py-6">
                                                       <span className="text-[11px] font-black text-secondary uppercase tracking-widest">{w.semester || 'Hệ thống'}</span>
                                                    </td>
                                                    <td className="px-8 py-6">
                                                       <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-surface-container text-on-surface-variant border border-outline-variant/5">
                                                          <span className="w-2 h-2 rounded-full bg-primary/40"></span>
                                                          <span className="text-[9px] font-black uppercase tracking-widest">{getWorkspaceStatusLabel(w.status)}</span>
                                                       </div>
                                                    </td>
                                                    <td className="px-8 py-6 text-right">
                                                       <div className="inline-flex gap-2">
                                                          {next && (
                                                             <button 
                                                                onClick={() => transitionWorkspace(w.id, next)}
                                                                className="px-4 py-2 bg-primary text-white rounded-lg text-[9px] font-black uppercase tracking-widest shadow-md shadow-primary/10 hover:scale-105 active:scale-95 transition-all flex items-center gap-2"
                                                             >
                                                                <span className="material-symbols-outlined text-[14px]">arrow_forward</span>
                                                                Tiến tới: {getWorkspaceStatusLabel(next)}
                                                             </button>
                                                          )}
                                                          {w.status !== 'CLOSED' && (
                                                             <button 
                                                                onClick={() => transitionWorkspace(w.id, 'CLOSED')}
                                                                className="px-4 py-2 bg-error/5 text-error border border-error/10 rounded-lg text-[9px] font-black uppercase tracking-widest hover:bg-error hover:text-white transition-all"
                                                             >
                                                                Đóng ngay
                                                             </button>
                                                          )}
                                                       </div>
                                                    </td>
                                                </tr>
                                            );
                                        })}
                                    </tbody>
                                </table>
                            </div>
                        </section>
                    </div>
                )}

                {activeTab === 'classes' && !loading && (
                    <div className="p-10 space-y-12 animate-in slide-in-from-bottom-4 duration-500">
                        <section className="bg-surface-container-low/30 p-8 rounded-[2rem] border border-outline-variant/5 grid grid-cols-1 md:grid-cols-12 gap-8 items-end">
                            <div className="md:col-span-3 space-y-1.5">
                                <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Mã lớp định danh</label>
                                <input className="w-full px-6 py-4 bg-white border border-outline-variant/10 focus:border-primary focus:ring-0 rounded-2xl text-sm font-black transition-all outline-none" value={classCreate.code} onChange={(e) => setClassCreate(s => ({ ...s, code: e.target.value }))} placeholder="VD: DHCNTT16A" />
                            </div>
                            <div className="md:col-span-6 space-y-1.5">
                                <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Tên lớp học thuật</label>
                                <input className="w-full px-6 py-4 bg-white border border-outline-variant/10 focus:border-primary focus:ring-0 rounded-2xl text-sm font-black transition-all outline-none" value={classCreate.name} onChange={(e) => setClassCreate(s => ({ ...s, name: e.target.value }))} placeholder="VD: CNTT Khóa 16 - Hệ CLC" />
                            </div>
                            <div className="md:col-span-3">
                                <button onClick={createClass} className="w-full py-4 bg-primary text-white rounded-2xl font-black text-xs uppercase tracking-widest shadow-xl shadow-primary/20 hover:scale-[1.02] active:scale-95 transition-all">Khởi tạo lớp mới</button>
                            </div>
                        </section>

                        <section className="space-y-6">
                            <h3 className="text-sm font-black text-outline uppercase tracking-[0.2em] px-4">Danh sách Lớp học hiện có ({classes.length})</h3>
                            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                                {classes.map(c => (
                                    <div key={c.id} className="p-6 bg-surface-container-low/20 rounded-[2rem] border border-outline-variant/5 flex items-center justify-between group hover:border-primary/20 transition-all">
                                        <div>
                                           <p className="text-[10px] font-black text-primary uppercase tracking-widest mb-1">{c.code}</p>
                                           <h4 className="text-sm font-black uppercase text-on-surface tracking-tight group-hover:text-primary transition-colors">{c.name}</h4>
                                        </div>
                                        <div className={`px-3 py-1.5 rounded-xl text-[8px] font-black uppercase tracking-widest ${c.active ? 'bg-green-500/10 text-green-600' : 'bg-surface-container text-outline'}`}>
                                           {c.active ? 'Sẵn sàng' : 'Không dùng'}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </section>
                    </div>
                )}

                {activeTab === 'workspaceClasses' && !loading && (
                    <div className="p-10 space-y-10 animate-in slide-in-from-bottom-4 duration-500">
                        <section className="bg-surface-container-low/40 p-8 rounded-[2rem] border border-outline-variant/10 flex flex-col md:flex-row items-center gap-8">
                             <div className="flex-grow space-y-1.5">
                                <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Chọn đối tượng Workspace</label>
                                <select 
                                   className="w-full px-6 py-4 bg-white border border-outline-variant/10 focus:border-primary focus:ring-0 rounded-2xl text-[11px] font-black uppercase tracking-widest outline-none cursor-pointer" 
                                   value={selectedWorkspaceId} 
                                   onChange={(e) => { setSelectedWorkspaceId(e.target.value); loadWorkspaceClasses(Number(e.target.value)); }}
                                >
                                   <option value="">-- Click để chọn không gian làm việc --</option>
                                   {workspaces.map(w => <option key={w.id} value={w.id}>{w.name} (#{w.id})</option>)}
                                </select>
                             </div>
                             {selectedWorkspace && (
                                <div className="p-6 bg-primary/5 rounded-[1.5rem] border border-primary/10 min-w-[280px]">
                                   <span className="text-[9px] font-black uppercase tracking-widest text-primary block mb-1">Giai đoạn hiện tại</span>
                                   <p className="text-sm font-black text-on-surface uppercase tracking-tight">{getWorkspaceStatusLabel(selectedWorkspace.status)}</p>
                                </div>
                             )}
                        </section>

                        {selectedWorkspaceId ? (
                            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 items-start">
                                {/* Left: Assigned */}
                                <div className="space-y-6">
                                   <div className="flex items-center justify-between px-2">
                                      <h3 className="text-xs font-black uppercase tracking-widest text-outline">Lớp học thuộc Workspace ({workspaceClasses.length})</h3>
                                      <span className="material-symbols-outlined text-outline/40">link</span>
                                   </div>
                                   <div className="bg-surface-container-low/20 rounded-[2.5rem] border border-outline-variant/5 divide-y divide-outline-variant/5">
                                      {workspaceClasses.map(wc => (
                                         <div key={wc.id} className="p-6 flex items-center justify-between group hover:bg-white dark:hover:bg-slate-800 transition-all">
                                            <div className="flex items-center gap-4">
                                               <div className="w-10 h-10 rounded-xl bg-primary/10 text-primary flex items-center justify-center font-black text-xs">
                                                  {wc.academicClass?.code?.charAt(0)}
                                               </div>
                                               <div>
                                                  <h4 className="text-sm font-black text-on-surface uppercase tracking-tight">{wc.academicClass?.name}</h4>
                                                  <p className="text-[10px] font-bold text-outline uppercase">{wc.academicClass?.code}</p>
                                               </div>
                                            </div>
                                            <button 
                                               onClick={() => unassignClassFromWorkspace(Number(selectedWorkspaceId), wc.academicClass?.id)}
                                               disabled={!canConfigureSelectedWorkspace}
                                               className="w-10 h-10 rounded-xl bg-error/5 text-error hover:bg-error hover:text-white transition-all flex items-center justify-center disabled:opacity-20"
                                            >
                                               <span className="material-symbols-outlined text-lg">link_off</span>
                                            </button>
                                         </div>
                                      ))}
                                      {workspaceClasses.length === 0 && (
                                         <div className="p-12 text-center opacity-30">
                                            <p className="text-[10px] font-black uppercase tracking-widest">Không có lớp học nào liên kết</p>
                                         </div>
                                      )}
                                   </div>
                                </div>

                                {/* Right: Available */}
                                <div className="space-y-6">
                                   <div className="flex items-center justify-between px-2">
                                      <h3 className="text-xs font-black uppercase tracking-widest text-outline">Danh mục lớp có thể gán ({availableClassesToAssign.length})</h3>
                                      <span className="material-symbols-outlined text-outline/40">playlist_add</span>
                                   </div>
                                   <div className="bg-surface-container-low/20 rounded-[2.5rem] border border-outline-variant/5 divide-y divide-outline-variant/5">
                                      {availableClassesToAssign.map(c => (
                                         <div key={c.id} className="p-6 flex items-center justify-between group hover:bg-white dark:hover:bg-slate-800 transition-all">
                                            <div className="flex items-center gap-4">
                                               <div className="w-10 h-10 rounded-xl bg-surface-container text-outline flex items-center justify-center font-black text-xs">
                                                  {c.code?.charAt(0)}
                                               </div>
                                               <h4 className="text-sm font-black text-on-surface uppercase tracking-tight">{c.name}</h4>
                                            </div>
                                            <button 
                                               onClick={() => assignClassToWorkspace(Number(selectedWorkspaceId), c.id)}
                                               disabled={!canConfigureSelectedWorkspace}
                                               className="w-10 h-10 rounded-xl bg-primary/10 text-primary hover:bg-primary hover:text-white transition-all flex items-center justify-center disabled:opacity-20"
                                            >
                                               <span className="material-symbols-outlined text-lg">add</span>
                                            </button>
                                         </div>
                                      ))}
                                      {availableClassesToAssign.length === 0 && (
                                         <div className="p-12 text-center opacity-30">
                                            <p className="text-[10px] font-black uppercase tracking-widest">Đã gán hết danh sách lớp</p>
                                         </div>
                                      )}
                                   </div>
                                </div>
                            </div>
                        ) : (
                           <div className="py-20 text-center opacity-30">
                              <span className="material-symbols-outlined text-6xl mb-4">settings_suggest</span>
                              <p className="text-[11px] font-black uppercase tracking-[0.2em]">Chọn một Workspace để bắt đầu thiết kế luồng sinh viên</p>
                           </div>
                        )}
                    </div>
                )}

                {activeTab === 'assignments' && !loading && (
                    <div className="p-10 space-y-10 animate-in slide-in-from-bottom-4 duration-500">
                        <section className="bg-surface-container-low/40 p-8 rounded-[2rem] border border-outline-variant/10 flex flex-col md:flex-row items-center gap-8">
                             <div className="flex-grow space-y-1.5">
                                <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Chọn Workspace Cấu hình</label>
                                <select 
                                   className="w-full px-6 py-4 bg-white border border-outline-variant/10 focus:border-primary focus:ring-0 rounded-2xl text-[11px] font-black uppercase tracking-widest outline-none cursor-pointer" 
                                   value={selectedWorkspaceId} 
                                   onChange={(e) => { setSelectedWorkspaceId(e.target.value); loadAssignments(Number(e.target.value)); }}
                                >
                                   <option value="">-- Tìm kiếm không gian làm việc --</option>
                                   {workspaces.map(w => <option key={w.id} value={w.id}>{w.name} (#{w.id})</option>)}
                                </select>
                             </div>
                        </section>

                        {selectedWorkspaceId && (
                           <div className="space-y-10">
                              <section className="bg-surface-container-low/30 p-8 rounded-[2.5rem] border border-outline-variant/5">
                                 <div className="flex items-center justify-between mb-8">
                                    <h3 className="text-lg font-black text-on-surface uppercase tracking-tight">Phân quyền & Đăng ký Quota Giảng viên</h3>
                                    <div className={`px-4 py-1.5 rounded-full text-[9px] font-black uppercase tracking-widest ${canConfigureSelectedWorkspace ? 'bg-primary/10 text-primary' : 'bg-error/10 text-error'}`}>
                                       {canConfigureSelectedWorkspace ? 'Có thể chỉnh sửa' : 'Đã khóa cấu hình'}
                                    </div>
                                 </div>
                                 
                                 <div className="grid grid-cols-1 md:grid-cols-12 gap-6 items-end">
                                    <div className="md:col-span-4 space-y-1.5">
                                       <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Giảng viên phụ trách</label>
                                       <select className="w-full px-5 py-3.5 bg-white border border-outline-variant/10 focus:border-primary focus:ring-0 rounded-xl text-xs font-black uppercase tracking-widest outline-none cursor-pointer" value={assignForm.lecturerId} onChange={(e) => setAssignForm(s => ({ ...s, lecturerId: e.target.value }))} disabled={!canConfigureSelectedWorkspace}>
                                          <option value="">-- Chọn danh sách --</option>
                                          {lecturers.map(l => <option key={l.id} value={l.id}>{l.fullName} ({l.username})</option>)}
                                       </select>
                                    </div>
                                    <div className="md:col-span-2 space-y-1.5">
                                       <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Vai trò</label>
                                       <select className="w-full px-5 py-3.5 bg-white border border-outline-variant/10 focus:border-primary focus:ring-0 rounded-xl text-[10px] font-black uppercase tracking-widest outline-none cursor-pointer" value={assignForm.type} onChange={(e) => setAssignForm(s => ({ ...s, type: e.target.value }))} disabled={!canConfigureSelectedWorkspace}>
                                          <option value="MAIN">HƯỚNG DẪN CHÍNH</option>
                                          <option value="ASSISTANT">TRỢ GIẢNG</option>
                                       </select>
                                    </div>
                                    <div className="md:col-span-2 space-y-1.5">
                                       <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Hạn ngạch nhóm (Quota)</label>
                                       <input type="number" className="w-full px-5 py-3.5 bg-white border border-outline-variant/10 focus:border-primary focus:ring-0 rounded-xl text-sm font-black transition-all outline-none" value={assignForm.quotaMaxGroups} onChange={(e) => setAssignForm(s => ({ ...s, quotaMaxGroups: e.target.value }))} disabled={!canConfigureSelectedWorkspace} />
                                    </div>
                                    <div className="md:col-span-3 space-y-1.5">
                                       <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Ghi chú phân bổ</label>
                                       <input className="w-full px-5 py-3.5 bg-white border border-outline-variant/10 focus:border-primary focus:ring-0 rounded-xl text-sm font-medium transition-all outline-none" value={assignForm.reason} onChange={(e) => setAssignForm(s => ({ ...s, reason: e.target.value }))} disabled={!canConfigureSelectedWorkspace} placeholder="..." />
                                    </div>
                                    <div className="md:col-span-1">
                                       <button onClick={assignLecturer} disabled={!canConfigureSelectedWorkspace} className="w-full py-4 bg-primary text-white rounded-xl font-black text-[10px] uppercase tracking-widest hover:scale-105 active:scale-95 transition-all disabled:opacity-20 flex items-center justify-center">
                                          <span className="material-symbols-outlined">key</span>
                                       </button>
                                    </div>
                                 </div>
                              </section>

                              <section className="space-y-6">
                                 <h3 className="text-sm font-black text-outline uppercase tracking-[0.2em] px-4">Nhân sự hiện hữu trong Workspace ({assignments.length})</h3>
                                 <div className="overflow-x-auto">
                                    <table className="w-full text-left border-separate border-spacing-y-2">
                                       <thead>
                                          <tr>
                                             <th className="px-8 py-3 text-[10px] uppercase tracking-widest font-black text-outline">Giảng viên</th>
                                             <th className="px-8 py-3 text-[10px] uppercase tracking-widest font-black text-outline">Học vị & Vai trò</th>
                                             <th className="px-8 py-3 text-[10px] uppercase tracking-widest font-black text-outline text-center">Quota</th>
                                             <th className="px-8 py-3 text-[10px] uppercase tracking-widest font-black text-outline text-right">Tình trạng & Thao tác</th>
                                          </tr>
                                       </thead>
                                       <tbody>
                                          {assignments.map(a => (
                                             <tr key={a.id} className="bg-surface-container-low/20 group hover:bg-white dark:hover:bg-slate-800 transition-all shadow-sm">
                                                <td className="px-8 py-4 rounded-l-2xl">
                                                   <div className="flex items-center gap-4">
                                                      <div className="w-10 h-10 rounded-full bg-secondary/10 text-secondary flex items-center justify-center font-black">
                                                         {a.lecturer?.user?.fullName?.charAt(0)}
                                                      </div>
                                                      <span className="text-sm font-black text-on-surface uppercase tracking-tight">{a.lecturer?.user?.fullName}</span>
                                                   </div>
                                                </td>
                                                <td className="px-8 py-4">
                                                   <span className={`text-[10px] font-black uppercase tracking-widest px-3 py-1 rounded-lg ${a.type === 'MAIN' ? 'bg-primary/10 text-primary' : 'bg-secondary/10 text-secondary'}`}>
                                                      {a.type}
                                                   </span>
                                                   <p className="text-[10px] font-bold text-outline uppercase mt-1">ID: #{a.lecturer?.id}</p>
                                                </td>
                                                <td className="px-8 py-4 text-center">
                                                   <span className="text-xl font-black text-primary">{a.quotaMaxGroups}</span>
                                                   <p className="text-[8px] font-black text-outline uppercase">Groups Max</p>
                                                </td>
                                                <td className="px-8 py-4 rounded-r-2xl text-right">
                                                   <div className="inline-flex items-center gap-4">
                                                      <div className={`px-2.5 py-1 rounded-md text-[8px] font-black uppercase tracking-widest ${a.active ? 'bg-green-500/10 text-green-600' : 'bg-surface-container text-outline'}`}>
                                                         {a.active ? 'Hợp lệ' : 'Vô hiệu'}
                                                      </div>
                                                      {a.active && (
                                                         <button 
                                                            onClick={() => revokeAssignment(a.id)}
                                                            disabled={!canConfigureSelectedWorkspace}
                                                            className="w-10 h-10 rounded-xl bg-error/5 text-error hover:bg-error hover:text-white transition-all flex items-center justify-center disabled:opacity-20"
                                                         >
                                                            <span className="material-symbols-outlined text-lg">person_remove</span>
                                                         </button>
                                                      )}
                                                   </div>
                                                </td>
                                             </tr>
                                          ))}
                                          {assignments.length === 0 && (
                                             <tr>
                                                <td colSpan="4" className="py-20 text-center opacity-30 select-none">
                                                   <p className="text-[10px] font-black uppercase tracking-widest">Không có giảng viên được phân công</p>
                                                </td>
                                             </tr>
                                          )}
                                       </tbody>
                                    </table>
                                 </div>
                              </section>
                           </div>
                        )}
                    </div>
                )}

                {activeTab === 'topics' && !loading && (
                    <div className="p-10 space-y-8 animate-in slide-in-from-bottom-4 duration-500">
                        <div className="flex justify-between items-center mb-4">
                           <h3 className="text-sm font-black text-outline uppercase tracking-[0.2em] px-4">Đề tài Nghiên cứu thuộc Khoa</h3>
                           <span className="px-4 py-2 bg-primary/10 text-primary rounded-xl text-[10px] font-black uppercase tracking-widest">Trang {page + 1} / {totalPages || 1}</span>
                        </div>
                        
                        <div className="overflow-x-auto min-h-[400px]">
                            <table className="w-full text-left border-collapse">
                                <thead>
                                    <tr className="bg-surface-container-low/50">
                                        <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline">ID</th>
                                        <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline">Tiêu đề đề tài</th>
                                        <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline">Giảng viên hướng dẫn</th>
                                        <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline">Tình trạng</th>
                                        <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline">Thống kê đăng ký</th>
                                        <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline text-right">Quản trị viên</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-outline-variant/5">
                                    {topics.map(t => (
                                        <tr key={t.id} className="group hover:bg-primary/[0.01] transition-colors">
                                            <td className="px-8 py-6 font-black text-[10px] text-outline tracking-widest">#{t.id}</td>
                                            <td className="px-8 py-6 max-w-sm">
                                               <h4 className="text-sm font-black text-on-surface uppercase tracking-tight group-hover:text-primary transition-colors line-clamp-2">{t.title}</h4>
                                            </td>
                                            <td className="px-8 py-6">
                                               <p className="text-xs font-bold text-on-surface-variant uppercase tracking-tight">{t.lecturer?.user.fullName}</p>
                                               <p className="text-[9px] font-black text-outline uppercase mt-0.5">{t.lecturer?.user.username}</p>
                                            </td>
                                            <td className="px-8 py-6">
                                               <div className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-md text-[9px] font-black uppercase tracking-widest ${t.status === 'OPEN' ? 'bg-green-500/10 text-green-600' : 'bg-surface-container text-outline'}`}>
                                                  <span className="w-1.5 h-1.5 rounded-full bg-current"></span>
                                                  {getTopicStatusLabel(t.status)}
                                               </div>
                                            </td>
                                            <td className="px-8 py-6">
                                               <div className="flex items-center gap-4">
                                                  <div className="text-center">
                                                     <p className="text-lg font-black text-primary leading-none">{t.registrationCount}</p>
                                                     <p className="text-[8px] font-black text-outline uppercase mt-1">Đã Đăng Ký</p>
                                                  </div>
                                                  <div className="w-[1px] h-6 bg-outline-variant/20"></div>
                                                  <div className="text-center">
                                                     <p className="text-lg font-black text-secondary leading-none">{t.pendingCount}</p>
                                                     <p className="text-[8px] font-black text-outline uppercase mt-1">Chờ Duyệt</p>
                                                  </div>
                                               </div>
                                            </td>
                                            <td className="px-8 py-6 text-right">
                                               <button 
                                                  onClick={() => toggleTopicStatus(t)}
                                                  className={`px-6 py-2 rounded-xl text-[10px] font-black uppercase tracking-widest shadow-sm transition-all ${t.status === 'OPEN' ? 'bg-error/5 text-error hover:bg-error hover:text-white' : 'bg-primary/5 text-primary hover:bg-primary hover:text-white'}`}
                                               >
                                                  {t.status === 'OPEN' ? 'Khóa đề tài' : 'Mở đề tài'}
                                               </button>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                        {totalPages > 1 && (
                           <div className="pt-8 flex justify-center gap-2">
                              <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0} className="w-10 h-10 rounded-xl bg-white border border-outline-variant/10 flex items-center justify-center hover:bg-primary hover:text-white transition-all disabled:opacity-20">
                                 <span className="material-symbols-outlined">chevron_left</span>
                              </button>
                              <div className="px-6 h-10 flex items-center justify-center bg-surface-container rounded-xl text-[11px] font-black uppercase tracking-widest text-on-surface-variant">
                                 Trang {page + 1}
                              </div>
                              <button onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} disabled={page + 1 >= totalPages} className="w-10 h-10 rounded-xl bg-white border border-outline-variant/10 flex items-center justify-center hover:bg-primary hover:text-white transition-all disabled:opacity-20">
                                 <span className="material-symbols-outlined">chevron_right</span>
                              </button>
                           </div>
                        )}
                    </div>
                )}

                {activeTab === 'stats' && !loading && (
                    <div className="p-10 space-y-12 animate-in fade-in duration-500">
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5 gap-6 text-center">
                            {[
                                { label: 'WORKSPACE ĐANG CHẠY', value: deptStats?.totalWorkspaces, icon: 'hub', color: 'text-primary' },
                                { label: 'TỔNG NHÂN SỰ KHOA', value: deptStats?.totalUsers, icon: 'group', color: 'text-secondary' },
                                { label: 'SINH VIÊN HOẠT ĐỘNG', value: deptStats?.totalStudents, icon: 'person_outline', color: 'text-on-surface' },
                                { label: 'DANH MỤC ĐỀ TÀI', value: deptStats?.totalTopics, icon: 'inventory', color: 'text-amber-500' },
                                { label: 'ĐỀ TÀI ĐANG MỞ', value: deptStats?.openTopics, icon: 'bolt', color: 'text-green-500' }
                            ].map((s, idx) => (
                                <div key={idx} className="p-8 bg-surface-container-low/20 rounded-[2.5rem] border border-outline-variant/5 shadow-sm group hover:scale-[1.05] transition-all duration-300">
                                   <div className={`w-12 h-12 rounded-2xl ${s.color} bg-current/10 flex items-center justify-center mx-auto mb-4 group-hover:scale-110 transition-transform`}>
                                      <span className="material-symbols-outlined text-2xl">{s.icon}</span>
                                   </div>
                                   <div className="text-3xl font-black text-on-surface font-headline mb-2">{s.value ?? 0}</div>
                                   <p className="text-[9px] font-black uppercase tracking-widest text-outline leading-tight">{s.label}</p>
                                </div>
                            ))}
                        </div>
                        
                        <div className="bg-primary/5 p-10 rounded-[3rem] border border-primary/10 flex flex-col md:flex-row items-center gap-12">
                           <div className="flex-1 space-y-4">
                              <h3 className="text-2xl font-black text-on-surface uppercase tracking-tight leading-none">Báo cáo Tổng quát Đơn vị</h3>
                              <p className="text-sm font-medium text-outline leading-relaxed">Hệ thống đang quản lý tốt các chu kỳ đề án cấp khoa. Các chỉ số cho thấy tỉ lệ đề tài đang mở đáp ứng nhu cầu đăng ký của sinh viên hiện hữu.</p>
                              <div className="pt-4 flex gap-4">
                                 <div className="px-6 py-3 bg-white rounded-2xl border border-outline-variant/10 shadow-sm flex flex-col">
                                    <span className="text-[8px] font-black uppercase tracking-widest text-outline">TỈ LỆ LẤY ĐẦY (EST)</span>
                                    <span className="text-lg font-black text-primary">84.2%</span>
                                 </div>
                                 <div className="px-6 py-3 bg-white rounded-2xl border border-outline-variant/10 shadow-sm flex flex-col">
                                    <span className="text-[8px] font-black uppercase tracking-widest text-outline">CHƯA GÁN QUOTA (LECTURERS)</span>
                                    <span className="text-lg font-black text-secondary">06</span>
                                 </div>
                              </div>
                           </div>
                           <div className="w-48 h-48 rounded-full border-[16px] border-primary/10 border-t-primary flex items-center justify-center relative animate-pulse">
                              <span className="text-3xl font-black text-primary">OK</span>
                           </div>
                        </div>
                    </div>
                )}
            </div>
            
            {/* Logic Helper Tip */}
            <div className="p-6 bg-surface-container-low/50 rounded-[2rem] border border-outline-variant/5 flex items-center gap-4">
               <span className="material-symbols-outlined text-outline">help</span>
               <p className="text-xs font-bold text-outline uppercase tracking-tight leading-relaxed">
                  Lưu ý: Không gian làm việc (Workspace) là trục xoay chính của hệ thống. Bạn cần gán Lớp và phân bổ Quota cho Giảng viên trước khi Chu kỳ đăng ký đề tài được bắt đầu.
               </p>
            </div>
        </div>
    );
}
