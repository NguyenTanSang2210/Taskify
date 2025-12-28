import React, { useCallback, useEffect, useMemo, useState } from 'react';
import api from '../api';
import userApi from '../api/userApi';

const normalizeKey = (value) => String(value ?? '').trim().toUpperCase();
const getRoleLabel = (roleName) => {
    const key = normalizeKey(roleName);
    if (key === 'ADMIN') return 'Quản trị';
    if (key === 'DEPARTMENT_ADMIN') return 'Quản trị khoa';
    if (key === 'LECTURER') return 'Giảng viên';
    if (key === 'STUDENT') return 'Sinh viên';
    return roleName || '';
};

export default function AdminPage() {
    const [loading, setLoading] = useState(false);
    const [activeTab, setActiveTab] = useState('createUser');

    const [users, setUsers] = useState([]);
    const [roles, setRoles] = useState([]);
    const [departments, setDepartments] = useState([]);
    const [departmentCreate, setDepartmentCreate] = useState({ code: '', name: '', active: true });
    const [selectedDepartmentId, setSelectedDepartmentId] = useState('');
    const [selectedUserRole, setSelectedUserRole] = useState('');
    const [academicClasses, setAcademicClasses] = useState([]);

    const [userCreate, setUserCreate] = useState({
        username: '',
        password: '',
        fullName: '',
        email: '',
        phone: '',
        roleId: '',
        departmentId: '',
        active: true,
        studentCode: '',
        className: '',
        academicClassId: '',
        degree: '',
        speciality: ''
    });

    const createRoleName = useMemo(() => {
        const id = Number(userCreate.roleId);
        const role = roles.find(r => r.id === id);
        return role?.name || '';
    }, [roles, userCreate.roleId]);

    const createRoleKey = useMemo(() => normalizeKey(createRoleName), [createRoleName]);

    const loadDepartments = useCallback(async () => {
        try {
            const res = await api.get('/departments');
            const items = res.data || [];
            setDepartments(items);
            setSelectedDepartmentId((current) => {
                if (current) return current;
                const first = items.find(d => d.active) || items[0];
                return first?.id != null ? String(first.id) : '';
            });
        } catch (error) {
            console.error('Failed to load departments', error);
            alert(error?.message || 'Có lỗi xảy ra');
        }
    }, []);

    const createDepartment = async (e) => {
        e?.preventDefault?.();
        const code = departmentCreate.code.trim();
        const name = departmentCreate.name.trim();
        if (!code || !name) {
            alert('Vui lòng nhập mã khoa và tên khoa');
            return;
        }
        setLoading(true);
        try {
            await api.post('/departments', { code, name, active: !!departmentCreate.active });
            alert('Tạo khoa thành công');
            setDepartmentCreate({ code: '', name: '', active: true });
            await loadDepartments();
        } catch (error) {
            console.error('Failed to create department', error);
            alert(error?.message || 'Có lỗi xảy ra');
        } finally {
            setLoading(false);
        }
    };

    const loadRoles = useCallback(async () => {
        try {
            const data = await userApi.getRoles();
            setRoles(data || []);
        } catch (error) {
            console.error('Failed to load roles', error);
        }
    }, []);

    const loadAcademicClasses = useCallback(async (departmentId) => {
        if (!departmentId) {
            setAcademicClasses([]);
            return;
        }
        try {
            const res = await api.get('/classes', { params: { departmentId } });
            setAcademicClasses(res.data || []);
        } catch (error) {
            console.error('Failed to load academic classes', error);
            setAcademicClasses([]);
        }
    }, []);

    const loadUsers = useCallback(async () => {
        setLoading(true);
        try {
            if (selectedUserRole === 'ADMIN') {
                const data = await userApi.getAll();
                const items = (data || []).filter(u => (u.role?.name || u.role) === 'ADMIN');
                setUsers(items);
                return;
            }

            const departmentId = selectedDepartmentId ? Number(selectedDepartmentId) : null;
            if (!departmentId) {
                setUsers([]);
                return;
            }
            const params = { departmentId };
            if (selectedUserRole) params.role = selectedUserRole;
            const data = await userApi.getByDepartment(params);
            setUsers(data || []);
        } catch (error) {
            console.error('Failed to load users', error);
            alert(error?.message || 'Có lỗi xảy ra');
        } finally {
            setLoading(false);
        }
    }, [selectedDepartmentId, selectedUserRole]);

    const toggleUserStatus = async (u) => {
        if (!window.confirm(`Bạn có chắc muốn ${u.active ? 'khóa' : 'mở khóa'} tài khoản ${u.username}?`)) return;
        try {
            await userApi.updateStatus(u.id, !u.active);
            loadUsers();
        } catch (error) {
            console.error('Failed to update user status', error);
            alert('Có lỗi xảy ra');
        }
    };

    const changeUserRole = async (u, newRoleId) => {
        if (!window.confirm(`Bạn có chắc muốn đổi vai trò của ${u.username}?`)) return;
        try {
            await userApi.updateRole(u.id, newRoleId);
            loadUsers();
        } catch (error) {
            console.error('Failed to update user role', error);
            alert('Có lỗi xảy ra');
        }
    };

    const createUser = async (e) => {
        e?.preventDefault?.();
        const username = userCreate.username.trim();
        const password = userCreate.password.trim();
        const fullName = userCreate.fullName.trim();
        const email = userCreate.email.trim();
        const phone = userCreate.phone.trim();
        const roleId = userCreate.roleId ? Number(userCreate.roleId) : null;

        if (!username || !password || !fullName) {
            alert('Vui lòng nhập username, mật khẩu và họ tên');
            return;
        }
        if (!roleId) {
            alert('Vui lòng chọn vai trò');
            return;
        }

        const role = roles.find(r => r.id === roleId);
        const roleName = normalizeKey(role?.name);
        const departmentIdRaw = userCreate.departmentId || selectedDepartmentId;

        const payload = {
            username,
            password,
            fullName,
            roleId,
            active: !!userCreate.active
        };

        if (email) payload.email = email;
        if (phone) payload.phone = phone;

        if (roleName && roleName !== 'ADMIN') {
            if (!departmentIdRaw) {
                alert('Vui lòng chọn khoa');
                return;
            }
            payload.departmentId = Number(departmentIdRaw);
        }

        if (roleName === 'STUDENT') {
            const studentCode = userCreate.studentCode.trim();
            const className = userCreate.className.trim();
            const academicClassId = userCreate.academicClassId ? Number(userCreate.academicClassId) : null;

            if (!studentCode) {
                alert('Vui lòng nhập mã sinh viên');
                return;
            }
            if (!className && !academicClassId) {
                alert('Vui lòng nhập tên lớp hoặc chọn lớp');
                return;
            }
            payload.studentCode = studentCode;
            if (className) payload.className = className;
            if (academicClassId) payload.academicClassId = academicClassId;
        }

        if (roleName === 'LECTURER') {
            const degree = userCreate.degree.trim();
            const speciality = userCreate.speciality.trim();
            if (degree) payload.degree = degree;
            if (speciality) payload.speciality = speciality;
        }

        setLoading(true);
        try {
            await userApi.create(payload);
            alert('Tạo người dùng thành công');
            setUserCreate(s => ({
                ...s,
                username: '',
                password: '',
                fullName: '',
                email: '',
                phone: '',
                active: true,
                studentCode: '',
                className: '',
                academicClassId: '',
                degree: '',
                speciality: ''
            }));
            if (roleName === 'ADMIN') {
                setSelectedDepartmentId('');
                setSelectedUserRole('ADMIN');
            } else if (payload.departmentId != null) {
                setSelectedDepartmentId(String(payload.departmentId));
                setSelectedUserRole('');
            }
            setActiveTab('userList');
        } catch (error) {
            console.error('Failed to create user', error);
            alert(error?.message || 'Có lỗi xảy ra');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadDepartments();
        loadRoles();
    }, [loadDepartments, loadRoles]);

    useEffect(() => {
        setUserCreate((s) => {
            if (s.roleId) return s;
            const first = roles.find(r => r.name !== 'ADMIN') || roles[0];
            if (!first?.id) return s;
            return { ...s, roleId: String(first.id) };
        });
    }, [roles]);

    useEffect(() => {
        setUserCreate((s) => {
            if (s.departmentId) return s;
            if (!selectedDepartmentId) return s;
            if (createRoleKey === 'ADMIN') return s;
            return { ...s, departmentId: selectedDepartmentId };
        });
    }, [createRoleKey, selectedDepartmentId]);

    useEffect(() => {
        const departmentId = selectedDepartmentId ? Number(selectedDepartmentId) : null;
        if (!departmentId) {
            setAcademicClasses([]);
            return;
        }
        loadAcademicClasses(departmentId);
    }, [loadAcademicClasses, selectedDepartmentId]);

    useEffect(() => {
        if (activeTab !== 'userList') return;
        loadUsers();
    }, [activeTab, loadUsers]);

    useEffect(() => {
        if (activeTab !== 'departments') return;
        loadDepartments();
    }, [activeTab, loadDepartments]);

    return (
        <div className="container-fluid">
            <h2 className="mb-4">Quản trị hệ thống</h2>

            <ul className="nav nav-tabs mb-4">
                <li className="nav-item">
                    <button
                        type="button"
                        className={`nav-link ${activeTab === 'createUser' ? 'active' : ''}`}
                        onClick={() => setActiveTab('createUser')}
                    >
                        Tạo tài khoản
                    </button>
                </li>
                <li className="nav-item">
                    <button
                        type="button"
                        className={`nav-link ${activeTab === 'userList' ? 'active' : ''}`}
                        onClick={() => setActiveTab('userList')}
                    >
                        Danh sách thành viên
                    </button>
                </li>
                <li className="nav-item">
                    <button
                        type="button"
                        className={`nav-link ${activeTab === 'departments' ? 'active' : ''}`}
                        onClick={() => setActiveTab('departments')}
                    >
                        Quản lý khoa
                    </button>
                </li>
            </ul>

            {loading && <div className="spinner-border text-primary" role="status"><span className="visually-hidden">Đang tải...</span></div>}

            {!loading && activeTab === 'createUser' && (
                <div>
                    <div className="card mb-3">
                        <div className="card-header">Tạo tài khoản</div>
                        <div className="card-body">
                            <div className="row g-2 align-items-end">
                                <div className="col-md-3">
                                    <label className="form-label">Tên đăng nhập</label>
                                    <input
                                        className="form-control"
                                        value={userCreate.username}
                                        onChange={(e) => setUserCreate(s => ({ ...s, username: e.target.value }))}
                                    />
                                </div>
                                <div className="col-md-3">
                                    <label className="form-label">Mật khẩu</label>
                                    <input
                                        type="password"
                                        className="form-control"
                                        value={userCreate.password}
                                        onChange={(e) => setUserCreate(s => ({ ...s, password: e.target.value }))}
                                    />
                                </div>
                                <div className="col-md-6">
                                    <label className="form-label">Họ tên</label>
                                    <input
                                        className="form-control"
                                        value={userCreate.fullName}
                                        onChange={(e) => setUserCreate(s => ({ ...s, fullName: e.target.value }))}
                                    />
                                </div>
                                <div className="col-md-4">
                                    <label className="form-label">Email</label>
                                    <input
                                        className="form-control"
                                        value={userCreate.email}
                                        onChange={(e) => setUserCreate(s => ({ ...s, email: e.target.value }))}
                                    />
                                </div>
                                <div className="col-md-3">
                                    <label className="form-label">Số điện thoại</label>
                                    <input
                                        className="form-control"
                                        value={userCreate.phone}
                                        onChange={(e) => setUserCreate(s => ({ ...s, phone: e.target.value }))}
                                    />
                                </div>
                                <div className="col-md-3">
                                    <label className="form-label">Vai trò</label>
                                    <select
                                        className="form-select"
                                        value={userCreate.roleId}
                                        onChange={(e) => setUserCreate(s => {
                                            const nextRoleId = e.target.value;
                                            const id = Number(nextRoleId);
                                            const role = roles.find(r => r.id === id);
                                            const name = normalizeKey(role?.name);
                                            const next = { ...s, roleId: nextRoleId };
                                            if (name === 'ADMIN') {
                                                return { ...next, departmentId: '' };
                                            }
                                            if (!next.departmentId && selectedDepartmentId) {
                                                return { ...next, departmentId: selectedDepartmentId };
                                            }
                                            return next;
                                        })}
                                    >
                                        <option value="">Chọn vai trò...</option>
                                        {roles.map(r => (
                                            <option key={r.id} value={String(r.id)}>{getRoleLabel(r.name)}</option>
                                        ))}
                                    </select>
                                </div>
                                <div className="col-md-2">
                                    <label className="form-label">Khoa</label>
                                    <select
                                        className="form-select"
                                        value={userCreate.departmentId}
                                        onChange={(e) => setUserCreate(s => ({ ...s, departmentId: e.target.value }))}
                                        disabled={createRoleKey === 'ADMIN'}
                                    >
                                        <option value="">Chọn khoa...</option>
                                        {departments.map(d => (
                                            <option key={d.id} value={String(d.id)}>
                                                {d.code} - {d.name}{d.active ? '' : ' (Không hoạt động)'}
                                            </option>
                                        ))}
                                    </select>
                                </div>
                                <div className="col-md-3">
                                    <div className="form-check">
                                        <input
                                            className="form-check-input"
                                            type="checkbox"
                                            checked={!!userCreate.active}
                                            onChange={(e) => setUserCreate(s => ({ ...s, active: e.target.checked }))}
                                            id="createUserActive"
                                        />
                                        <label className="form-check-label" htmlFor="createUserActive">Kích hoạt</label>
                                    </div>
                                </div>

                                {createRoleKey === 'STUDENT' && (
                                    <>
                                        <div className="col-md-3">
                                            <label className="form-label">Mã sinh viên</label>
                                            <input
                                                className="form-control"
                                                value={userCreate.studentCode}
                                                onChange={(e) => setUserCreate(s => ({ ...s, studentCode: e.target.value }))}
                                            />
                                        </div>
                                        <div className="col-md-4">
                                            <label className="form-label">Chọn lớp</label>
                                            <select
                                                className="form-select"
                                                value={userCreate.academicClassId}
                                                onChange={(e) => setUserCreate(s => ({ ...s, academicClassId: e.target.value }))}
                                            >
                                                <option value="">-- Chọn --</option>
                                                {academicClasses.filter(c => c.active).map(c => (
                                                    <option key={c.id} value={String(c.id)}>{c.code} - {c.name}</option>
                                                ))}
                                            </select>
                                        </div>
                                        <div className="col-md-5">
                                            <label className="form-label">Tên lớp (tuỳ chọn)</label>
                                            <input
                                                className="form-control"
                                                value={userCreate.className}
                                                onChange={(e) => setUserCreate(s => ({ ...s, className: e.target.value }))}
                                            />
                                        </div>
                                    </>
                                )}

                                {createRoleKey === 'LECTURER' && (
                                    <>
                                        <div className="col-md-3">
                                            <label className="form-label">Học vị</label>
                                            <input
                                                className="form-control"
                                                value={userCreate.degree}
                                                onChange={(e) => setUserCreate(s => ({ ...s, degree: e.target.value }))}
                                            />
                                        </div>
                                        <div className="col-md-9">
                                            <label className="form-label">Chuyên môn</label>
                                            <input
                                                className="form-control"
                                                value={userCreate.speciality}
                                                onChange={(e) => setUserCreate(s => ({ ...s, speciality: e.target.value }))}
                                            />
                                        </div>
                                    </>
                                )}

                                <div className="col-12">
                                    <button type="button" className="btn btn-primary" onClick={createUser} disabled={!userCreate.roleId}>
                                        Tạo người dùng
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {!loading && activeTab === 'userList' && (
                <div>
                    <div className="card">
                        <div className="card-header">Danh sách thành viên</div>
                        <div className="card-body">
                            <div className="row g-3 align-items-end mb-3">
                                <div className="col-md-5">
                                    <label className="form-label">Khoa</label>
                                    <select
                                        className="form-select"
                                        value={selectedDepartmentId}
                                        onChange={(e) => setSelectedDepartmentId(e.target.value)}
                                    >
                                        <option value="">Chọn khoa...</option>
                                        {departments.map(d => (
                                            <option key={d.id} value={String(d.id)}>
                                                {d.code} - {d.name}{d.active ? '' : ' (Không hoạt động)'}
                                            </option>
                                        ))}
                                    </select>
                                </div>
                                <div className="col-md-4">
                                    <label className="form-label">Vai trò</label>
                                    <select
                                        className="form-select"
                                        value={selectedUserRole}
                                        onChange={(e) => setSelectedUserRole(e.target.value)}
                                    >
                                        <option value="">Tất cả</option>
                                        {roles.map(r => (
                                            <option key={r.id} value={r.name}>{getRoleLabel(r.name)}</option>
                                        ))}
                                    </select>
                                </div>
                                <div className="col-md-3 text-end">
                                    <button type="button" className="btn btn-outline-primary w-100" onClick={loadUsers} disabled={!selectedDepartmentId && selectedUserRole !== 'ADMIN'}>
                                        Tải lại
                                    </button>
                                </div>
                            </div>

                            {!selectedDepartmentId && selectedUserRole !== 'ADMIN' ? (
                                <div className="alert alert-info mb-0">Vui lòng chọn khoa để xem danh sách người dùng.</div>
                            ) : (
                                <div className="table-responsive">
                                    <table className="table table-striped table-hover mb-0">
                                        <thead className="table-dark">
                                            <tr>
                                                <th>ID</th>
                                                <th>Tên đăng nhập</th>
                                                <th>Họ tên</th>
                                                <th>Email</th>
                                                <th>Khoa</th>
                                                <th>Vai trò</th>
                                                <th>Trạng thái</th>
                                                <th>Hành động</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {users.map(u => (
                                                <tr key={u.id}>
                                                    <td>{u.id}</td>
                                                    <td>{u.username}</td>
                                                    <td>{u.fullName}</td>
                                                    <td>{u.email}</td>
                                                    <td>{u.department ? `${u.department.code} - ${u.department.name}` : ''}</td>
                                                    <td>
                                                        <select
                                                            className="form-select form-select-sm"
                                                            value={u.role ? u.role.id : ''}
                                                            onChange={(e) => changeUserRole(u, e.target.value)}
                                                            disabled={u.role && u.role.name === 'ADMIN'}
                                                        >
                                                            {roles.map(r => (
                                                                <option key={r.id} value={r.id}>{getRoleLabel(r.name)}</option>
                                                            ))}
                                                        </select>
                                                    </td>
                                                    <td>
                                                        <span className={`badge ${u.active ? 'bg-success' : 'bg-danger'}`}>
                                                            {u.active ? 'Hoạt động' : 'Bị khóa'}
                                                        </span>
                                                    </td>
                                                    <td>
                                                        {u.role && u.role.name !== 'ADMIN' && (
                                                            <button type="button"
                                                                className={`btn btn-sm ${u.active ? 'btn-warning' : 'btn-success'}`}
                                                                onClick={() => toggleUserStatus(u)}
                                                            >
                                                                {u.active ? 'Khóa' : 'Mở khóa'}
                                                            </button>
                                                        )}
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            )}

            {!loading && activeTab === 'departments' && (
                <div>
                    <div className="card mb-3">
                        <div className="card-header">Tạo khoa</div>
                        <div className="card-body">
                            <div className="row g-2 align-items-end">
                                <div className="col-md-3">
                                    <label className="form-label">Mã khoa</label>
                                    <input
                                        className="form-control"
                                        value={departmentCreate.code}
                                        onChange={(e) => setDepartmentCreate(s => ({ ...s, code: e.target.value }))}
                                    />
                                </div>
                                <div className="col-md-6">
                                    <label className="form-label">Tên khoa</label>
                                    <input
                                        className="form-control"
                                        value={departmentCreate.name}
                                        onChange={(e) => setDepartmentCreate(s => ({ ...s, name: e.target.value }))}
                                    />
                                </div>
                                <div className="col-md-3">
                                    <div className="form-check mb-2">
                                        <input
                                            className="form-check-input"
                                            type="checkbox"
                                            checked={!!departmentCreate.active}
                                            onChange={(e) => setDepartmentCreate(s => ({ ...s, active: e.target.checked }))}
                                            id="createDepartmentActive"
                                        />
                                        <label className="form-check-label" htmlFor="createDepartmentActive">Kích hoạt</label>
                                    </div>
                                    <button type="button" className="btn btn-primary w-100" onClick={createDepartment}>
                                        Tạo khoa
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="card">
                        <div className="card-header">Danh sách khoa</div>
                        <div className="card-body p-0">
                            <div className="table-responsive">
                                <table className="table table-striped table-hover mb-0">
                                    <thead className="table-dark">
                                        <tr>
                                            <th>ID</th>
                                            <th>Mã khoa</th>
                                            <th>Tên khoa</th>
                                            <th>Trạng thái</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {departments.map(d => (
                                            <tr key={d.id}>
                                                <td>{d.id}</td>
                                                <td>{d.code}</td>
                                                <td>{d.name}</td>
                                                <td>
                                                    <span className={`badge ${d.active ? 'bg-success' : 'bg-secondary'}`}>
                                                        {d.active ? 'Hoạt động' : 'Không hoạt động'}
                                                    </span>
                                                </td>
                                            </tr>
                                        ))}
                                        {departments.length === 0 && (
                                            <tr>
                                                <td colSpan="4" className="text-center text-muted p-3">Chưa có khoa</td>
                                            </tr>
                                        )}
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
