import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from "react-router-dom";
import api from '../api';
import userApi from '../api/userApi';
import InlineNotice from '../components/InlineNotice';

const normalizeKey = (value) => String(value ?? '').trim().toUpperCase();
const getRoleLabel = (roleName) => {
    const key = normalizeKey(roleName);
    if (key === 'ADMIN') return 'Quản trị hệ thống';
    if (key === 'DEPARTMENT_ADMIN') return 'Quản trị khoa';
    if (key === 'LECTURER') return 'Giảng viên';
    if (key === 'STUDENT') return 'Sinh viên';
    return roleName || '';
};

export default function AdminPage() {
    const [loading, setLoading] = useState(false);
    const [activeTab, setActiveTab] = useState('createUser');
    const [notice, setNotice] = useState(null);

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
        }
    }, []);

    const createDepartment = async (e) => {
        e?.preventDefault?.();
        const code = departmentCreate.code.trim();
        const name = departmentCreate.name.trim();
        if (!code || !name) {
            setNotice({ type: 'warning', message: 'Vui lòng nhập mã và tên khoa.' });
            return;
        }
        setLoading(true);
        try {
            await api.post('/departments', { code, name, active: !!departmentCreate.active });
            setNotice({ type: 'success', message: 'Hệ thống đã khởi tạo khoa mới thành công!' });
            setDepartmentCreate({ code: '', name: '', active: true });
            await loadDepartments();
        } catch (error) {
            setNotice({ type: 'danger', message: error.message || 'Lỗi tạo khoa' });
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
        } finally {
            setLoading(false);
        }
    }, [selectedDepartmentId, selectedUserRole]);

    const toggleUserStatus = async (u) => {
        if (!window.confirm(`Bạn có chắc muốn ${u.active ? 'khóa' : 'mở khóa'} tài khoản ${u.username}?`)) return;
        try {
            await userApi.updateStatus(u.id, !u.active);
            setNotice({ type: 'success', message: `Đã cập nhật trạng thái tài khoản ${u.username}.` });
            loadUsers();
        } catch (error) {
            setNotice({ type: 'danger', message: 'Không thể cập nhật trạng thái người dùng.' });
        }
    };

    const changeUserRole = async (u, newRoleId) => {
        if (!window.confirm(`Xác nhận thay đổi vai trò của ${u.username}?`)) return;
        try {
            await userApi.updateRole(u.id, newRoleId);
            setNotice({ type: 'success', message: `Đã cập nhật vai trò mới cho ${u.username}.` });
            loadUsers();
        } catch (error) {
            setNotice({ type: 'danger', message: 'Không thể thay đổi vai trò.' });
        }
    };

    const createUser = async (e) => {
        e?.preventDefault?.();
        const username = userCreate.username.trim();
        const password = userCreate.password.trim();
        const fullName = userCreate.fullName.trim();
        const roleId = userCreate.roleId ? Number(userCreate.roleId) : null;

        if (!username || !password || !fullName || !roleId) {
            setNotice({ type: 'warning', message: 'Vui lòng điền đầy đủ các thông tin bắt buộc.' });
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
            active: !!userCreate.active,
            email: userCreate.email.trim(),
            phone: userCreate.phone.trim()
        };

        if (roleName && roleName !== 'ADMIN') {
            if (!departmentIdRaw) {
                setNotice({ type: 'warning', message: 'Vui lòng chọn khoa cho người dùng.' });
                return;
            }
            payload.departmentId = Number(departmentIdRaw);
        }

        if (roleName === 'STUDENT') {
            const studentCode = userCreate.studentCode.trim();
            if (!studentCode) {
                setNotice({ type: 'warning', message: 'Mã sinh viên là bắt buộc.' });
                return;
            }
            payload.studentCode = studentCode;
            if (userCreate.className) payload.className = userCreate.className.trim();
            if (userCreate.academicClassId) payload.academicClassId = Number(userCreate.academicClassId);
        }

        if (roleName === 'LECTURER') {
            if (userCreate.degree) payload.degree = userCreate.degree.trim();
            if (userCreate.speciality) payload.speciality = userCreate.speciality.trim();
        }

        setLoading(true);
        try {
            await userApi.create(payload);
            setNotice({ type: 'success', message: 'Đã tạo tài khoản mới thành công!' });
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
            setActiveTab('userList');
        } catch (error) {
            setNotice({ type: 'danger', message: error.message || 'Lỗi hệ thống khi tạo người dùng' });
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
        if (activeTab === 'userList') loadUsers();
        if (activeTab === 'departments') loadDepartments();
    }, [activeTab, loadUsers, loadDepartments]);

    return (
        <div className="space-y-8 animate-in fade-in duration-500 pb-12">
            {/* Header / Hero Section */}
            <div className="flex flex-col md:flex-row md:items-end justify-between gap-6 shrink-0">
                <div>
                  <span className="text-[10px] font-black uppercase tracking-[0.2em] text-primary mb-2 block">Cấu hình hạ tầng</span>
                  <h1 className="text-3xl font-black text-on-surface tracking-tight font-headline uppercase leading-none">Quản trị hệ thống</h1>
                  <p className="text-sm text-outline mt-2 font-medium max-w-2xl">
                    Trung tâm điều phối người dùng, quản lý khoa ngành và kiểm soát phân quyền toàn diện trên nền tảng.
                  </p>
                </div>
                
                <div className="flex items-center gap-3">
                   <Link 
                     to="/roles"
                     className="px-6 py-2.5 bg-primary/10 text-primary border border-primary/20 rounded-xl shadow-sm text-[10px] font-black uppercase tracking-widest flex items-center gap-2 hover:bg-primary hover:text-white transition-all"
                   >
                      <span className="material-symbols-outlined text-sm">key</span>
                      Quản lý Quyền hạn
                   </Link>
                   <div className="px-4 py-2 bg-white dark:bg-slate-900 border border-outline-variant/10 rounded-xl shadow-sm text-[10px] font-black uppercase tracking-widest flex items-center gap-2">
                      <span className="material-symbols-outlined text-sm">shield_person</span>
                      {roles.length} VAI TRÒ
                   </div>
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
            <div className="bg-white dark:bg-slate-900 p-2 rounded-[2rem] border border-outline-variant/10 shadow-sm flex flex-wrap gap-1">
                {[
                    { id: 'createUser', label: 'Khởi tạo tài khoản', icon: 'person_add' },
                    { id: 'userList', label: 'Danh bạ thành viên', icon: 'group_work' },
                    { id: 'departments', label: 'Quản lý Khoa', icon: 'account_tree' }
                ].map(tab => (
                    <button 
                        key={tab.id}
                        onClick={() => setActiveTab(tab.id)}
                        className={`flex items-center gap-3 px-8 py-3.5 rounded-[1.5rem] text-[10px] font-black uppercase tracking-widest transition-all duration-300 ${
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

            {/* Content Area Rendering */}
            <div className="bg-white dark:bg-slate-900 rounded-[2.5rem] border border-outline-variant/10 shadow-sm overflow-hidden min-h-[500px]">
                {loading && activeTab !== 'userList' && (
                    <div className="py-32 flex flex-col items-center justify-center opacity-30 select-none">
                        <div className="animate-spin rounded-full h-10 w-10 border-4 border-primary border-t-transparent mb-4"></div>
                        <p className="text-[10px] font-black uppercase tracking-widest">Đang tải dữ liệu cấu hình...</p>
                    </div>
                )}

                {activeTab === 'createUser' && !loading && (
                    <div className="p-10 space-y-10 animate-in slide-in-from-bottom-4 duration-500">
                        <div className="flex justify-between items-center bg-surface-container-low/30 p-6 rounded-[2rem] border border-outline-variant/5">
                            <div>
                                <h2 className="text-xl font-black text-on-surface uppercase tracking-tight font-headline">Thông tin tài khoản mới</h2>
                                <p className="text-[10px] text-outline font-bold mt-1 uppercase tracking-widest">Thiết lập định danh và phân quyền cơ sở</p>
                            </div>
                            <div className="px-5 py-2 bg-primary/10 text-primary rounded-xl text-[10px] font-black uppercase tracking-widest border border-primary/20">
                                {getRoleLabel(createRoleName || 'VAI TRÒ')}
                            </div>
                        </div>

                        <form onSubmit={createUser} className="space-y-8">
                            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
                                <div className="space-y-1.5">
                                    <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Tên đăng nhập (Username)</label>
                                    <input className="w-full px-6 py-4 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-2xl text-sm font-black transition-all duration-300 outline-none" value={userCreate.username} onChange={(e) => setUserCreate(s => ({ ...s, username: e.target.value }))} placeholder="VD: 202160...." required />
                                </div>
                                <div className="space-y-1.5">
                                    <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Mật khẩu khởi tạo</label>
                                    <input type="password" className="w-full px-6 py-4 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-2xl text-sm font-black transition-all duration-300 outline-none" value={userCreate.password} onChange={(e) => setUserCreate(s => ({ ...s, password: e.target.value }))} placeholder="••••••••" required />
                                </div>
                                <div className="space-y-1.5 lg:col-span-1">
                                    <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Họ và tên đầy đủ</label>
                                    <input className="w-full px-6 py-4 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-2xl text-sm font-black transition-all duration-300 outline-none" value={userCreate.fullName} onChange={(e) => setUserCreate(s => ({ ...s, fullName: e.target.value }))} placeholder="Nguyễn Văn A" required />
                                </div>
                                
                                <div className="space-y-1.5">
                                    <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Địa chỉ Email</label>
                                    <input className="w-full px-6 py-4 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-2xl text-sm font-black transition-all duration-300 outline-none" value={userCreate.email} onChange={(e) => setUserCreate(s => ({ ...s, email: e.target.value }))} placeholder="example@email.com" />
                                </div>
                                <div className="space-y-1.5">
                                    <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Số điện thoại</label>
                                    <input className="w-full px-6 py-4 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-2xl text-sm font-black transition-all duration-300 outline-none" value={userCreate.phone} onChange={(e) => setUserCreate(s => ({ ...s, phone: e.target.value }))} placeholder="090..." />
                                </div>
                                <div className="space-y-1.5">
                                    <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Phân quyền vai trò</label>
                                    <select className="w-full px-6 py-4 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-2xl text-xs font-black uppercase tracking-widest transition-all duration-300 outline-none cursor-pointer" value={userCreate.roleId} onChange={(e) => setUserCreate(s => ({ ...s, roleId: e.target.value }))}>
                                        <option value="">Chọn vai trò...</option>
                                        {roles.map(r => <option key={r.id} value={String(r.id)}>{getRoleLabel(r.name)}</option>)}
                                    </select>
                                </div>

                                <div className="space-y-1.5">
                                    <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Đơn vị Khoa quản lý</label>
                                    <select className="w-full px-6 py-4 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-2xl text-xs font-black uppercase tracking-widest transition-all duration-300 outline-none cursor-pointer disabled:opacity-30" value={userCreate.departmentId} onChange={(e) => setUserCreate(s => ({ ...s, departmentId: e.target.value }))} disabled={createRoleKey === 'ADMIN'}>
                                        <option value="">Chọn khoa...</option>
                                        {departments.map(d => <option key={d.id} value={String(d.id)}>{d.code} - {d.name}</option>)}
                                    </select>
                                </div>

                                <div className="flex items-center gap-4 lg:pt-8">
                                   <button 
                                      type="button" 
                                      onClick={() => setUserCreate(s => ({ ...s, active: !s.active }))}
                                      className={`flex items-center gap-3 px-6 py-4 rounded-2xl border transition-all ${
                                        userCreate.active 
                                        ? "bg-primary/5 border-primary text-primary" 
                                        : "bg-surface-container border-outline-variant/20 text-outline"
                                      }`}
                                   >
                                      <span className="material-symbols-outlined">{userCreate.active ? 'check_circle' : 'do_not_disturb_on'}</span>
                                      <span className="text-[10px] font-black uppercase tracking-widest">{userCreate.active ? 'Kích hoạt' : 'Bản nháp (Khóa)'}</span>
                                   </button>
                                </div>
                            </div>

                            {createRoleKey === 'STUDENT' && (
                                <div className="p-8 bg-surface-container-low/30 rounded-[2rem] border border-outline-variant/10 grid grid-cols-1 md:grid-cols-3 gap-8 animate-in zoom-in-95 duration-300">
                                   <div className="space-y-1.5">
                                       <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Mã số sinh viên (MSSV)</label>
                                       <input className="w-full px-6 py-4 bg-white border-transparent focus:border-primary focus:ring-0 rounded-2xl text-sm font-black transition-all outline-none" value={userCreate.studentCode} onChange={(e) => setUserCreate(s => ({ ...s, studentCode: e.target.value }))} required />
                                   </div>
                                   <div className="space-y-1.5">
                                       <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Chọn lớp học cố định</label>
                                       <select className="w-full px-6 py-4 bg-white border-transparent focus:border-primary focus:ring-0 rounded-2xl text-[10px] font-black uppercase tracking-widest outline-none" value={userCreate.academicClassId} onChange={(e) => setUserCreate(s => ({ ...s, academicClassId: e.target.value }))}>
                                           <option value="">-- Chọn danh sách --</option>
                                           {academicClasses.filter(c => c.active).map(c => <option key={c.id} value={String(c.id)}>{c.code} - {c.name}</option>)}
                                       </select>
                                   </div>
                                   <div className="space-y-1.5">
                                       <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Tên lớp học tùy chỉnh</label>
                                       <input className="w-full px-6 py-4 bg-white border-transparent focus:border-primary focus:ring-0 rounded-2xl text-sm font-black transition-all outline-none" value={userCreate.className} onChange={(e) => setUserCreate(s => ({ ...s, className: e.target.value }))} placeholder="VD: CNTT1-K25" />
                                   </div>
                                </div>
                            )}

                            {createRoleKey === 'LECTURER' && (
                                <div className="p-8 bg-surface-container-low/30 rounded-[2rem] border border-outline-variant/10 grid grid-cols-1 md:grid-cols-3 gap-8 animate-in zoom-in-95 duration-300">
                                   <div className="space-y-1.5 col-span-1">
                                       <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Học hàm / Học vị</label>
                                       <input className="w-full px-6 py-4 bg-white border-transparent focus:border-primary focus:ring-0 rounded-2xl text-sm font-black transition-all outline-none" value={userCreate.degree} onChange={(e) => setUserCreate(s => ({ ...s, degree: e.target.value }))} placeholder="VD: Tiến sĩ" />
                                   </div>
                                   <div className="space-y-1.5 col-span-2">
                                       <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Lĩnh vực chuyên môn nghiên cứu</label>
                                       <input className="w-full px-6 py-4 bg-white border-transparent focus:border-primary focus:ring-0 rounded-2xl text-sm font-black transition-all outline-none" value={userCreate.speciality} onChange={(e) => setUserCreate(s => ({ ...s, speciality: e.target.value }))} placeholder="VD: Trí tuệ nhân tạo, Big Data..." />
                                   </div>
                                </div>
                            )}

                            <div className="pt-8 border-t border-outline-variant/5 flex justify-end">
                                <button type="submit" className="px-12 py-4 bg-primary text-white rounded-2xl font-black text-xs uppercase tracking-[0.2em] shadow-2xl shadow-primary/30 hover:scale-105 active:scale-95 transition-all flex items-center gap-3">
                                   <span className="material-symbols-outlined">how_to_reg</span>
                                   Khởi tạo tài khoản hệ thống
                                </button>
                            </div>
                        </form>
                    </div>
                )}

                {activeTab === 'userList' && (
                    <div className="p-8 space-y-8 animate-in fade-in duration-500">
                        <div className="flex flex-col md:flex-row gap-6 items-end bg-surface-container-low/20 p-6 rounded-[2rem]">
                            <div className="flex-grow space-y-1.5">
                               <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Lọc theo Khoa/Viện</label>
                               <select className="w-full px-4 py-3 bg-white border-transparent focus:border-primary focus:ring-0 rounded-xl text-sm font-black transition-all outline-none" value={selectedDepartmentId} onChange={(e) => setSelectedDepartmentId(e.target.value)}>
                                  <option value="">Chọn khoa...</option>
                                  {departments.map(d => <option key={d.id} value={String(d.id)}>{d.code} - {d.name}</option>)}
                               </select>
                            </div>
                            <div className="flex-grow space-y-1.5">
                               <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Lọc theo vai trò</label>
                               <select className="w-full px-4 py-3 bg-white border-transparent focus:border-primary focus:ring-0 rounded-xl text-xs font-black uppercase tracking-widest outline-none" value={selectedUserRole} onChange={(e) => setSelectedUserRole(e.target.value)}>
                                  <option value="">Tất cả vai trò</option>
                                  {roles.map(r => <option key={r.id} value={r.name}>{getRoleLabel(r.name)}</option>)}
                               </select>
                            </div>
                            <button onClick={loadUsers} className="px-8 py-3 bg-white border border-outline-variant/20 rounded-xl text-[10px] font-black uppercase tracking-widest hover:bg-surface-container transition-all flex items-center gap-2">
                               <span className="material-symbols-outlined text-lg">refresh</span> Làm mới
                            </button>
                        </div>

                        <div className="overflow-x-auto min-h-[400px]">
                            <table className="w-full text-left border-collapse">
                                <thead>
                                    <tr className="bg-surface-container-low/50">
                                        <th className="px-6 py-4 text-[10px] uppercase tracking-widest font-black text-outline">Định danh</th>
                                        <th className="px-6 py-4 text-[10px] uppercase tracking-widest font-black text-outline">Thông tin cơ bản</th>
                                        <th className="px-6 py-4 text-[10px] uppercase tracking-widest font-black text-outline">Đơn vị quản lý</th>
                                        <th className="px-6 py-4 text-[10px] uppercase tracking-widest font-black text-outline">Vai trò hệ thống</th>
                                        <th className="px-6 py-4 text-[10px] uppercase tracking-widest font-black text-outline">Trạng thái</th>
                                        <th className="px-6 py-4 text-[10px] uppercase tracking-widest font-black text-outline text-right">Thao tác</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-outline-variant/5">
                                    {users.map(u => (
                                        <tr key={u.id} className="group hover:bg-primary/[0.01] transition-colors">
                                            <td className="px-6 py-5">
                                               <p className="text-xs font-black text-outline tracking-widest">#{u.id}</p>
                                               <p className="text-sm font-black text-on-surface uppercase group-hover:text-primary transition-colors">{u.username}</p>
                                            </td>
                                            <td className="px-6 py-5">
                                               <p className="text-sm font-black text-on-surface uppercase tracking-tight">{u.fullName}</p>
                                               <p className="text-[10px] text-outline font-bold lowercase opacity-60">{u.email || "No email"}</p>
                                            </td>
                                            <td className="px-6 py-5 max-w-[200px]">
                                               <span className="text-[11px] font-black text-secondary uppercase tracking-tighter leading-tight">
                                                  {u.department ? `${u.department.code} - ${u.department.name}` : <span className="text-outline/30 italic">Hệ thống chung</span>}
                                               </span>
                                            </td>
                                            <td className="px-6 py-5">
                                                <select 
                                                   className="px-3 py-1.5 bg-surface-container rounded-lg text-[10px] font-black uppercase tracking-widest outline-none border-none focus:ring-2 ring-primary/20 cursor-pointer disabled:opacity-40" 
                                                   value={u.role ? u.role.id : ''} 
                                                   onChange={(e) => changeUserRole(u, e.target.value)} 
                                                   disabled={u.role && u.role.name === 'ADMIN'}
                                                >
                                                    {roles.map(r => <option key={r.id} value={r.id}>{getRoleLabel(r.name)}</option>)}
                                                </select>
                                            </td>
                                            <td className="px-6 py-5">
                                               <div className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[9px] font-black uppercase tracking-widest ${u.active ? 'bg-green-500/10 text-green-600' : 'bg-error/10 text-error'}`}>
                                                  <span className="w-1.5 h-1.5 rounded-full bg-current"></span>
                                                  {u.active ? 'Hoạt động' : 'Tạm khóa'}
                                               </div>
                                            </td>
                                            <td className="px-6 py-5 text-right">
                                               {u.role && u.role.name !== 'ADMIN' && (
                                                 <button 
                                                    onClick={() => toggleUserStatus(u)}
                                                    className={`p-2.5 rounded-xl transition-all shadow-sm ${u.active ? 'bg-error/5 text-error hover:bg-error hover:text-white' : 'bg-green-500/5 text-green-600 hover:bg-green-600 hover:text-white'}`}
                                                    title={u.active ? 'Khóa tài khoản' : 'Mở khóa'}
                                                 >
                                                    <span className="material-symbols-outlined text-lg">{u.active ? 'lock' : 'lock_open'}</span>
                                                 </button>
                                               )}
                                            </td>
                                        </tr>
                                    ))}
                                    {users.length === 0 && (
                                       <tr>
                                          <td colSpan="6" className="py-32 text-center opacity-30 select-none">
                                             <span className="material-symbols-outlined text-5xl mb-4">search_off</span>
                                             <p className="text-[10px] font-black uppercase tracking-widest leading-relaxed">Không tìm thấy thành viên nào<br/>khớp với tiêu chí lọc.</p>
                                          </td>
                                       </tr>
                                    )}
                                </tbody>
                            </table>
                        </div>
                    </div>
                )}

                {activeTab === 'departments' && (
                    <div className="p-10 space-y-12 animate-in fade-in duration-500">
                        <section className="space-y-6">
                            <div className="bg-surface-container-low/30 p-8 rounded-[2.5rem] border border-outline-variant/10">
                               <div className="flex items-center gap-4 mb-8 text-primary">
                                  <span className="material-symbols-outlined text-3xl">domain_add</span>
                                  <h3 className="text-xl font-black uppercase tracking-tight font-headline">Thiết lập Khoa/Đơn vị mới</h3>
                               </div>
                               <div className="grid grid-cols-1 md:grid-cols-12 gap-8 items-end">
                                   <div className="md:col-span-3 space-y-1.5">
                                       <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Mã định danh khoa</label>
                                       <input className="w-full px-6 py-4 bg-white border-transparent focus:border-primary focus:ring-0 rounded-2xl text-sm font-black transition-all outline-none" value={departmentCreate.code} onChange={(e) => setDepartmentCreate(s => ({ ...s, code: e.target.value }))} placeholder="VD: CNTT, CK..." />
                                   </div>
                                   <div className="md:col-span-6 space-y-1.5">
                                       <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Tên đơn vị đầy đủ</label>
                                       <input className="w-full px-6 py-4 bg-white border-transparent focus:border-primary focus:ring-0 rounded-2xl text-sm font-black transition-all outline-none" value={departmentCreate.name} onChange={(e) => setDepartmentCreate(s => ({ ...s, name: e.target.value }))} placeholder="VD: Công nghệ Thông tin" />
                                   </div>
                                   <div className="md:col-span-3">
                                       <button onClick={createDepartment} className="w-full py-4 bg-primary text-white rounded-2xl font-black text-[10px] uppercase tracking-widest shadow-xl shadow-primary/20 hover:scale-[1.02] active:scale-95 transition-all">Xác nhận tạo khoa</button>
                                   </div>
                               </div>
                            </div>
                        </section>

                        <section className="space-y-6">
                            <div className="flex justify-between items-center px-4">
                               <h3 className="text-sm font-black text-outline uppercase tracking-widest">Danh mục đơn vị hiện hành ({departments.length})</h3>
                            </div>
                            <div className="overflow-x-auto bg-surface-container-low/10 rounded-[2.5rem] border border-outline-variant/5">
                                <table className="w-full text-left border-collapse">
                                    <thead>
                                        <tr className="bg-surface-container-low/50">
                                           <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline">Mã Khoa</th>
                                           <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline">Tên đơn vị chi tiết</th>
                                           <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline">Trạng thái</th>
                                           <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline text-right">Id</th>
                                        </tr>
                                    </thead>
                                    <tbody className="divide-y divide-outline-variant/5">
                                        {departments.map(d => (
                                           <tr key={d.id} className="group hover:bg-primary/[0.01] transition-colors">
                                              <td className="px-8 py-5">
                                                 <span className="text-sm font-black text-primary uppercase tracking-widest">{d.code}</span>
                                              </td>
                                              <td className="px-8 py-5">
                                                 <p className="text-base font-black text-on-surface uppercase tracking-tight">{d.name}</p>
                                              </td>
                                              <td className="px-8 py-5">
                                                 <div className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[9px] font-black uppercase tracking-widest ${d.active ? 'bg-green-500/10 text-green-600' : 'bg-surface-container text-outline'}`}>
                                                    {d.active ? 'Đang hoạt động' : 'Tạm ngưng'}
                                                 </div>
                                              </td>
                                              <td className="px-8 py-5 text-right font-black text-[10px] text-outline/30">#{d.id}</td>
                                           </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        </section>
                    </div>
                )}
            </div>
            
            {/* System Tip */}
            <div className="bg-primary/5 p-6 rounded-[2rem] border border-primary/10 flex items-center gap-4">
               <div className="w-10 h-10 rounded-xl bg-primary text-white flex items-center justify-center shrink-0">
                  <span className="material-symbols-outlined">analytics</span>
               </div>
               <p className="text-xs font-bold text-on-surface-variant leading-relaxed">
                  <span className="font-black text-primary mr-2 uppercase tracking-widest">Thông số quản trị:</span>
                  Tài khoản được khởi tạo sẽ mặc định được kích hoạt. Hãy đảm bảo mật khẩu khởi tạo đủ an toàn và cung cấp cho người dùng trong lần đăng nhập đầu tiên.
               </p>
            </div>
        </div>
    );
}
