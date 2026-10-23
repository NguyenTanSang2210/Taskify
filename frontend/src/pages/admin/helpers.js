export const normalizeKey = (value) => String(value ?? '').trim().toUpperCase();
export const getRoleLabel = (roleName) => {
    const key = normalizeKey(roleName);
    if (key === 'ADMIN') return 'Quản trị hệ thống';
    if (key === 'DEPARTMENT_ADMIN') return 'Quản trị khoa';
    if (key === 'LECTURER') return 'Giảng viên';
    if (key === 'STUDENT') return 'Sinh viên';
    return roleName || '';
};
