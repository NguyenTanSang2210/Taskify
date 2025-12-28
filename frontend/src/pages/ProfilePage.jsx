import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import api from '../api';
import InlineNotice from '../components/InlineNotice';

export default function ProfilePage() {
    const { user, updateUser } = useAuth();
    
    const [activeTab, setActiveTab] = useState('info'); // 'info' or 'password'
    const [loading, setLoading] = useState(false);
    const [notice, setNotice] = useState(null);

    // Form states
    const [fullName, setFullName] = useState("");
    const [email, setEmail] = useState("");
    const [username, setUsername] = useState("");
    
    // Password states
    const [oldPassword, setOldPassword] = useState("");
    const [newPassword, setNewPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");

    const fetchLatestProfile = useCallback(async () => {
        if (!user?.id) return;
        try {
            const res = await api.get(`/users/${user.id}`);
            if (res.data) {
                const data = res.data;
                
                // Fix: Backend returns Role entity object, but frontend expects role name string
                const cleanData = { ...data };
                if (cleanData.role && typeof cleanData.role === 'object') {
                    cleanData.role = cleanData.role.name;
                }

                // Sync context silently if changed
                if (cleanData.fullName !== user.fullName || 
                    cleanData.email !== user.email || 
                    cleanData.role !== user.role) {
                    
                    // Merge carefully to preserve token
                    updateUser({ ...user, ...cleanData });
                }
            }
        } catch (err) {
            console.error(err);
        }
    }, [user, updateUser]);

    useEffect(() => {
        if (user) {
            setFullName(user.fullName || "");
            setEmail(user.email || "");
            setUsername(user.username || "");
            fetchLatestProfile();
        }
    }, [user, fetchLatestProfile]);

    const handleUpdateInfo = async (e) => {
        e.preventDefault();
        setLoading(true);
        setNotice(null);
        try {
            const res = await api.put(`/users/${user.id}/profile`, { fullName, email });
            updateUser({ ...user, fullName: res.data.fullName, email: res.data.email });
            setNotice({ type: 'success', message: 'Cập nhật thông tin thành công' });
        } catch (err) {
            setNotice({ type: 'danger', message: err.message || 'Lỗi cập nhật hồ sơ' });
        } finally {
            setLoading(false);
        }
    };

    const handleChangePassword = async (e) => {
        e.preventDefault();
        setNotice(null);
        
        if (newPassword !== confirmPassword) {
            setNotice({ type: 'danger', message: 'Mật khẩu xác nhận không khớp' });
            return;
        }
        
        if (newPassword.length < 6) {
             setNotice({ type: 'danger', message: 'Mật khẩu mới phải có ít nhất 6 ký tự' });
             return;
        }

        setLoading(true);
        try {
            await api.put(`/users/${user.id}/password`, { oldPassword, newPassword });
            setNotice({ type: 'success', message: 'Đổi mật khẩu thành công' });
            setOldPassword('');
            setNewPassword('');
            setConfirmPassword('');
        } catch (err) {
            setNotice({ type: 'danger', message: err.message || 'Mật khẩu cũ không chính xác hoặc lỗi hệ thống' });
        } finally {
            setLoading(false);
        }
    };

    if (!user) return <div className="p-5 text-center">Vui lòng đăng nhập để xem hồ sơ.</div>;

    const getRoleDisplayName = (role) => {
        if (!role) return 'Không xác định';
        
        // Handle if role is an object (e.g. {id: 1, name: "STUDENT", ...})
        let roleNameStr = role;
        if (typeof role === 'object' && role !== null) {
             roleNameStr = role.name || ''; 
        }

        const r = String(roleNameStr).toUpperCase().trim();
        if (r === 'STUDENT') return 'Sinh viên';
        if (r === 'LECTURER') return 'Giảng viên';
        if (r === 'ADMIN') return 'Quản trị viên';
        if (r === 'DEPARTMENT_ADMIN') return 'Quản trị khoa';
        return roleNameStr; // fallback to original string
    };

    const roleName = getRoleDisplayName(user.role);

    return (
        <div className="container-fluid py-4" style={{ backgroundColor: '#f8f9fa', minHeight: '100%' }}>
            <div className="row">
                {/* Left Sidebar */}
                <div className="col-lg-3 col-md-4 mb-4">
                    <div className="profile-card p-4 text-center">
                        <div className="profile-avatar-container">
                            <i className="bi bi-person-fill" style={{ fontSize: '4rem' }}></i>
                        </div>
                        <h5 className="profile-name">{user.fullName}</h5>
                        <span className="profile-role-badge">{roleName}</span>
                        <p className="text-muted small mt-2 mb-0">{user.email}</p>
                    </div>

                    <div className="profile-card p-0 overflow-hidden">
                        <div 
                            className={`profile-nav-item ${activeTab === 'info' ? 'active' : ''}`}
                            onClick={() => setActiveTab('info')}
                        >
                            <i className="bi bi-person-lines-fill me-2"></i> Thông tin chung
                        </div>
                        <div 
                            className={`profile-nav-item ${activeTab === 'password' ? 'active' : ''}`}
                            onClick={() => setActiveTab('password')}
                        >
                            <i className="bi bi-shield-lock-fill me-2"></i> Đổi mật khẩu
                        </div>
                    </div>
                </div>

                {/* Right Content */}
                <div className="col-lg-9 col-md-8">
                    <div className="profile-card p-4">
                        {notice && (
                            <div className="mb-4">
                                <InlineNotice type={notice.type} message={notice.message} onClose={() => setNotice(null)} />
                            </div>
                        )}

                        {activeTab === 'info' && (
                            <form onSubmit={handleUpdateInfo}>
                                <h4 className="profile-section-title">Thông tin cá nhân</h4>
                                <div className="row mb-3">
                                    <div className="col-md-6">
                                        <label className="form-label text-muted small fw-bold">Tên đăng nhập / MSSV</label>
                                        <input 
                                            type="text" 
                                            className="form-control bg-light" 
                                            value={username} 
                                            readOnly 
                                            disabled 
                                        />
                                    </div>
                                    <div className="col-md-6">
                                        <label className="form-label text-muted small fw-bold">Vai trò</label>
                                        <input 
                                            type="text" 
                                            className="form-control bg-light" 
                                            value={roleName} 
                                            readOnly 
                                            disabled 
                                        />
                                    </div>
                                </div>

                                <div className="mb-3">
                                    <label className="form-label fw-bold">Họ và tên</label>
                                    <input 
                                        type="text" 
                                        className="form-control" 
                                        value={fullName}
                                        onChange={(e) => setFullName(e.target.value)}
                                        required
                                    />
                                </div>

                                <div className="mb-4">
                                    <label className="form-label fw-bold">Email</label>
                                    <input 
                                        type="email" 
                                        className="form-control" 
                                        value={email}
                                        onChange={(e) => setEmail(e.target.value)}
                                        required
                                    />
                                </div>

                                <div className="d-flex justify-content-end">
                                    <button type="submit" className="btn btn-primary" disabled={loading}>
                                        {loading ? <span className="spinner-border spinner-border-sm me-2"></span> : <i className="bi bi-save me-2"></i>}
                                        Lưu thay đổi
                                    </button>
                                </div>
                            </form>
                        )}

                        {activeTab === 'password' && (
                            <form onSubmit={handleChangePassword}>
                                <h4 className="profile-section-title">Đổi mật khẩu</h4>
                                <div className="mb-3">
                                    <label className="form-label fw-bold">Mật khẩu hiện tại</label>
                                    <input 
                                        type="password" 
                                        className="form-control" 
                                        value={oldPassword}
                                        onChange={(e) => setOldPassword(e.target.value)}
                                        required
                                    />
                                </div>
                                <div className="mb-3">
                                    <label className="form-label fw-bold">Mật khẩu mới</label>
                                    <input 
                                        type="password" 
                                        className="form-control" 
                                        value={newPassword}
                                        onChange={(e) => setNewPassword(e.target.value)}
                                        required
                                        placeholder="Ít nhất 6 ký tự"
                                    />
                                </div>
                                <div className="mb-4">
                                    <label className="form-label fw-bold">Xác nhận mật khẩu mới</label>
                                    <input 
                                        type="password" 
                                        className="form-control" 
                                        value={confirmPassword}
                                        onChange={(e) => setConfirmPassword(e.target.value)}
                                        required
                                    />
                                </div>
                                <div className="d-flex justify-content-end">
                                    <button type="submit" className="btn btn-danger" disabled={loading}>
                                        {loading ? <span className="spinner-border spinner-border-sm me-2"></span> : <i className="bi bi-key me-2"></i>}
                                        Cập nhật mật khẩu
                                    </button>
                                </div>
                            </form>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
}
