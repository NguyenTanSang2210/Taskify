import React, { useEffect, useState } from 'react';
import { dashboardApi } from '../api/dashboardApi';
import InlineNotice from '../components/InlineNotice';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function DashboardPage() {
    const { user } = useAuth();
    const roleName = typeof user?.role === 'object' && user?.role ? user.role.name : user?.role;

    const [stats, setStats] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        if (!user) return;
        const fetchStats = async () => {
            try {
                let data;
                if (roleName === 'ADMIN') {
                    data = await dashboardApi.getAdminStats();
                } else if (roleName === 'LECTURER') {
                    data = await dashboardApi.getLecturerStats();
                }
                setStats(data);
            } catch {
                setError('Không thể tải dữ liệu thống kê');
            } finally {
                setLoading(false);
            }
        };
        fetchStats();
    }, [user, roleName]);

    if (!user) return (
        <div className="flex flex-col items-center justify-center min-h-[60vh] text-center p-8 bg-white dark:bg-slate-900 rounded-3xl border border-outline-variant/10 shadow-sm">
            <span className="material-symbols-outlined text-outline text-6xl mb-4 animate-bounce">login</span>
            <h2 className="text-2xl font-black text-primary uppercase tracking-widest">Vui lòng đăng nhập</h2>
            <p className="text-outline font-bold mt-2">Bạn cần truy cập tài khoản để xem bảng điều khiển.</p>
            <Link to="/login" className="mt-6 px-8 py-3 bg-primary text-white rounded-xl font-black uppercase tracking-widest shadow-lg shadow-primary/20 hover:scale-105 active:scale-95 transition-all">Đến trang đăng nhập</Link>
        </div>
    );

    const getGreeting = () => {
        const hour = new Date().getHours();
        if (hour < 12) return 'Chào buổi sáng';
        if (hour < 18) return 'Chào buổi chiều';
        return 'Chào buổi tối';
    };

    const statCards = roleName === 'ADMIN' && stats
        ? [
            { label: 'Người dùng', value: stats.totalUsers, icon: 'groups', color: 'text-primary', bg: 'bg-primary/10', copy: 'Tổng số tài khoản' },
            { label: 'Đề tài', value: stats.totalTopics, icon: 'topic', color: 'text-secondary', bg: 'bg-secondary/10', copy: 'Đề tài trong hệ thống' },
            { label: 'Đang mở', value: stats.openTopics, icon: 'door_open', color: 'text-tertiary', bg: 'bg-tertiary/10', copy: 'Sẵn sàng đăng ký' },
            { label: 'Sinh viên', value: stats.totalStudents, icon: 'school', color: 'text-primary', bg: 'bg-primary/10', copy: 'Sinh viên các khoa' },
        ]
        : roleName === 'LECTURER' && stats
            ? [
                { label: 'Đề tài của tôi', value: stats.myTopics, icon: 'menu_book', color: 'text-primary', bg: 'bg-primary/10', copy: 'Đang phụ trách quản lý' },
                { label: 'Sinh viên hướng dẫn', value: stats.totalStudents || 0, icon: 'group', color: 'text-secondary', bg: 'bg-secondary/10', copy: 'Đang thực hiện đề tài' }
            ]
            : [];

    return (
        <div className="space-y-8 animate-in fade-in duration-700">
            {/* Hero Section */}
            <div className="relative overflow-hidden rounded-[2rem] bg-[#00152a] p-8 md:p-12 shadow-2xl text-white">
                <div className="absolute top-0 right-0 w-1/3 h-full opacity-10 pointer-events-none">
                    <div className="bg-gradient-to-br from-white/20 to-transparent w-full h-full transform skew-x-12"></div>
                </div>
                <div className="relative z-10">
                    <span className="inline-block px-3 py-1 rounded-full bg-primary-container/20 text-blue-200 text-[10px] font-black uppercase tracking-[0.2em] border border-white/10 mb-6">
                        {roleName} DASHBOARD
                    </span>
                    <h1 className="text-3xl md:text-5xl font-black font-headline tracking-tighter mb-4 leading-none">
                        {getGreeting()}, <span className="text-secondary-fixed">{user.fullName || 'bạn'}</span>!
                    </h1>
                    <p className="text-blue-100/70 text-sm md:text-base max-w-2xl font-medium leading-relaxed">
                        Chào mừng bạn trở lại với trung tâm điều khiển học thuật. Theo dõi tiến độ, quản lý đề tài và nhận các cập nhật quan trọng ngay tại đây.
                    </p>
                    <div className="flex flex-wrap gap-4 mt-10">
                        {roleName === 'ADMIN' && <Link to="/admin" className="px-6 py-2.5 bg-white text-[#00152a] rounded-xl font-black text-xs uppercase tracking-widest hover:scale-105 active:scale-95 transition-all shadow-xl">Quản trị hệ thống</Link>}
                        {roleName === 'LECTURER' && <Link to="/lecturer-topics" className="px-6 py-2.5 bg-white text-[#00152a] rounded-xl font-black text-xs uppercase tracking-widest hover:scale-105 active:scale-95 transition-all shadow-xl">Quản lý đề tài</Link>}
                        {(roleName === 'STUDENT' || roleName === 'LECTURER') && <Link to="/project-space" className="px-6 py-2.5 bg-white/10 text-white border border-white/20 rounded-xl font-black text-xs uppercase tracking-widest hover:bg-white/20 active:scale-95 transition-all">Không gian làm việc</Link>}
                    </div>
                </div>
                {/* Decoration */}
                <div className="absolute -bottom-12 -right-12 w-64 h-64 bg-secondary/20 rounded-full blur-[80px]"></div>
            </div>

            {error && (
                <div className="mx-auto max-w-4xl">
                    <InlineNotice type="danger" message={error} onClose={() => setError(null)} />
                </div>
            )}

            {/* Metrics Bento Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                {statCards.map((card, idx) => (
                    <div 
                        key={card.label} 
                        className="group bg-white dark:bg-slate-900 p-6 rounded-[1.5rem] shadow-[0_8px_30px_rgb(0,0,0,0.04)] border border-outline-variant/10 hover:shadow-[0_8px_30px_rgb(0,0,0,0.08)] hover:-translate-y-1 transition-all duration-300"
                        style={{ animationDelay: `${idx * 100}ms` }}
                    >
                        <div className="flex justify-between items-start mb-4">
                            <div className={`${card.bg} ${card.color} w-10 h-10 rounded-xl flex items-center justify-center shrink-0`}>
                                <span className="material-symbols-outlined">{card.icon}</span>
                            </div>
                            <span className="material-symbols-outlined text-outline/30 group-hover:text-primary transition-colors">trending_up</span>
                        </div>
                        <p className="text-[10px] font-black uppercase tracking-widest text-outline">{card.label}</p>
                        <h3 className="text-2xl font-black text-on-surface mt-1 group-hover:text-primary transition-colors">{card.value ?? 0}</h3>
                        <p className="text-[10px] text-outline font-bold mt-2 uppercase tracking-tight">{card.copy}</p>
                    </div>
                ))}
            </div>

            {/* Special Section for Students (if no stats) */}
            {roleName === 'STUDENT' && !statCards.length && (
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                    <div className="lg:col-span-2 bg-white dark:bg-slate-900 p-8 rounded-[2rem] border border-outline-variant/10 shadow-sm flex flex-col md:flex-row gap-8 items-center">
                        <div className="w-full md:w-1/3 aspect-square rounded-2xl bg-secondary-container/30 flex flex-col items-center justify-center text-secondary relative overflow-hidden group">
                           <span className="material-symbols-outlined text-6xl group-hover:scale-110 transition-transform duration-500">architecture</span>
                           <div className="mt-4 text-[10px] font-black uppercase tracking-widest">Tiến độ 0%</div>
                           <div className="absolute inset-x-0 bottom-0 h-1.5 bg-surface-container">
                              <div className="h-full bg-secondary w-[5%]"></div>
                           </div>
                        </div>
                        <div className="flex-1 space-y-4 text-center md:text-left">
                            <h2 className="text-2xl font-black text-on-surface uppercase tracking-tight leading-none font-headline">Bạn chưa tham gia đề tài?</h2>
                            <p className="text-sm text-on-surface-variant font-medium leading-relaxed">
                                Hãy khám phá danh sách đề tài nghiên cứu từ các giảng viên để bắt đầu hành trình học thuật của mình.
                            </p>
                            <div className="flex flex-wrap justify-center md:justify-start gap-4 pt-4">
                                <Link to="/" className="px-8 py-3 bg-primary text-white rounded-xl font-black text-xs uppercase tracking-widest shadow-xl shadow-primary/20 hover:scale-105 active:scale-95 transition-all">Xem danh sách đề tài</Link>
                                <Link to="/notifications" className="px-8 py-3 bg-surface-container text-on-surface-variant rounded-xl font-black text-xs uppercase tracking-widest hover:bg-surface-container-high transition-all">Xem thông báo</Link>
                            </div>
                        </div>
                    </div>
                    <div className="bg-gradient-to-br from-tertiary-container to-secondary-container p-8 rounded-[2rem] flex flex-col justify-center text-center">
                        <span className="material-symbols-outlined text-4xl text-on-tertiary-container mb-4">auto_awesome</span>
                        <h3 className="text-lg font-black text-on-tertiary-container uppercase tracking-widest">Gợi ý từ AI</h3>
                        <p className="text-xs text-on-tertiary-container/80 mt-2 font-bold leading-relaxed">
                            Hoàn thiện hồ sơ cá nhân để nhận được các gợi ý đề tài phù hợp với năng lực của bạn.
                        </p>
                        <Link to="/profile" className="mt-6 text-[10px] font-black uppercase tracking-widest text-on-tertiary-container hover:underline">Cập nhật hồ sơ →</Link>
                    </div>
                </div>
            )}

            {/* Quick Actions Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {/* Card 1 */}
                <div className="group bg-white dark:bg-slate-900 p-6 rounded-3xl border border-outline-variant/10 shadow-sm hover:shadow-xl transition-all">
                    <div className="flex items-center gap-4 mb-6">
                       <div className="w-12 h-12 rounded-2xl bg-primary/5 text-primary flex items-center justify-center group-hover:bg-primary group-hover:text-white transition-all duration-500">
                          <span className="material-symbols-outlined text-2xl">rocket_launch</span>
                       </div>
                       <div>
                          <h3 className="text-sm font-black text-on-surface uppercase tracking-widest">Học tập & Nghiên cứu</h3>
                          <p className="text-[10px] text-outline font-bold mt-0.5">Tiếp tục các dự án dang dở</p>
                       </div>
                    </div>
                    <div className="flex flex-col gap-2">
                       <Link to="/" className="w-full flex justify-between items-center p-3 rounded-2xl bg-surface-container-low hover:bg-primary-container hover:text-white transition-all">
                          <span className="text-[11px] font-black uppercase tracking-widest">Danh sách đề tài</span>
                          <span className="material-symbols-outlined text-sm">arrow_forward</span>
                       </Link>
                       <Link to="/progress" className="w-full flex justify-between items-center p-3 rounded-2xl bg-surface-container-low hover:bg-primary-container hover:text-white transition-all">
                          <span className="text-[11px] font-black uppercase tracking-widest">Theo dõi tiến độ</span>
                          <span className="material-symbols-outlined text-sm">arrow_forward</span>
                       </Link>
                    </div>
                </div>

                {/* Card 2 */}
                <div className="group bg-white dark:bg-slate-900 p-6 rounded-3xl border border-outline-variant/10 shadow-sm hover:shadow-xl transition-all">
                    <div className="flex items-center gap-4 mb-6">
                       <div className="w-12 h-12 rounded-2xl bg-secondary/5 text-secondary flex items-center justify-center group-hover:bg-secondary group-hover:text-white transition-all duration-500">
                          <span className="material-symbols-outlined text-2xl">forum</span>
                       </div>
                       <div>
                          <h3 className="text-sm font-black text-on-surface uppercase tracking-widest">Trao đổi & Hợp tác</h3>
                          <p className="text-[10px] text-outline font-bold mt-0.5">Kết nối với giảng viên & đồng đội</p>
                       </div>
                    </div>
                    <div className="flex flex-col gap-2">
                       <Link to="/project-space" className="w-full flex justify-between items-center p-3 rounded-2xl bg-surface-container-low hover:bg-secondary-container hover:text-on-secondary-container transition-all">
                          <span className="text-[11px] font-black uppercase tracking-widest">Workspace</span>
                          <span className="material-symbols-outlined text-sm">arrow_forward</span>
                       </Link>
                       <Link to="/project-chat" className="w-full flex justify-between items-center p-3 rounded-2xl bg-surface-container-low hover:bg-secondary-container hover:text-on-secondary-container transition-all">
                          <span className="text-[11px] font-black uppercase tracking-widest">Kênh thảo luận</span>
                          <span className="material-symbols-outlined text-sm">arrow_forward</span>
                       </Link>
                    </div>
                </div>

                {/* Card 3 - Contextual */}
                <div className="group bg-white dark:bg-slate-900 p-6 rounded-3xl border border-outline-variant/10 shadow-sm hover:shadow-xl transition-all">
                    <div className="flex items-center gap-4 mb-6">
                       <div className="w-12 h-12 rounded-2xl bg-tertiary/5 text-tertiary flex items-center justify-center group-hover:bg-tertiary group-hover:text-white transition-all duration-500">
                          <span className="material-symbols-outlined text-2xl">account_circle</span>
                       </div>
                       <div>
                          <h3 className="text-sm font-black text-on-surface uppercase tracking-widest">Quản lý cá nhân</h3>
                          <p className="text-[10px] text-outline font-bold mt-0.5">Tùy chỉnh trải nghiệm của bạn</p>
                       </div>
                    </div>
                    <div className="flex flex-col gap-2">
                       <Link to="/profile" className="w-full flex justify-between items-center p-3 rounded-2xl bg-surface-container-low hover:bg-tertiary-container hover:text-on-tertiary-container transition-all">
                          <span className="text-[11px] font-black uppercase tracking-widest">Hồ sơ cá nhân</span>
                          <span className="material-symbols-outlined text-sm">arrow_forward</span>
                       </Link>
                       <Link to="/notifications" className="w-full flex justify-between items-center p-3 rounded-2xl bg-surface-container-low hover:bg-tertiary-container hover:text-on-tertiary-container transition-all">
                          <span className="text-[11px] font-black uppercase tracking-widest">Trung tâm thông báo</span>
                          <span className="material-symbols-outlined text-sm">arrow_forward</span>
                       </Link>
                    </div>
                </div>
            </div>

            {/* Role-specific bottom sections */}
            {roleName === 'ADMIN' && stats && (
              <div className="bg-primary/5 p-8 rounded-[2rem] border border-primary/10 flex flex-col md:flex-row justify-between items-center gap-6">
                 <div>
                    <h2 className="text-xl font-black text-primary uppercase tracking-widest">Quản trị viên</h2>
                    <p className="text-xs text-outline font-bold mt-1 uppercase tracking-tight">Quyền truy cập toàn diện vào hệ thống dữ liệu và quản lý người dùng.</p>
                 </div>
                 <div className="flex gap-4">
                    <Link to="/admin" className="px-6 py-2.5 bg-primary text-white rounded-xl font-black text-[10px] uppercase tracking-widest shadow-lg shadow-primary/20">Quản lý người dùng</Link>
                    <Link to="/department-admin" className="px-6 py-2.5 bg-surface-container text-on-surface-variant rounded-xl font-black text-[10px] uppercase tracking-widest">Quản lý khoa</Link>
                 </div>
              </div>
            )}

            {roleName === 'LECTURER' && stats && (
              <div className="bg-secondary/5 p-8 rounded-[2rem] border border-secondary/10 flex flex-col md:flex-row justify-between items-center gap-6">
                 <div>
                    <h2 className="text-xl font-black text-secondary uppercase tracking-widest">Lối tắt giảng viên</h2>
                    <p className="text-xs text-outline font-bold mt-1 uppercase tracking-tight">Duyệt đăng ký, phản hồi tiến độ và quản lý danh sách đề tài của bạn.</p>
                 </div>
                 <div className="flex gap-4">
                    <Link to="/lecturer" className="px-6 py-2.5 bg-secondary text-white rounded-xl font-black text-[10px] uppercase tracking-widest shadow-lg shadow-secondary/20">Duyệt đăng ký</Link>
                    <Link to="/lecturer-progress" className="px-6 py-2.5 bg-surface-container text-on-surface-variant rounded-xl font-black text-[10px] uppercase tracking-widest">Nhận xét tiến độ</Link>
                 </div>
              </div>
            )}
        </div>
    );
}