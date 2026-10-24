/** Tab "assignments" của DepartmentAdminPage (tách ở P3-11; dữ liệu + xử lý ở useDepartmentAdmin). */
export default function AssignmentsTab({ workspaces, selectedWorkspaceId, setSelectedWorkspaceId, lecturers, assignments, assignForm, setAssignForm, canConfigureSelectedWorkspace, loadAssignments, assignLecturer, revokeAssignment }) {
    return (
                    <div className="p-10 space-y-10 animate-in slide-in-from-bottom-4 duration-500">
                        <section className="bg-surface-container-low/40 p-8 rounded-[2rem] border border-outline-variant/10 flex flex-col md:flex-row items-center gap-8">
                             <div className="flex-grow space-y-1.5">
                                <label htmlFor="assignmentstab-select-8" className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Chọn Workspace Cấu hình</label>
                                <select id="assignmentstab-select-8" 
                                   className="w-full px-6 py-4 bg-white dark:bg-slate-900 border border-outline-variant/10 focus:border-primary focus:ring-0 rounded-2xl text-[11px] font-black uppercase tracking-widest outline-none cursor-pointer" 
                                   value={selectedWorkspaceId} 
                                   onChange={(e) => { setSelectedWorkspaceId(e.target.value); loadAssignments(Number(e.target.value)); }}
                                >
                                   <option value="">-- Tìm kiếm không gian làm việc --</option>
                                   {workspaces.map(w => <option key={w.id} value={w.id}>{w.name} (#{w.id})</option>)}
                                </select>
                             </div>
                        </section>

                        {selectedWorkspaceId && (
                           <div className="space-y-10">
                              <section className="bg-surface-container-low/30 p-8 rounded-[2.5rem] border border-outline-variant/5">
                                 <div className="flex items-center justify-between mb-8">
                                    <h3 className="text-lg font-black text-on-surface uppercase tracking-tight">Phân quyền & Đăng ký Quota Giảng viên</h3>
                                    <div className={`px-4 py-1.5 rounded-full text-[9px] font-black uppercase tracking-widest ${canConfigureSelectedWorkspace ? 'bg-primary/10 text-primary' : 'bg-error/10 text-error'}`}>
                                       {canConfigureSelectedWorkspace ? 'Có thể chỉnh sửa' : 'Đã khóa cấu hình'}
                                    </div>
                                 </div>
                                 
                                 <div className="grid grid-cols-1 md:grid-cols-12 gap-6 items-end">
                                    <div className="md:col-span-4 space-y-1.5">
                                       <label htmlFor="assignmentstab-select-32" className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Giảng viên phụ trách</label>
                                       <select id="assignmentstab-select-32" className="w-full px-5 py-3.5 bg-white dark:bg-slate-900 border border-outline-variant/10 focus:border-primary focus:ring-0 rounded-xl text-xs font-black uppercase tracking-widest outline-none cursor-pointer" value={assignForm.lecturerId} onChange={(e) => setAssignForm(s => ({ ...s, lecturerId: e.target.value }))} disabled={!canConfigureSelectedWorkspace}>
                                          <option value="">-- Chọn danh sách --</option>
                                          {lecturers.map(l => <option key={l.id} value={l.id}>{l.fullName} ({l.username})</option>)}
                                       </select>
                                    </div>
                                    <div className="md:col-span-2 space-y-1.5">
                                       <label htmlFor="assignmentstab-select-39" className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Vai trò</label>
                                       <select id="assignmentstab-select-39" className="w-full px-5 py-3.5 bg-white dark:bg-slate-900 border border-outline-variant/10 focus:border-primary focus:ring-0 rounded-xl text-[10px] font-black uppercase tracking-widest outline-none cursor-pointer" value={assignForm.type} onChange={(e) => setAssignForm(s => ({ ...s, type: e.target.value }))} disabled={!canConfigureSelectedWorkspace}>
                                          <option value="MAIN">HƯỚNG DẪN CHÍNH</option>
                                          <option value="ASSISTANT">TRỢ GIẢNG</option>
                                       </select>
                                    </div>
                                    <div className="md:col-span-2 space-y-1.5">
                                       <label htmlFor="assignmentstab-input-46" className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Hạn ngạch nhóm (Quota)</label>
                                       <input id="assignmentstab-input-46" type="number" className="w-full px-5 py-3.5 bg-white dark:bg-slate-900 border border-outline-variant/10 focus:border-primary focus:ring-0 rounded-xl text-sm font-black transition-all outline-none" value={assignForm.quotaMaxGroups} onChange={(e) => setAssignForm(s => ({ ...s, quotaMaxGroups: e.target.value }))} disabled={!canConfigureSelectedWorkspace} />
                                    </div>
                                    <div className="md:col-span-3 space-y-1.5">
                                       <label htmlFor="assignmentstab-input-50" className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Ghi chú phân bổ</label>
                                       <input id="assignmentstab-input-50" className="w-full px-5 py-3.5 bg-white dark:bg-slate-900 border border-outline-variant/10 focus:border-primary focus:ring-0 rounded-xl text-sm font-medium transition-all outline-none" value={assignForm.reason} onChange={(e) => setAssignForm(s => ({ ...s, reason: e.target.value }))} disabled={!canConfigureSelectedWorkspace} placeholder="..." />
                                    </div>
                                    <div className="md:col-span-1">
                                       <button aria-label="Phân công giảng viên" onClick={assignLecturer} disabled={!canConfigureSelectedWorkspace} className="w-full py-4 bg-primary text-on-primary rounded-xl font-black text-[10px] uppercase tracking-widest hover:scale-105 active:scale-95 transition-all disabled:opacity-20 flex items-center justify-center">
                                          <span className="material-symbols-outlined">key</span>
                                       </button>
                                    </div>
                                 </div>
                              </section>

                              <section className="space-y-6">
                                 <h3 className="text-sm font-black text-outline uppercase tracking-[0.2em] px-4">Nhân sự hiện hữu trong Workspace ({assignments.length})</h3>
                                 <div className="overflow-x-auto">
                                    <table className="w-full text-left border-separate border-spacing-y-2">
                                       <thead>
                                          <tr>
                                             <th className="px-8 py-3 text-[10px] uppercase tracking-widest font-black text-outline">Giảng viên</th>
                                             <th className="px-8 py-3 text-[10px] uppercase tracking-widest font-black text-outline">Học vị & Vai trò</th>
                                             <th className="px-8 py-3 text-[10px] uppercase tracking-widest font-black text-outline text-center">Quota</th>
                                             <th className="px-8 py-3 text-[10px] uppercase tracking-widest font-black text-outline text-right">Tình trạng & Thao tác</th>
                                          </tr>
                                       </thead>
                                       <tbody>
                                          {assignments.map(a => (
                                             <tr key={a.id} className="bg-surface-container-low/20 group hover:bg-white dark:hover:bg-slate-800 transition-all shadow-sm">
                                                <td className="px-8 py-4 rounded-l-2xl">
                                                   <div className="flex items-center gap-4">
                                                      <div className="w-10 h-10 rounded-full bg-secondary/10 text-secondary flex items-center justify-center font-black">
                                                         {a.lecturer?.user?.fullName?.charAt(0)}
                                                      </div>
                                                      <span className="text-sm font-black text-on-surface uppercase tracking-tight">{a.lecturer?.user?.fullName}</span>
                                                   </div>
                                                </td>
                                                <td className="px-8 py-4">
                                                   <span className={`text-[10px] font-black uppercase tracking-widest px-3 py-1 rounded-lg ${a.type === 'MAIN' ? 'bg-primary/10 text-primary' : 'bg-secondary/10 text-secondary'}`}>
                                                      {a.type}
                                                   </span>
                                                   <p className="text-[10px] font-bold text-outline uppercase mt-1">ID: #{a.lecturer?.id}</p>
                                                </td>
                                                <td className="px-8 py-4 text-center">
                                                   <span className="text-xl font-black text-primary">{a.quotaMaxGroups}</span>
                                                   <p className="text-[8px] font-black text-outline uppercase">Groups Max</p>
                                                </td>
                                                <td className="px-8 py-4 rounded-r-2xl text-right">
                                                   <div className="inline-flex items-center gap-4">
                                                      <div className={`px-2.5 py-1 rounded-md text-[8px] font-black uppercase tracking-widest ${a.active ? 'bg-green-500/10 text-green-700 dark:text-green-400' : 'bg-surface-container text-outline'}`}>
                                                         {a.active ? 'Hợp lệ' : 'Vô hiệu'}
                                                      </div>
                                                      {a.active && (
                                                         <button 
                                                            onClick={() => revokeAssignment(a.id)}
                                                            disabled={!canConfigureSelectedWorkspace}
                                                            className="w-10 h-10 rounded-xl bg-error/5 text-error hover:bg-error hover:text-on-error transition-all flex items-center justify-center disabled:opacity-20"
                                                         >
                                                            <span className="material-symbols-outlined text-lg">person_remove</span>
                                                         </button>
                                                      )}
                                                   </div>
                                                </td>
                                             </tr>
                                          ))}
                                          {assignments.length === 0 && (
                                             <tr>
                                                <td colSpan="4" className="py-20 text-center text-outline [&_.material-symbols-outlined]:opacity-40 select-none">
                                                   <p className="text-[10px] font-black uppercase tracking-widest">Không có giảng viên được phân công</p>
                                                </td>
                                             </tr>
                                          )}
                                       </tbody>
                                    </table>
                                 </div>
                              </section>
                           </div>
                        )}
                    </div>
                
    );
}
