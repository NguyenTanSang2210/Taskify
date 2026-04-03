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
                const cleanData = { ...data };
                if (cleanData.role && typeof cleanData.role === 'object') {
                    cleanData.role = cleanData.role.name;
                }
                if (cleanData.fullName !== user.fullName || 
                    cleanData.email !== user.email || 
                    cleanData.role !== user.role) {
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
            setNotice({ type: 'success', message: 'Hồ sơ của bạn đã được cập nhật thành công!' });
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
            setNotice({ type: 'danger', message: 'Xác nhận mật khẩu không trùng khớp.' });
            return;
        }
        
        if (newPassword.length < 6) {
             setNotice({ type: 'danger', message: 'Mật khẩu phải chứa ít nhất 6 ký tự.' });
             return;
        }

        setLoading(true);
        try {
            await api.put(`/users/${user.id}/password`, { oldPassword, newPassword });
            setNotice({ type: 'success', message: 'Mật khẩu đã được thay đổi thành công!' });
            setOldPassword('');
            setNewPassword('');
            setConfirmPassword('');
        } catch (err) {
            setNotice({ type: 'danger', message: err.message || 'Mật khẩu cũ không chính xác.' });
        } finally {
            setLoading(false);
        }
    };

    if (!user) return (
       <div className="flex flex-col items-center justify-center p-20 text-center opacity-30 select-none">
          <span className="material-symbols-outlined text-7xl mb-4">account_circle</span>
          <h2 className="text-xl font-black uppercase tracking-widest text-outline">Truy cập bị từ chối</h2>
          <p className="text-xs font-bold text-outline mt-2 uppercase tracking-tight">Vui lòng đăng nhập để xem thông tin cá nhân.</p>
       </div>
    );

    const getRoleDisplayName = (role) => {
        if (!role) return 'N/A';
        let roleNameStr = role;
        if (typeof role === 'object' && role !== null) roleNameStr = role.name || ''; 
        const r = String(roleNameStr).toUpperCase().trim();
        if (r === 'STUDENT') return 'Sinh viên';
        if (r === 'LECTURER') return 'Giảng viên';
        if (r === 'ADMIN') return 'Quản trị viên';
        if (r === 'DEPARTMENT_ADMIN') return 'Quản trị khoa';
        return roleNameStr;
    };

    const roleName = getRoleDisplayName(user.role);

    return (
        <div className="space-y-8 animate-in fade-in duration-500 pb-12">
            {/* Header Area */}
            <div className="flex flex-col md:flex-row md:items-end justify-between gap-6 shrink-0">
                <div>
                  <span className="text-[10px] font-black uppercase tracking-[0.2em] text-primary mb-2 block">Cài đặt tài khoản</span>
                  <h1 className="text-3xl font-black text-on-surface tracking-tight font-headline uppercase leading-none">Hồ sơ cá nhân</h1>
                  <p className="text-sm text-outline mt-2 font-medium">Quản lý thông tin định danh, tùy chỉnh bảo mật và quyền truy cập của bạn.</p>
                </div>
                
                <div className="flex items-center gap-3">
                   <div className="px-4 py-2 bg-surface-container text-on-surface-variant rounded-xl text-[10px] font-black uppercase tracking-widest flex items-center gap-2 border border-outline-variant/10">
                      <span className="material-symbols-outlined text-sm">enhanced_encryption</span>
                      Dữ liệu được bảo mật
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

            <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
                {/* Sidebar Summary */}
                <aside className="lg:col-span-4 space-y-6">
                    <div className="bg-white dark:bg-slate-900 p-8 rounded-[2.5rem] border border-outline-variant/10 shadow-sm text-center relative overflow-hidden group">
                        <div className="absolute top-0 left-0 w-full h-24 bg-primary/5 group-hover:bg-primary/10 transition-colors"></div>
                        <div className="relative mt-4">
                           <div className="w-24 h-24 rounded-[2rem] bg-primary text-white flex items-center justify-center mx-auto mb-4 border-4 border-white dark:border-slate-900 shadow-xl">
                              <span className="text-3xl font-black uppercase">{user.fullName?.charAt(0)}</span>
                           </div>
                           <h2 className="text-xl font-black text-on-surface uppercase tracking-tight">{user.fullName}</h2>
                           <div className="mt-2 inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-surface-container text-on-surface-variant">
                              <span className="material-symbols-outlined text-sm">verified</span>
                              <span className="text-[10px] font-black uppercase tracking-widest">{roleName}</span>
                           </div>
                           <p className="mt-4 text-xs font-bold text-outline uppercase tracking-tight">{user.email}</p>
                        </div>
                    </div>

                    <div className="bg-white dark:bg-slate-900 p-2 rounded-[2rem] border border-outline-variant/10 shadow-sm space-y-1">
                        <button 
                            className={`w-full flex items-center gap-4 p-5 rounded-[1.5rem] transition-all duration-300 ${activeTab === 'info' ? 'bg-primary text-white shadow-lg shadow-primary/20' : 'hover:bg-surface-container text-outline hover:text-on-surface'}`}
                            onClick={() => setActiveTab('info')}
                        >
                            <span className="material-symbols-outlined">person</span>
                            <span className="text-[11px] font-black uppercase tracking-[0.2em]">Thông tin chung</span>
                        </button>
                        <button 
                            className={`w-full flex items-center gap-4 p-5 rounded-[1.5rem] transition-all duration-300 ${activeTab === 'password' ? 'bg-primary text-white shadow-lg shadow-primary/20' : 'hover:bg-surface-container text-outline hover:text-on-surface'}`}
                            onClick={() => setActiveTab('password')}
                        >
                            <span className="material-symbols-outlined">security</span>
                            <span className="text-[11px] font-black uppercase tracking-[0.2em]">Bảo mật & Mật khẩu</span>
                        </button>
                    </div>

                    <div className="p-6 bg-secondary/5 rounded-[2rem] border border-secondary/10">
                       <p className="text-[9px] font-black uppercase tracking-widest text-secondary block mb-2">Lời khuyên</p>
                       <p className="text-[11px] font-bold text-on-surface-variant leading-relaxed">Luôn sử dụng mật khẩu mạnh kết hợp chữ cái, chữ số và ký tự đặc biệt để bảo vệ tài sản học thuật của bạn.</p>
                    </div>
                </aside>

                {/* Content Area */}
                <main className="lg:col-span-8">
                    <div className="bg-white dark:bg-slate-900 p-10 rounded-[2.5rem] border border-outline-variant/10 shadow-sm min-h-[500px] animate-in slide-in-from-right-4 duration-500">
                        {activeTab === 'info' && (
                            <form onSubmit={handleUpdateInfo} className="space-y-8">
                                <div>
                                   <h3 className="text-xl font-black text-on-surface uppercase tracking-tight font-headline">Thông tin định danh</h3>
                                   <p className="text-[10px] text-outline font-bold mt-1 uppercase tracking-widest">Các thông tin cơ bản được hiển thị trên hệ thống</p>
                                </div>

                                <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                                    <div className="space-y-1.5">
                                        <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Tên đăng nhập / MSSV</label>
                                        <div className="px-6 py-4 bg-surface-container-low rounded-2xl border border-outline-variant/5 text-sm font-black text-outline/50 flex items-center gap-3 select-none">
                                           <span className="material-symbols-outlined text-lg">badge</span>
                                           {username}
                                        </div>
                                    </div>
                                    <div className="space-y-1.5">
                                        <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Vai trò hệ thống</label>
                                        <div className="px-6 py-4 bg-surface-container-low rounded-2xl border border-outline-variant/5 text-sm font-black text-outline/50 flex items-center gap-3 select-none">
                                           <span className="material-symbols-outlined text-lg">policy</span>
                                           {roleName}
                                        </div>
                                    </div>
                                </div>

                                <div className="space-y-1.5">
                                    <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Họ và tên đầy đủ</label>
                                    <div className="relative group">
                                       <span className="material-symbols-outlined absolute left-6 top-1/2 -translate-y-1/2 text-outline group-focus-within:text-primary transition-colors">edit_note</span>
                                       <input 
                                          type="text" 
                                          className="w-full pl-16 pr-6 py-4 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-2xl text-sm font-black transition-all duration-300 outline-none" 
                                          value={fullName}
                                          onChange={(e) => setFullName(e.target.value)}
                                          required
                                          placeholder="Nhập họ và tên..."
                                       />
                                    </div>
                                </div>

                                <div className="space-y-1.5">
                                    <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Địa chỉ Email liên hệ</label>
                                    <div className="relative group">
                                       <span className="material-symbols-outlined absolute left-6 top-1/2 -translate-y-1/2 text-outline group-focus-within:text-primary transition-colors">mail</span>
                                       <input 
                                          type="email" 
                                          className="w-full pl-16 pr-6 py-4 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-2xl text-sm font-black transition-all duration-300 outline-none" 
                                          value={email}
                                          onChange={(e) => setEmail(e.target.value)}
                                          required
                                          placeholder="name@university.edu.vn"
                                       />
                                    </div>
                                </div>

                                <div className="pt-6 border-t border-outline-variant/5 flex justify-end">
                                    <button 
                                       type="submit" 
                                       className="px-10 py-4 bg-primary text-white rounded-2xl font-black text-xs uppercase tracking-widest shadow-xl shadow-primary/20 hover:scale-105 active:scale-95 transition-all flex items-center gap-3 disabled:opacity-50" 
                                       disabled={loading}
                                    >
                                        {loading ? <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></span> : <span className="material-symbols-outlined">save</span>}
                                        Lưu cấu hình hồ sơ
                                    </button>
                                </div>
                            </form>
                        )}

                        {activeTab === 'password' && (
                            <form onSubmit={handleChangePassword} className="space-y-8">
                                <div>
                                   <h3 className="text-xl font-black text-on-surface uppercase tracking-tight font-headline">Xác thực & Bảo mật</h3>
                                   <p className="text-[10px] text-outline font-bold mt-1 uppercase tracking-widest">Thay đổi định kỳ để đảm bảo an toàn cho dữ liệu</p>
                                </div>

                                <div className="space-y-1.5">
                                    <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Mật khẩu hiện tại</label>
                                    <div className="relative group">
                                       <span className="material-symbols-outlined absolute left-6 top-1/2 -translate-y-1/2 text-outline group-focus-within:text-error transition-colors">lock_open</span>
                                       <input 
                                          type="password" 
                                          className="w-full pl-16 pr-6 py-4 bg-surface-container-low border-transparent focus:border-error focus:ring-0 focus:bg-white rounded-2xl text-sm font-black transition-all duration-300 outline-none" 
                                          value={oldPassword}
                                          onChange={(e) => setOldPassword(e.target.value)}
                                          required
                                          placeholder="••••••••"
                                       />
                                    </div>
                                </div>

                                <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                                   <div className="space-y-1.5">
                                       <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Mật khẩu mới</label>
                                       <input 
                                          type="password" 
                                          className="w-full px-6 py-4 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-2xl text-sm font-black transition-all duration-300 outline-none" 
                                          value={newPassword}
                                          onChange={(e) => setNewPassword(e.target.value)}
                                          required
                                          placeholder="Tối thiểu 6 ký tự"
                                       />
                                   </div>
                                   <div className="space-y-1.5">
                                       <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Xác nhận lại</label>
                                       <input 
                                          type="password" 
                                          className="w-full px-6 py-4 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-2xl text-sm font-black transition-all duration-300 outline-none" 
                                          value={confirmPassword}
                                          onChange={(e) => setConfirmPassword(e.target.value)}
                                          required
                                          placeholder="Nhập lại mật khẩu mới"
                                       />
                                   </div>
                                </div>

                                <div className="p-6 bg-primary/[0.03] rounded-2xl border border-primary/10 flex items-center gap-4">
                                   <div className="w-10 h-10 bg-primary/10 rounded-xl text-primary flex items-center justify-center shrink-0">
                                      <span className="material-symbols-outlined">shield</span>
                                   </div>
                                   <p className="text-[11px] font-bold text-on-surface-variant opacity-70">Lưu ý: Bạn sẽ không cần đăng nhập lại sau khi đổi mật khẩu nếu phiên làm việc hiện tại vẫn còn hiệu lực.</p>
                                </div>

                                <div className="pt-6 border-t border-outline-variant/5 flex justify-end">
                                    <button 
                                       type="submit" 
                                       className="px-10 py-4 bg-primary text-white rounded-2xl font-black text-xs uppercase tracking-widest shadow-xl shadow-primary/20 hover:scale-105 active:scale-95 transition-all flex items-center gap-3 disabled:opacity-50" 
                                       disabled={loading}
                                    >
                                        {loading ? <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></span> : <span className="material-symbols-outlined">key</span>}
                                        Cập nhật mật khẩu mới
                                    </button>
                                </div>
                            </form>
                        )}
                    </div>
                </main>
            </div>
        </div>
    );
}
