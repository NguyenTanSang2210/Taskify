import React, { useCallback, useState } from 'react';
import { Link } from 'react-router-dom';
import { dashboardApi } from '../api/dashboardApi';
import InlineNotice from '../components/InlineNotice';
import { useAuth } from '../context/AuthContext';
import useLoadEffect from '../hooks/useLoadEffect';
import LecturerSection from './dashboard/LecturerSection';
import OverviewSection from './dashboard/OverviewSection';
import StudentSection from './dashboard/StudentSection';

const ROLE_TITLES = {
    ADMIN: 'Bảng điều khiển quản trị',
    DEPARTMENT_ADMIN: 'Bảng điều khiển khoa',
    LECTURER: 'Bảng điều khiển giảng viên',
    STUDENT: 'Bảng điều khiển sinh viên',
};

const getGreeting = () => {
    const hour = new Date().getHours();
    if (hour < 12) return 'Chào buổi sáng';
    if (hour < 18) return 'Chào buổi chiều';
    return 'Chào buổi tối';
};

/** Trang tổng quan theo vai trò (P4-01): số liệu thật — không còn khối giả lập "Tiến độ 0%" / "Gợi ý từ AI". */
export default function DashboardPage() {
    const { user } = useAuth();
    const roleName = typeof user?.role === 'object' && user?.role ? user.role.name : user?.role;
    const personal = roleName === 'LECTURER' || roleName === 'STUDENT';

    const [stats, setStats] = useState(null);
    const [error, setError] = useState(null);

    const load = useCallback(async () => {
        if (!personal) return;
        try {
            setStats(roleName === 'LECTURER' ? await dashboardApi.getLecturerStats() : await dashboardApi.getStudentStats());
        } catch (e) {
            setError(e.message || 'Không thể tải dữ liệu thống kê');
        }
    }, [personal, roleName]);
    useLoadEffect(load);

    if (!user) return null;

    return (
        <div className="space-y-8 animate-in fade-in duration-700">
            {/* Hero Section */}
            <div className="relative overflow-hidden rounded-[2rem] bg-[#00152a] p-8 md:p-12 shadow-2xl text-white">
                <div className="absolute top-0 right-0 w-1/3 h-full opacity-10 pointer-events-none">
                    <div className="bg-gradient-to-br from-white/20 to-transparent w-full h-full transform skew-x-12"></div>
                </div>
                <div className="relative z-10">
                    <span className="inline-block px-3 py-1 rounded-full bg-primary-container/20 text-blue-200 text-[10px] font-black uppercase tracking-[0.2em] border border-white/10 mb-6">
                        {ROLE_TITLES[roleName] || "Tổng quan"}
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
                        {roleName === 'DEPARTMENT_ADMIN' && <Link to="/department-admin" className="px-6 py-2.5 bg-white text-[#00152a] rounded-xl font-black text-xs uppercase tracking-widest hover:scale-105 active:scale-95 transition-all shadow-xl">Quản trị khoa</Link>}
                        {(roleName === 'STUDENT' || roleName === 'LECTURER') && <Link to="/project-space" className="px-6 py-2.5 bg-white/10 text-white border border-white/20 rounded-xl font-black text-xs uppercase tracking-widest hover:bg-white/20 active:scale-95 transition-all">Không gian làm việc</Link>}
                    </div>
                </div>
                {/* Decoration */}
                <div className="absolute -bottom-12 -right-12 w-64 h-64 bg-secondary/20 rounded-full blur-[80px]"></div>
            </div>


            {error && <InlineNotice type="danger" message={error} onClose={() => setError(null)} />}

            {(roleName === 'ADMIN' || roleName === 'DEPARTMENT_ADMIN') && <OverviewSection isAdmin={roleName === 'ADMIN'} />}
            {personal && !stats && !error && (
                <p className="text-center text-xs font-bold uppercase tracking-widest text-outline animate-pulse">Đang tải số liệu...</p>
            )}
            {roleName === 'LECTURER' && stats && <LecturerSection stats={stats} />}
            {roleName === 'STUDENT' && stats && <StudentSection stats={stats} />}

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

        </div>
    );
}
