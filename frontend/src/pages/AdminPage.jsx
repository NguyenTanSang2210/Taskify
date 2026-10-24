import { Link } from "react-router-dom";
import InlineNotice from '../components/InlineNotice';
import UserImportPanel from '../components/userImport/UserImportPanel';
import GradeRevisionPanel from '../components/GradeRevisionPanel';
import { useAdmin } from './admin/useAdmin';
import CreateUserTab from './admin/CreateUserTab';
import UserListTab from './admin/UserListTab';
import DepartmentsTab from './admin/DepartmentsTab';

export default function AdminPage() {
    const d = useAdmin();
    const { loading, activeTab, setActiveTab, notice, setNotice, roles } = d;

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
                     className="px-6 py-2.5 bg-primary/10 text-primary border border-primary/20 rounded-xl shadow-sm text-[10px] font-black uppercase tracking-widest flex items-center gap-2 hover:bg-primary hover:text-on-primary transition-all"
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
                    { id: 'departments', label: 'Quản lý Khoa', icon: 'account_tree' },
                    { id: 'importUsers', label: 'Nhập từ Excel', icon: 'upload_file' },
                    { id: 'gradeRevisions', label: 'Yêu cầu chấm lại', icon: 'lock_reset' }
                ].map(tab => (
                    <button 
                        key={tab.id}
                        onClick={() => setActiveTab(tab.id)}
                        className={`flex items-center gap-3 px-8 py-3.5 rounded-[1.5rem] text-[10px] font-black uppercase tracking-widest transition-all duration-300 ${
                            activeTab === tab.id 
                            ? "bg-primary text-on-primary shadow-lg shadow-primary/20" 
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
                    <div className="py-32 flex flex-col items-center justify-center text-outline [&_.material-symbols-outlined]:opacity-40 select-none">
                        <div className="animate-spin rounded-full h-10 w-10 border-4 border-primary border-t-transparent mb-4"></div>
                        <p className="text-[10px] font-black uppercase tracking-widest">Đang tải dữ liệu cấu hình...</p>
                    </div>
                )}

                {activeTab === 'createUser' && !loading && <CreateUserTab {...d} />}

                {activeTab === 'userList' && <UserListTab {...d} />}

                {activeTab === 'importUsers' && <UserImportPanel departments={d.departments} />}

                {activeTab === 'gradeRevisions' && <GradeRevisionPanel />}

                {activeTab === 'departments' && <DepartmentsTab {...d} />}
            </div>
            
            {/* System Tip */}
            <div className="bg-primary/5 p-6 rounded-[2rem] border border-primary/10 flex items-center gap-4">
               <div className="w-10 h-10 rounded-xl bg-primary text-on-primary flex items-center justify-center shrink-0">
                  <span className="material-symbols-outlined">analytics</span>
               </div>
               <p className="text-xs font-bold text-on-surface-variant leading-relaxed">
                  <span className="font-black text-primary mr-2 uppercase tracking-widest">Thông số quản trị:</span>
                  Để trống mật khẩu khi tạo tài khoản: người dùng nhận mã kích hoạt qua email và tự đặt mật khẩu. Nếu đặt mật khẩu tạm, người dùng bắt buộc đổi ở lần đăng nhập đầu tiên. Nhập hàng loạt: tab "Nhập từ Excel".
               </p>
            </div>
        </div>
    );
}
