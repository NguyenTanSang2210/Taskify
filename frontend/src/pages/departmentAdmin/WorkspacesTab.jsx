import { getWorkspaceStatusLabel } from './helpers';

/** Tab "workspaces" của DepartmentAdminPage (tách ở P3-11; dữ liệu + xử lý ở useDepartmentAdmin). */
export default function WorkspacesTab({ workspaces, wsCreate, setWsCreate, createWorkspace, transitionWorkspace, nextWorkspaceStatus }) {
    return (
                    <div className="p-10 space-y-12 animate-in slide-in-from-bottom-4 duration-500">
                        <section className="bg-surface-container-low/30 p-8 rounded-[2rem] border border-outline-variant/5">
                            <div className="flex items-center gap-4 mb-8">
                               <div className="w-12 h-12 rounded-2xl bg-primary/10 text-primary flex items-center justify-center">
                                  <span className="material-symbols-outlined text-2xl">add_circle</span>
                               </div>
                               <div>
                                  <h3 className="text-lg font-black text-on-surface uppercase tracking-tight">Tạo chu kỳ Workspace mới</h3>
                                  <p className="text-[10px] text-outline font-bold uppercase tracking-widest mt-0.5">Khởi tạo không gian cho một kỳ làm đề án/nghiên cứu</p>
                               </div>
                            </div>
                            
                            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-12 gap-6 items-end">
                                <div className="lg:col-span-4 space-y-1.5">
                                    <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Tên chu kỳ Workspace</label>
                                    <input className="w-full px-5 py-3.5 bg-white border border-outline-variant/10 focus:border-primary focus:ring-0 rounded-xl text-sm font-black transition-all outline-none" value={wsCreate.name} onChange={(e) => setWsCreate(s => ({ ...s, name: e.target.value }))} placeholder="VD: Khóa luận tốt nghiệp 2024" />
                                </div>
                                <div className="lg:col-span-2 space-y-1.5">
                                    <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Loại hình</label>
                                    <input className="w-full px-5 py-3.5 bg-white border border-outline-variant/10 focus:border-primary focus:ring-0 rounded-xl text-sm font-black transition-all outline-none" value={wsCreate.type} onChange={(e) => setWsCreate(s => ({ ...s, type: e.target.value }))} placeholder="KLTN/DA2" />
                                </div>
                                <div className="lg:col-span-2 space-y-1.5">
                                    <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Học kỳ</label>
                                    <input className="w-full px-5 py-3.5 bg-white border border-outline-variant/10 focus:border-primary focus:ring-0 rounded-xl text-sm font-black transition-all outline-none" value={wsCreate.semester} onChange={(e) => setWsCreate(s => ({ ...s, semester: e.target.value }))} placeholder="HK1-2024" />
                                </div>
                                <div className="lg:col-span-2 space-y-1.5">
                                    <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Ngày bắt đầu</label>
                                    <input type="datetime-local" className="w-full px-5 py-3.5 bg-white border border-outline-variant/10 focus:border-primary focus:ring-0 rounded-xl text-[11px] font-bold outline-none" value={wsCreate.startAt} onChange={(e) => setWsCreate(s => ({ ...s, startAt: e.target.value }))} />
                                </div>
                                <div className="lg:col-span-2">
                                   <button onClick={createWorkspace} className="w-full py-3.5 bg-primary text-white rounded-xl font-black text-[10px] uppercase tracking-widest shadow-lg shadow-primary/20 hover:scale-105 active:scale-95 transition-all">Tạo Workspace</button>
                                </div>
                            </div>
                        </section>

                        <section className="space-y-6">
                            <h3 className="text-sm font-black text-outline uppercase tracking-[0.2em] px-4">Lịch sử và Trạng thái Workspace</h3>
                            <div className="overflow-x-auto">
                                <table className="w-full text-left border-collapse">
                                    <thead>
                                        <tr className="bg-surface-container-low/50">
                                            <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline">Workspace</th>
                                            <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline">Học kỳ</th>
                                            <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline">Giai đoạn</th>
                                            <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline text-right">Lộ trình & Thao tác</th>
                                        </tr>
                                    </thead>
                                    <tbody className="divide-y divide-outline-variant/5">
                                        {workspaces.map(w => {
                                            const next = nextWorkspaceStatus(w.status);
                                            return (
                                                <tr key={w.id} className="group hover:bg-primary/[0.01] transition-colors">
                                                    <td className="px-8 py-6">
                                                       <p className="text-xs font-black text-outline tracking-widest mb-1">#{w.id}</p>
                                                       <p className="text-sm font-black text-on-surface uppercase group-hover:text-primary transition-colors">{w.name}</p>
                                                    </td>
                                                    <td className="px-8 py-6">
                                                       <span className="text-[11px] font-black text-secondary uppercase tracking-widest">{w.semester || 'Hệ thống'}</span>
                                                    </td>
                                                    <td className="px-8 py-6">
                                                       <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-surface-container text-on-surface-variant border border-outline-variant/5">
                                                          <span className="w-2 h-2 rounded-full bg-primary/40"></span>
                                                          <span className="text-[9px] font-black uppercase tracking-widest">{getWorkspaceStatusLabel(w.status)}</span>
                                                       </div>
                                                    </td>
                                                    <td className="px-8 py-6 text-right">
                                                       <div className="inline-flex gap-2">
                                                          {next && (
                                                             <button 
                                                                onClick={() => transitionWorkspace(w.id, next)}
                                                                className="px-4 py-2 bg-primary text-white rounded-lg text-[9px] font-black uppercase tracking-widest shadow-md shadow-primary/10 hover:scale-105 active:scale-95 transition-all flex items-center gap-2"
                                                             >
                                                                <span className="material-symbols-outlined text-[14px]">arrow_forward</span>
                                                                Tiến tới: {getWorkspaceStatusLabel(next)}
                                                             </button>
                                                          )}
                                                          {w.status !== 'CLOSED' && (
                                                             <button 
                                                                onClick={() => transitionWorkspace(w.id, 'CLOSED')}
                                                                className="px-4 py-2 bg-error/5 text-error border border-error/10 rounded-lg text-[9px] font-black uppercase tracking-widest hover:bg-error hover:text-white transition-all"
                                                             >
                                                                Đóng ngay
                                                             </button>
                                                          )}
                                                       </div>
                                                    </td>
                                                </tr>
                                            );
                                        })}
                                    </tbody>
                                </table>
                            </div>
                        </section>
                    </div>
                
    );
}
