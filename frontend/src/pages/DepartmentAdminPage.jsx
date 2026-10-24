import InlineNotice from '../components/InlineNotice';
import GradeRevisionPanel from '../components/GradeRevisionPanel';
import UserImportPanel from '../components/userImport/UserImportPanel';
import { useDepartmentAdmin } from './departmentAdmin/useDepartmentAdmin';
import WorkspacesTab from './departmentAdmin/WorkspacesTab';
import ClassesTab from './departmentAdmin/ClassesTab';
import WorkspaceClassesTab from './departmentAdmin/WorkspaceClassesTab';
import AssignmentsTab from './departmentAdmin/AssignmentsTab';
import TopicsTab from './departmentAdmin/TopicsTab';
import StatsTab from './departmentAdmin/StatsTab';

export default function DepartmentAdminPage() {
    const d = useDepartmentAdmin();
    const { activeTab, setActiveTab, loading, notice, setNotice, loadDeptStats } = d;

    return (
        <div className="space-y-8 animate-in fade-in duration-500 pb-12">
            {/* Header Area */}
            <div className="flex flex-col md:flex-row md:items-end justify-between gap-6 shrink-0">
                <div>
                  <span className="text-[10px] font-black uppercase tracking-[0.2em] text-primary mb-2 block">Cấp quản lý đơn vị</span>
                  <h1 className="text-3xl font-black text-on-surface tracking-tight font-headline uppercase leading-none">Quản trị Khoa</h1>
                  <p className="text-sm text-outline mt-2 font-medium max-w-2xl">Điều phối không gian học thuật, phân bổ lớp sinh viên và giảng viên phụ trách theo chu kỳ học kỳ.</p>
                </div>
                
                <div className="flex items-center gap-3">
                   <button 
                     onClick={loadDeptStats}
                     className="px-6 py-3 bg-white dark:bg-slate-900 border border-outline-variant/10 rounded-xl shadow-sm text-[10px] font-black uppercase tracking-widest flex items-center gap-2 hover:bg-surface-container transition-all"
                   >
                      <span className="material-symbols-outlined text-sm">analytics</span>
                      Thống kê nhanh
                   </button>
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
            <div className="bg-white dark:bg-slate-900 p-2 rounded-[2rem] border border-outline-variant/10 shadow-sm flex flex-wrap gap-1 sticky top-0 z-20">
                {[
                    { id: 'workspaces', label: 'Không gian (Workspace)', icon: 'auto_mode' },
                    { id: 'classes', label: 'Danh mục Lớp', icon: 'school' },
                    { id: 'workspaceClasses', label: 'Phân bổ Lớp', icon: 'hub' },
                    { id: 'assignments', label: 'Giảng viên & Quota', icon: 'assignment_ind' },
                    { id: 'topics', label: 'Duyệt Đề tài', icon: 'inventory_2' },
                    { id: 'importUsers', label: 'Nhập từ Excel', icon: 'upload_file' },
                    { id: 'gradeRevisions', label: 'Yêu cầu chấm lại', icon: 'lock_reset' },
                    { id: 'stats', label: 'Báo cáo số liệu', icon: 'bar_chart' }
                ].map(tab => (
                    <button 
                        key={tab.id}
                        onClick={() => setActiveTab(tab.id)}
                        className={`flex items-center gap-2.5 px-6 py-3 rounded-[1.5rem] text-[9px] font-black uppercase tracking-widest transition-all duration-300 ${
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

            {/* Main Content Rendering Layer */}
            <div className="bg-white dark:bg-slate-900 rounded-[2.5rem] border border-outline-variant/10 shadow-sm overflow-hidden min-h-[500px]">
                {loading && activeTab !== 'stats' && activeTab !== 'gradeRevisions' && activeTab !== 'importUsers' && (
                    <div className="py-32 flex flex-col items-center justify-center text-outline [&_.material-symbols-outlined]:opacity-40 select-none">
                        <div className="animate-spin rounded-full h-10 w-10 border-4 border-primary border-t-transparent mb-4"></div>
                        <p className="text-[10px] font-black uppercase tracking-widest">Đang trích xuất dữ liệu...</p>
                    </div>
                )}

                {activeTab === 'workspaces' && !loading && <WorkspacesTab {...d} />}

                {activeTab === 'classes' && !loading && <ClassesTab {...d} />}

                {activeTab === 'workspaceClasses' && !loading && <WorkspaceClassesTab {...d} />}

                {activeTab === 'assignments' && !loading && <AssignmentsTab {...d} />}

                {activeTab === 'topics' && !loading && <TopicsTab {...d} />}

                {activeTab === 'importUsers' && <UserImportPanel />}

                {activeTab === 'gradeRevisions' && <GradeRevisionPanel />}

                {activeTab === 'stats' && !loading && <StatsTab {...d} />}
            </div>
            
            {/* Logic Helper Tip */}
            <div className="p-6 bg-surface-container-low/50 rounded-[2rem] border border-outline-variant/5 flex items-center gap-4">
               <span className="material-symbols-outlined text-outline">help</span>
               <p className="text-xs font-bold text-outline uppercase tracking-tight leading-relaxed">
                  Lưu ý: Không gian làm việc (Workspace) là trục xoay chính của hệ thống. Bạn cần gán Lớp và phân bổ Quota cho Giảng viên trước khi Chu kỳ đăng ký đề tài được bắt đầu.
               </p>
            </div>
        </div>
    );
}
