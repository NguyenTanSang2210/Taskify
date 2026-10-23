import { getWorkspaceStatusLabel } from './helpers';

/** Tab "workspaceClasses" của DepartmentAdminPage (tách ở P3-11; dữ liệu + xử lý ở useDepartmentAdmin). */
export default function WorkspaceClassesTab({ workspaces, selectedWorkspaceId, setSelectedWorkspaceId, workspaceClasses, selectedWorkspace, canConfigureSelectedWorkspace, loadWorkspaceClasses, assignClassToWorkspace, unassignClassFromWorkspace, availableClassesToAssign }) {
    return (
                    <div className="p-10 space-y-10 animate-in slide-in-from-bottom-4 duration-500">
                        <section className="bg-surface-container-low/40 p-8 rounded-[2rem] border border-outline-variant/10 flex flex-col md:flex-row items-center gap-8">
                             <div className="flex-grow space-y-1.5">
                                <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Chọn đối tượng Workspace</label>
                                <select 
                                   className="w-full px-6 py-4 bg-white border border-outline-variant/10 focus:border-primary focus:ring-0 rounded-2xl text-[11px] font-black uppercase tracking-widest outline-none cursor-pointer" 
                                   value={selectedWorkspaceId} 
                                   onChange={(e) => { setSelectedWorkspaceId(e.target.value); loadWorkspaceClasses(Number(e.target.value)); }}
                                >
                                   <option value="">-- Click để chọn không gian làm việc --</option>
                                   {workspaces.map(w => <option key={w.id} value={w.id}>{w.name} (#{w.id})</option>)}
                                </select>
                             </div>
                             {selectedWorkspace && (
                                <div className="p-6 bg-primary/5 rounded-[1.5rem] border border-primary/10 min-w-[280px]">
                                   <span className="text-[9px] font-black uppercase tracking-widest text-primary block mb-1">Giai đoạn hiện tại</span>
                                   <p className="text-sm font-black text-on-surface uppercase tracking-tight">{getWorkspaceStatusLabel(selectedWorkspace.status)}</p>
                                </div>
                             )}
                        </section>

                        {selectedWorkspaceId ? (
                            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 items-start">
                                {/* Left: Assigned */}
                                <div className="space-y-6">
                                   <div className="flex items-center justify-between px-2">
                                      <h3 className="text-xs font-black uppercase tracking-widest text-outline">Lớp học thuộc Workspace ({workspaceClasses.length})</h3>
                                      <span className="material-symbols-outlined text-outline/40">link</span>
                                   </div>
                                   <div className="bg-surface-container-low/20 rounded-[2.5rem] border border-outline-variant/5 divide-y divide-outline-variant/5">
                                      {workspaceClasses.map(wc => (
                                         <div key={wc.id} className="p-6 flex items-center justify-between group hover:bg-white dark:hover:bg-slate-800 transition-all">
                                            <div className="flex items-center gap-4">
                                               <div className="w-10 h-10 rounded-xl bg-primary/10 text-primary flex items-center justify-center font-black text-xs">
                                                  {wc.academicClass?.code?.charAt(0)}
                                               </div>
                                               <div>
                                                  <h4 className="text-sm font-black text-on-surface uppercase tracking-tight">{wc.academicClass?.name}</h4>
                                                  <p className="text-[10px] font-bold text-outline uppercase">{wc.academicClass?.code}</p>
                                               </div>
                                            </div>
                                            <button 
                                               onClick={() => unassignClassFromWorkspace(Number(selectedWorkspaceId), wc.academicClass?.id)}
                                               disabled={!canConfigureSelectedWorkspace}
                                               className="w-10 h-10 rounded-xl bg-error/5 text-error hover:bg-error hover:text-white transition-all flex items-center justify-center disabled:opacity-20"
                                            >
                                               <span className="material-symbols-outlined text-lg">link_off</span>
                                            </button>
                                         </div>
                                      ))}
                                      {workspaceClasses.length === 0 && (
                                         <div className="p-12 text-center opacity-30">
                                            <p className="text-[10px] font-black uppercase tracking-widest">Không có lớp học nào liên kết</p>
                                         </div>
                                      )}
                                   </div>
                                </div>

                                {/* Right: Available */}
                                <div className="space-y-6">
                                   <div className="flex items-center justify-between px-2">
                                      <h3 className="text-xs font-black uppercase tracking-widest text-outline">Danh mục lớp có thể gán ({availableClassesToAssign.length})</h3>
                                      <span className="material-symbols-outlined text-outline/40">playlist_add</span>
                                   </div>
                                   <div className="bg-surface-container-low/20 rounded-[2.5rem] border border-outline-variant/5 divide-y divide-outline-variant/5">
                                      {availableClassesToAssign.map(c => (
                                         <div key={c.id} className="p-6 flex items-center justify-between group hover:bg-white dark:hover:bg-slate-800 transition-all">
                                            <div className="flex items-center gap-4">
                                               <div className="w-10 h-10 rounded-xl bg-surface-container text-outline flex items-center justify-center font-black text-xs">
                                                  {c.code?.charAt(0)}
                                               </div>
                                               <h4 className="text-sm font-black text-on-surface uppercase tracking-tight">{c.name}</h4>
                                            </div>
                                            <button 
                                               onClick={() => assignClassToWorkspace(Number(selectedWorkspaceId), c.id)}
                                               disabled={!canConfigureSelectedWorkspace}
                                               className="w-10 h-10 rounded-xl bg-primary/10 text-primary hover:bg-primary hover:text-white transition-all flex items-center justify-center disabled:opacity-20"
                                            >
                                               <span className="material-symbols-outlined text-lg">add</span>
                                            </button>
                                         </div>
                                      ))}
                                      {availableClassesToAssign.length === 0 && (
                                         <div className="p-12 text-center opacity-30">
                                            <p className="text-[10px] font-black uppercase tracking-widest">Đã gán hết danh sách lớp</p>
                                         </div>
                                      )}
                                   </div>
                                </div>
                            </div>
                        ) : (
                           <div className="py-20 text-center opacity-30">
                              <span className="material-symbols-outlined text-6xl mb-4">settings_suggest</span>
                              <p className="text-[11px] font-black uppercase tracking-[0.2em]">Chọn một Workspace để bắt đầu thiết kế luồng sinh viên</p>
                           </div>
                        )}
                    </div>
                
    );
}
