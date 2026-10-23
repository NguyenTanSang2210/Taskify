import { useCallback, useEffect, useMemo, useState } from 'react';
import api from '../../api';
import userApi from '../../api/userApi';
import { isPasswordValid } from '../../components/PasswordChecklist';
import { normalizeKey } from './helpers';

/** Trạng thái + xử lý của AdminPage (tách ở P3-11). */
export function useAdmin() {
    const [loading, setLoading] = useState(false);
    const [activeTab, setActiveTab] = useState('createUser');
    const [notice, setNotice] = useState(null);

    const [users, setUsers] = useState([]);
    const [userPage, setUserPage] = useState(0);
    const [userPageInfo, setUserPageInfo] = useState({ totalPages: 0, totalElements: 0 });
    const [userQuery, setUserQuery] = useState('');
    const [userQueryInput, setUserQueryInput] = useState('');
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
            const paging = { page: userPage, size: 20, q: userQuery || undefined };
            let data;
            if (selectedUserRole === 'ADMIN') {
                data = await userApi.getAll({ ...paging, role: 'ADMIN' });
            } else {
                const departmentId = selectedDepartmentId ? Number(selectedDepartmentId) : null;
                if (!departmentId) {
                    setUsers([]);
                    setUserPageInfo({ totalPages: 0, totalElements: 0 });
                    return;
                }
                data = await userApi.getByDepartment({ ...paging, departmentId, role: selectedUserRole || undefined });
            }
            setUsers(data?.items || []);
            setUserPageInfo({ totalPages: data?.totalPages || 0, totalElements: data?.totalElements || 0 });
        } catch (error) {
            console.error('Failed to load users', error);
        } finally {
            setLoading(false);
        }
    }, [selectedDepartmentId, selectedUserRole, userPage, userQuery]);

    const toggleUserStatus = async (u) => {
        if (!window.confirm(`Bạn có chắc muốn ${u.active ? 'khóa' : 'mở khóa'} tài khoản ${u.username}?`)) return;
        try {
            await userApi.updateStatus(u.id, !u.active);
            setNotice({ type: 'success', message: `Đã cập nhật trạng thái tài khoản ${u.username}.` });
            loadUsers();
        } catch {
            setNotice({ type: 'danger', message: 'Không thể cập nhật trạng thái người dùng.' });
        }
    };

    const changeUserRole = async (u, newRoleId) => {
        if (!window.confirm(`Xác nhận thay đổi vai trò của ${u.username}?`)) return;
        try {
            await userApi.updateRole(u.id, newRoleId);
            setNotice({ type: 'success', message: `Đã cập nhật vai trò mới cho ${u.username}.` });
            loadUsers();
        } catch {
            setNotice({ type: 'danger', message: 'Không thể thay đổi vai trò.' });
        }
    };

    const resendActivation = async (u) => {
        try {
            await userApi.resendActivation(u.id);
            setNotice({ type: 'success', message: `Đã gửi lại mã kích hoạt tới email của ${u.username}.` });
        } catch (err) {
            setNotice({ type: 'danger', message: err.message || 'Không gửi lại được mã kích hoạt.' });
        }
    };

    const createUser = async (e) => {
        e?.preventDefault?.();
        const username = userCreate.username.trim();
        const password = userCreate.password.trim();
        const fullName = userCreate.fullName.trim();
        const roleId = userCreate.roleId ? Number(userCreate.roleId) : null;

        if (!username || !fullName || !roleId) {
            setNotice({ type: 'warning', message: 'Vui lòng điền đầy đủ các thông tin bắt buộc.' });
            return;
        }
        if (!password && !userCreate.email.trim()) {
            setNotice({ type: 'warning', message: 'Để trống mật khẩu thì cần nhập email: hệ thống sẽ gửi mã kích hoạt để người dùng tự đặt mật khẩu.' });
            return;
        }
        if (password && !isPasswordValid(password, username)) {
            setNotice({ type: 'warning', message: 'Mật khẩu tạm chưa đạt yêu cầu: 8–64 ký tự, có chữ cái và chữ số, không chứa tên đăng nhập.' });
            return;
        }

        const role = roles.find(r => r.id === roleId);
        const roleName = normalizeKey(role?.name);
        const departmentIdRaw = userCreate.departmentId || selectedDepartmentId;

        const payload = {
            username,
            ...(password ? { password } : {}),
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

    return { loading, setLoading, activeTab, setActiveTab, notice, setNotice, users, setUsers, userPage, setUserPage, userPageInfo, setUserPageInfo, userQuery, setUserQuery, userQueryInput, setUserQueryInput, roles, setRoles, departments, setDepartments, departmentCreate, setDepartmentCreate, selectedDepartmentId, setSelectedDepartmentId, selectedUserRole, setSelectedUserRole, academicClasses, setAcademicClasses, userCreate, setUserCreate, createRoleName, createRoleKey, loadDepartments, createDepartment, loadRoles, loadAcademicClasses, loadUsers, toggleUserStatus, changeUserRole, resendActivation, createUser };
}
