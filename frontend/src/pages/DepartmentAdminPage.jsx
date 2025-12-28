import React, { useCallback, useEffect, useMemo, useState } from 'react';
import api from '../api';
import userApi from '../api/userApi';
import { topicApi } from '../api/topicApi';

const normalizeKey = (value) => String(value ?? '').trim().toUpperCase();
const getWorkspaceStatusLabel = (status) => {
    const s = normalizeKey(status);
    if (s === 'DRAFT') return 'Nháp';
    if (s === 'OPEN' || s === 'OPEN_TOPIC') return 'Mở đề tài';
    if (s === 'OPEN_REGISTRATION') return 'Mở đăng ký';
    if (s === 'LOCK_REGISTRATION') return 'Khóa đăng ký';
    if (s === 'IN_PROGRESS') return 'Đang thực hiện';
    if (s === 'CLOSED') return 'Đã đóng';
    return status || '';
};

const getTopicStatusLabel = (status) => {
    const s = normalizeKey(status);
    if (s === 'OPEN') return 'Mở';
    if (s === 'CLOSED') return 'Đóng';
    return status || '';
};

export default function DepartmentAdminPage() {
    const [activeTab, setActiveTab] = useState('workspaces');
    const [loading, setLoading] = useState(false);

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
        if (!window.confirm(`Đổi trạng thái đề tài sang ${getTopicStatusLabel(nextAction)}?`)) return;
        try {
            if (nextAction === 'OPEN') {
                await topicApi.open(t.id, null);
            } else {
                await topicApi.close(t.id, null);
            }
            loadTopics();
        } catch (error) {
            console.error('Failed to update topic', error);
            alert('Có lỗi xảy ra');
        }
    };

    const loadWorkspaces = useCallback(async () => {
        setLoading(true);
        try {
            const res = await api.get('/workspaces');
            setWorkspaces(res.data || []);
        } catch (error) {
            console.error('Failed to load workspaces', error);
            alert(error?.message || 'Có lỗi xảy ra');
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
            alert(error?.message || 'Có lỗi xảy ra');
        } finally {
            setLoading(false);
        }
    }, []);

    const createWorkspace = async () => {
        const name = wsCreate.name.trim();
        if (!name) {
            alert('Vui lòng nhập tên không gian làm việc');
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
            setWsCreate({ name: '', type: '', semester: '', startAt: '', endAt: '' });
            await loadWorkspaces();
        } catch (error) {
            console.error('Failed to create workspace', error);
            alert(error?.message || 'Có lỗi xảy ra');
        } finally {
            setLoading(false);
        }
    };

    const transitionWorkspace = async (workspaceId, to) => {
        if (!window.confirm(`Chuyển không gian làm việc sang trạng thái ${getWorkspaceStatusLabel(to)}?`)) return;
        setLoading(true);
        try {
            await api.post(`/workspaces/${workspaceId}/transition`, null, { params: { to } });
            await loadWorkspaces();
        } catch (error) {
            console.error('Failed to transition workspace', error);
            alert(error?.message || 'Có lỗi xảy ra');
        } finally {
            setLoading(false);
        }
    };

    const createClass = async () => {
        const code = classCreate.code.trim();
        const name = classCreate.name.trim();
        if (!code || !name) {
            alert('Vui lòng nhập mã lớp và tên lớp');
            return;
        }
        setLoading(true);
        try {
            await api.post('/classes', { code, name });
            setClassCreate({ code: '', name: '' });
            await loadClasses();
        } catch (error) {
            console.error('Failed to create class', error);
            alert(error?.message || 'Có lỗi xảy ra');
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
            alert(error?.message || 'Có lỗi xảy ra');
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
            console.error('Failed to assign class', error);
            alert(error?.message || 'Có lỗi xảy ra');
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
            console.error('Failed to unassign class', error);
            alert(error?.message || 'Có lỗi xảy ra');
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
            alert(error?.message || 'Có lỗi xảy ra');
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
            alert(error?.message || 'Có lỗi xảy ra');
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
            alert(error?.message || 'Có lỗi xảy ra');
        } finally {
            setLoading(false);
        }
    }, []);

    const assignLecturer = async () => {
        const workspaceId = Number(selectedWorkspaceId);
        const lecturerId = Number(assignForm.lecturerId);
        if (!workspaceId || !lecturerId) {
            alert('Vui lòng chọn không gian làm việc và giảng viên');
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
            setAssignForm({ lecturerId: '', type: 'MAIN', quotaMaxGroups: 0, reason: '' });
            await loadAssignments(workspaceId);
        } catch (error) {
            console.error('Failed to assign lecturer', error);
            alert(error?.message || 'Có lỗi xảy ra');
        } finally {
            setLoading(false);
        }
    };

    const revokeAssignment = async (assignmentId) => {
        const reason = window.prompt('Lý do thu hồi (tuỳ chọn):') || '';
        setLoading(true);
        try {
            await api.post('/assignments/revoke', { assignmentId, reason: reason.trim() || null });
            await loadAssignments(Number(selectedWorkspaceId));
        } catch (error) {
            console.error('Failed to revoke assignment', error);
            alert(error?.message || 'Có lỗi xảy ra');
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
        <div className="container-fluid">
            <h2 className="mb-4">Quản trị khoa</h2>

            <ul className="nav nav-tabs mb-4">
                <li className="nav-item">
                    <button
                        className={`nav-link ${activeTab === 'workspaces' ? 'active' : ''}`}
                        onClick={() => setActiveTab('workspaces')}
                    >
                        Quản lý không gian làm việc
                    </button>
                </li>
                <li className="nav-item">
                    <button
                        className={`nav-link ${activeTab === 'classes' ? 'active' : ''}`}
                        onClick={() => setActiveTab('classes')}
                    >
                        Quản lý Lớp
                    </button>
                </li>
                <li className="nav-item">
                    <button
                        className={`nav-link ${activeTab === 'workspaceClasses' ? 'active' : ''}`}
                        onClick={() => setActiveTab('workspaceClasses')}
                    >
                        Gán Lớp
                    </button>
                </li>
                <li className="nav-item">
                    <button
                        className={`nav-link ${activeTab === 'assignments' ? 'active' : ''}`}
                        onClick={() => setActiveTab('assignments')}
                    >
                        Giảng viên
                    </button>
                </li>
                <li className="nav-item">
                    <button
                        className={`nav-link ${activeTab === 'topics' ? 'active' : ''}`}
                        onClick={() => { setActiveTab('topics'); setPage(0); }}
                    >
                        Đề tài (Khoa)
                    </button>
                </li>
                <li className="nav-item">
                    <button
                        className={`nav-link ${activeTab === 'stats' ? 'active' : ''}`}
                        onClick={() => setActiveTab('stats')}
                    >
                        Thống kê Khoa
                    </button>
                </li>
            </ul>

            {loading && <div className="spinner-border text-primary" role="status"><span className="visually-hidden">Đang tải...</span></div>}

            {!loading && activeTab === 'topics' && (
                <div>
                    <div className="table-responsive">
                        <table className="table table-striped table-hover">
                            <thead className="table-dark">
                                <tr>
                                    <th>ID</th>
                                    <th>Tiêu đề</th>
                                    <th>GVHD</th>
                                    <th>Trạng thái</th>
                                    <th>Đăng ký</th>
                                    <th>Hành động</th>
                                </tr>
                            </thead>
                            <tbody>
                                {topics.map(t => (
                                    <tr key={t.id}>
                                        <td>{t.id}</td>
                                        <td>{t.title}</td>
                                        <td>{t.lecturer ? t.lecturer.user.fullName : 'Không có'}</td>
                                        <td>
                                            <span className={`badge ${t.status === 'OPEN' ? 'bg-success' : 'bg-secondary'}`}>
                                                {getTopicStatusLabel(t.status)}
                                            </span>
                                        </td>
                                        <td>{t.registrationCount} (Chờ duyệt: {t.pendingCount})</td>
                                        <td>
                                            <button
                                                className="btn btn-sm btn-info me-2"
                                                onClick={() => toggleTopicStatus(t)}
                                            >
                                                {t.status === 'OPEN' ? 'Đóng' : 'Mở'}
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                    {totalPages > 1 && (
                        <nav>
                            <ul className="pagination justify-content-center">
                                <li className={`page-item ${page === 0 ? 'disabled' : ''}`}>
                                    <button className="page-link" onClick={() => setPage(p => Math.max(0, p - 1))}>Trước</button>
                                </li>
                                <li className="page-item disabled">
                                    <span className="page-link">Trang {page + 1}/{totalPages}</span>
                                </li>
                                <li className={`page-item ${page + 1 >= totalPages ? 'disabled' : ''}`}>
                                    <button className="page-link" onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}>Tiếp</button>
                                </li>
                            </ul>
                        </nav>
                    )}
                </div>
            )}

            {!loading && activeTab === 'workspaces' && (
                <div>
                    <div className="card mb-3">
                        <div className="card-body">
                            <div className="row g-2 align-items-end">
                                <div className="col-md-4">
                                    <label className="form-label">Tên không gian làm việc</label>
                                    <input className="form-control" value={wsCreate.name} onChange={(e) => setWsCreate(s => ({ ...s, name: e.target.value }))} />
                                </div>
                                <div className="col-md-2">
                                    <label className="form-label">Loại</label>
                                    <input className="form-control" value={wsCreate.type} onChange={(e) => setWsCreate(s => ({ ...s, type: e.target.value }))} />
                                </div>
                                <div className="col-md-2">
                                    <label className="form-label">Học kỳ</label>
                                    <input className="form-control" value={wsCreate.semester} onChange={(e) => setWsCreate(s => ({ ...s, semester: e.target.value }))} />
                                </div>
                                <div className="col-md-2">
                                    <label className="form-label">Bắt đầu</label>
                                    <input type="datetime-local" className="form-control" value={wsCreate.startAt} onChange={(e) => setWsCreate(s => ({ ...s, startAt: e.target.value }))} />
                                </div>
                                <div className="col-md-2">
                                    <label className="form-label">Kết thúc</label>
                                    <input type="datetime-local" className="form-control" value={wsCreate.endAt} onChange={(e) => setWsCreate(s => ({ ...s, endAt: e.target.value }))} />
                                </div>
                                <div className="col-12">
                                    <button className="btn btn-primary" onClick={createWorkspace}>Tạo không gian làm việc</button>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="table-responsive">
                        <table className="table table-striped table-hover">
                            <thead className="table-dark">
                                <tr>
                                    <th>ID</th>
                                    <th>Tên</th>
                                    <th>Học kỳ</th>
                                    <th>Trạng thái</th>
                                    <th>Hành động</th>
                                </tr>
                            </thead>
                            <tbody>
                                {workspaces.map(w => {
                                    const next = nextWorkspaceStatus(w.status);
                                    return (
                                        <tr key={w.id}>
                                            <td>{w.id}</td>
                                            <td>{w.name}</td>
                                            <td>{w.semester || ''}</td>
                                            <td><span className="badge bg-secondary">{getWorkspaceStatusLabel(w.status)}</span></td>
                                            <td>
                                                {next && (
                                                    <button className="btn btn-sm btn-primary me-2" onClick={() => transitionWorkspace(w.id, next)}>
                                                        Chuyển: {getWorkspaceStatusLabel(next)}
                                                    </button>
                                                )}
                                                {w.status !== 'CLOSED' && (
                                                    <button className="btn btn-sm btn-outline-danger" onClick={() => transitionWorkspace(w.id, 'CLOSED')}>
                                                        Đóng
                                                    </button>
                                                )}
                                            </td>
                                        </tr>
                                    );
                                })}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}

            {!loading && activeTab === 'classes' && (
                <div>
                    <div className="card mb-3">
                        <div className="card-body">
                            <div className="row g-2 align-items-end">
                                <div className="col-md-3">
                                    <label className="form-label">Mã lớp</label>
                                    <input className="form-control" value={classCreate.code} onChange={(e) => setClassCreate(s => ({ ...s, code: e.target.value }))} />
                                </div>
                                <div className="col-md-6">
                                    <label className="form-label">Tên lớp</label>
                                    <input className="form-control" value={classCreate.name} onChange={(e) => setClassCreate(s => ({ ...s, name: e.target.value }))} />
                                </div>
                                <div className="col-md-3">
                                    <button className="btn btn-primary w-100" onClick={createClass}>Tạo lớp</button>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="table-responsive">
                        <table className="table table-striped table-hover">
                            <thead className="table-dark">
                                <tr>
                                    <th>ID</th>
                                    <th>Mã lớp</th>
                                    <th>Tên lớp</th>
                                    <th>Trạng thái</th>
                                </tr>
                            </thead>
                            <tbody>
                                {classes.map(c => (
                                    <tr key={c.id}>
                                        <td>{c.id}</td>
                                        <td>{c.code}</td>
                                        <td>{c.name}</td>
                                        <td>
                                            <span className={`badge ${c.active ? 'bg-success' : 'bg-secondary'}`}>
                                                {c.active ? 'Hoạt động' : 'Không hoạt động'}
                                            </span>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}

            {!loading && activeTab === 'workspaceClasses' && (
                <div>
                    <div className="row g-3 mb-3">
                        <div className="col-md-6">
                            <label className="form-label">Chọn không gian làm việc</label>
                            <select className="form-select" value={selectedWorkspaceId} onChange={(e) => { setSelectedWorkspaceId(e.target.value); loadWorkspaceClasses(Number(e.target.value)); }}>
                                <option value="">-- Chọn --</option>
                                {workspaces.map(w => (
                                    <option key={w.id} value={w.id}>{w.name} (#{w.id})</option>
                                ))}
                            </select>
                        </div>
                        <div className="col-md-6 d-flex align-items-end">
                            <div className="text-muted small">
                                {selectedWorkspace ? `Trạng thái không gian làm việc: ${getWorkspaceStatusLabel(selectedWorkspace.status)}` : ''}
                            </div>
                        </div>
                    </div>

                    {selectedWorkspaceId && (
                        <div className="row g-3">
                            {!canConfigureSelectedWorkspace && (
                                <div className="col-12">
                                    <div className="alert alert-warning mb-0">
                                        Không thể gán/gỡ lớp khi không gian làm việc ở trạng thái {getWorkspaceStatusLabel(selectedWorkspaceStatus)}.
                                    </div>
                                </div>
                            )}
                            <div className="col-lg-6">
                                <div className="card">
                                    <div className="card-header">Lớp đã gán</div>
                                    <div className="card-body p-0">
                                        <div className="table-responsive">
                                            <table className="table mb-0">
                                                <thead>
                                                    <tr>
                                                        <th>Lớp</th>
                                                        <th>Trạng thái</th>
                                                        <th></th>
                                                    </tr>
                                                </thead>
                                                <tbody>
                                                    {workspaceClasses.map(wc => (
                                                        <tr key={wc.id}>
                                                            <td>{wc.academicClass?.code} - {wc.academicClass?.name}</td>
                                                            <td>
                                                                <span className={`badge ${wc.active ? 'bg-success' : 'bg-secondary'}`}>{wc.active ? 'Hoạt động' : 'Không hoạt động'}</span>
                                                            </td>
                                                            <td className="text-end">
                                                                {wc.active && (
                                                                    <button className="btn btn-sm btn-outline-danger" onClick={() => unassignClassFromWorkspace(Number(selectedWorkspaceId), wc.academicClass?.id)} disabled={!canConfigureSelectedWorkspace}>
                                                                        Gỡ
                                                                    </button>
                                                                )}
                                                            </td>
                                                        </tr>
                                                    ))}
                                                    {workspaceClasses.length === 0 && (
                                                        <tr><td colSpan="3" className="text-center text-muted p-3">Chưa có lớp</td></tr>
                                                    )}
                                                </tbody>
                                            </table>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <div className="col-lg-6">
                                <div className="card">
                                    <div className="card-header">Lớp chưa gán</div>
                                    <div className="card-body p-0">
                                        <div className="table-responsive">
                                            <table className="table mb-0">
                                                <thead>
                                                    <tr>
                                                        <th>Lớp</th>
                                                        <th></th>
                                                    </tr>
                                                </thead>
                                                <tbody>
                                                    {availableClassesToAssign.map(c => (
                                                        <tr key={c.id}>
                                                            <td>{c.code} - {c.name}</td>
                                                            <td className="text-end">
                                                                <button className="btn btn-sm btn-primary" onClick={() => assignClassToWorkspace(Number(selectedWorkspaceId), c.id)} disabled={!canConfigureSelectedWorkspace}>
                                                                    Gán
                                                                </button>
                                                            </td>
                                                        </tr>
                                                    ))}
                                                    {availableClassesToAssign.length === 0 && (
                                                        <tr><td colSpan="2" className="text-center text-muted p-3">Không còn lớp để gán</td></tr>
                                                    )}
                                                </tbody>
                                            </table>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    )}
                </div>
            )}

            {!loading && activeTab === 'assignments' && (
                <div>
                    <div className="row g-3 mb-3">
                        <div className="col-md-6">
                            <label className="form-label">Chọn không gian làm việc</label>
                            <select className="form-select" value={selectedWorkspaceId} onChange={(e) => { setSelectedWorkspaceId(e.target.value); loadAssignments(Number(e.target.value)); }}>
                                <option value="">-- Chọn --</option>
                                {workspaces.map(w => (
                                    <option key={w.id} value={w.id}>{w.name} (#{w.id})</option>
                                ))}
                            </select>
                        </div>
                        <div className="col-md-6 d-flex align-items-end">
                            <div className="text-muted small">
                                {selectedWorkspace ? `Trạng thái không gian làm việc: ${getWorkspaceStatusLabel(selectedWorkspace.status)}` : ''}
                            </div>
                        </div>
                    </div>

                    {selectedWorkspaceId && (
                        <>
                            {!canConfigureSelectedWorkspace && (
                                <div className="alert alert-warning">
                                    Không thể cấu hình quota giảng viên khi không gian làm việc ở trạng thái {getWorkspaceStatusLabel(selectedWorkspaceStatus)}.
                                </div>
                            )}
                            <div className="card mb-3">
                                <div className="card-body">
                                    <div className="row g-2 align-items-end">
                                        <div className="col-md-4">
                                            <label className="form-label">Giảng viên</label>
                                            <select className="form-select" value={assignForm.lecturerId} onChange={(e) => setAssignForm(s => ({ ...s, lecturerId: e.target.value }))} disabled={!canConfigureSelectedWorkspace}>
                                                <option value="">-- Chọn --</option>
                                                {lecturers.map(l => (
                                                    <option key={l.id} value={l.id}>{l.fullName} ({l.username})</option>
                                                ))}
                                            </select>
                                        </div>
                                        <div className="col-md-2">
                                            <label className="form-label">Loại</label>
                                            <select className="form-select" value={assignForm.type} onChange={(e) => setAssignForm(s => ({ ...s, type: e.target.value }))} disabled={!canConfigureSelectedWorkspace}>
                                                <option value="MAIN">Chính</option>
                                                <option value="ASSISTANT">Phụ</option>
                                            </select>
                                        </div>
                                        <div className="col-md-2">
                                            <label className="form-label">nhóm</label>
                                            <input type="number" className="form-control" value={assignForm.quotaMaxGroups} onChange={(e) => setAssignForm(s => ({ ...s, quotaMaxGroups: e.target.value }))} disabled={!canConfigureSelectedWorkspace} />
                                        </div>
                                        <div className="col-md-3">
                                            <label className="form-label">Ghi chú</label>
                                            <input className="form-control" value={assignForm.reason} onChange={(e) => setAssignForm(s => ({ ...s, reason: e.target.value }))} disabled={!canConfigureSelectedWorkspace} />
                                        </div>
                                        <div className="col-md-1">
                                            <button className="btn btn-primary w-100" onClick={assignLecturer} disabled={!canConfigureSelectedWorkspace}>Gán</button>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <div className="table-responsive">
                                <table className="table table-striped table-hover">
                                    <thead className="table-dark">
                                        <tr>
                                            <th>ID</th>
                                            <th>Giảng viên</th>
                                            <th>Loại</th>
                                            <th>Nhóm</th>
                                            <th>Trạng thái</th>
                                            <th></th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {assignments.map(a => (
                                            <tr key={a.id}>
                                                <td>{a.id}</td>
                                                <td>{a.lecturer?.user?.fullName || ''}</td>
                                                <td><span className="badge bg-secondary">{a.type}</span></td>
                                                <td>{a.quotaMaxGroups}</td>
                                                <td>
                                                    <span className={`badge ${a.active ? 'bg-success' : 'bg-secondary'}`}>{a.active ? 'Hoạt động' : 'Không hoạt động'}</span>
                                                </td>
                                                <td className="text-end">
                                                    {a.active && (
                                                        <button className="btn btn-sm btn-outline-danger" onClick={() => revokeAssignment(a.id)} disabled={!canConfigureSelectedWorkspace}>
                                                            Thu hồi
                                                        </button>
                                                    )}
                                                </td>
                                            </tr>
                                        ))}
                                        {assignments.length === 0 && (
                                            <tr><td colSpan="6" className="text-center text-muted p-3">Chưa có phân công</td></tr>
                                        )}
                                    </tbody>
                                </table>
                            </div>
                        </>
                    )}
                </div>
            )}

            {!loading && activeTab === 'stats' && (
                <div className="row g-3">
                    <div className="col-md-4">
                        <div className="card">
                            <div className="card-body">
                                <div className="text-muted small">Không gian làm việc</div>
                                <div className="fs-4">{deptStats?.totalWorkspaces ?? 0}</div>
                            </div>
                        </div>
                    </div>
                    <div className="col-md-4">
                        <div className="card">
                            <div className="card-body">
                                <div className="text-muted small">Người dùng</div>
                                <div className="fs-4">{deptStats?.totalUsers ?? 0}</div>
                            </div>
                        </div>
                    </div>
                    <div className="col-md-4">
                        <div className="card">
                            <div className="card-body">
                                <div className="text-muted small">Sinh viên</div>
                                <div className="fs-4">{deptStats?.totalStudents ?? 0}</div>
                            </div>
                        </div>
                    </div>
                    <div className="col-md-6">
                        <div className="card">
                            <div className="card-body">
                                <div className="text-muted small">Tổng đề tài</div>
                                <div className="fs-4">{deptStats?.totalTopics ?? 0}</div>
                            </div>
                        </div>
                    </div>
                    <div className="col-md-6">
                        <div className="card">
                            <div className="card-body">
                                <div className="text-muted small">Đề tài đang mở</div>
                                <div className="fs-4">{deptStats?.openTopics ?? 0}</div>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
