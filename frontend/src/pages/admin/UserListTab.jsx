import Pagination from '../../components/Pagination';
import { getRoleLabel } from './helpers';

/** Tab "userList" của AdminPage (tách ở P3-11; dữ liệu + xử lý ở useAdmin). */
export default function UserListTab({ loading, users, userPage, setUserPage, userPageInfo, setUserQuery, userQueryInput, setUserQueryInput, roles, departments, selectedDepartmentId, setSelectedDepartmentId, selectedUserRole, setSelectedUserRole, loadUsers, toggleUserStatus, changeUserRole, resendActivation }) {
    return (
                    <div className="p-8 space-y-8 animate-in fade-in duration-500">
                        <div className="flex flex-col md:flex-row gap-6 items-end bg-surface-container-low/20 p-6 rounded-[2rem]">
                            <div className="flex-grow space-y-1.5">
                               <label htmlFor="userlisttab-select-11" className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Lọc theo Khoa/Viện</label>
                               <select id="userlisttab-select-11" className="w-full px-4 py-3 bg-white dark:bg-slate-900 border-transparent focus:border-primary focus:ring-0 rounded-xl text-sm font-black transition-all outline-none" value={selectedDepartmentId} onChange={(e) => { setSelectedDepartmentId(e.target.value); setUserPage(0); }}>
                                  <option value="">Chọn khoa...</option>
                                  {departments.map(d => <option key={d.id} value={String(d.id)}>{d.code} - {d.name}</option>)}
                               </select>
                            </div>
                            <div className="flex-grow space-y-1.5">
                               <label htmlFor="userlisttab-select-18" className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Lọc theo vai trò</label>
                               <select id="userlisttab-select-18" className="w-full px-4 py-3 bg-white dark:bg-slate-900 border-transparent focus:border-primary focus:ring-0 rounded-xl text-xs font-black uppercase tracking-widest outline-none" value={selectedUserRole} onChange={(e) => { setSelectedUserRole(e.target.value); setUserPage(0); }}>
                                  <option value="">Tất cả vai trò</option>
                                  {roles.map(r => <option key={r.id} value={r.name}>{getRoleLabel(r.name)}</option>)}
                               </select>
                            </div>
                            <form className="flex-grow space-y-1.5" onSubmit={(e) => { e.preventDefault(); setUserQuery(userQueryInput.trim()); setUserPage(0); }}>
                               <label htmlFor="userlisttab-input-25" className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Tìm kiếm</label>
                               <input id="userlisttab-input-25" className="w-full px-4 py-3 bg-white dark:bg-slate-900 border-transparent focus:border-primary focus:ring-0 rounded-xl text-sm font-bold transition-all outline-none" value={userQueryInput} onChange={(e) => setUserQueryInput(e.target.value)} placeholder="Tên đăng nhập, họ tên, email… (Enter)" />
                            </form>
                            <button onClick={loadUsers} className="px-8 py-3 bg-white dark:bg-slate-900 border border-outline-variant/20 rounded-xl text-[10px] font-black uppercase tracking-widest hover:bg-surface-container transition-all flex items-center gap-2">
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
                                               <p className="text-[10px] text-outline font-bold lowercase">{u.email || "No email"}</p>
                                            </td>
                                            <td className="px-6 py-5 max-w-[200px]">
                                               <span className="text-[11px] font-black text-secondary uppercase tracking-tighter leading-tight">
                                                  {u.department ? `${u.department.code} - ${u.department.name}` : <span className="text-outline italic">Hệ thống chung</span>}
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
                                               <div className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[9px] font-black uppercase tracking-widest ${u.active ? 'bg-green-500/10 text-green-700 dark:text-green-400' : 'bg-error/10 text-error'}`}>
                                                  <span className="w-1.5 h-1.5 rounded-full bg-current"></span>
                                                  {u.active ? 'Hoạt động' : 'Tạm khóa'}
                                               </div>
                                               {u.activated === false && (
                                                  <div className="mt-1 inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[9px] font-black uppercase tracking-widest bg-amber-500/10 text-amber-700 block w-fit">
                                                     Chưa kích hoạt
                                                  </div>
                                               )}
                                            </td>
                                            <td className="px-6 py-5 text-right">
                                               {u.activated === false && (
                                                 <button
                                                    onClick={() => resendActivation(u)}
                                                    className="p-2.5 mr-2 rounded-xl transition-all shadow-sm bg-amber-500/10 text-amber-700 hover:bg-amber-600 hover:text-white"
                                                    title="Gửi lại email kích hoạt"
                                                 >
                                                    <span className="material-symbols-outlined text-lg">forward_to_inbox</span>
                                                 </button>
                                               )}
                                               {u.role && u.role.name !== 'ADMIN' && (
                                                 <button 
                                                    onClick={() => toggleUserStatus(u)}
                                                    className={`p-2.5 rounded-xl transition-all shadow-sm ${u.active ? 'bg-error/5 text-error hover:bg-error hover:text-on-error' : 'bg-green-500/5 text-green-700 dark:text-green-400 hover:bg-green-600 hover:text-on-error'}`}
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
                                          <td colSpan="6" className="py-32 text-center text-outline [&_.material-symbols-outlined]:opacity-40 select-none">
                                             <span className="material-symbols-outlined text-5xl mb-4">search_off</span>
                                             <p className="text-[10px] font-black uppercase tracking-widest leading-relaxed">Không tìm thấy thành viên nào<br/>khớp với tiêu chí lọc.</p>
                                          </td>
                                       </tr>
                                    )}
                                </tbody>
                            </table>
                        </div>
                        <Pagination page={userPage} totalPages={userPageInfo.totalPages} totalElements={userPageInfo.totalElements}
                                    onChange={setUserPage} disabled={loading} />
                    </div>
                
    );
}
