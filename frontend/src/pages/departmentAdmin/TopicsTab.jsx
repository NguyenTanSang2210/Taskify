import { getTopicStatusLabel } from './helpers';

/** Tab "topics" của DepartmentAdminPage (tách ở P3-11; dữ liệu + xử lý ở useDepartmentAdmin). */
export default function TopicsTab({ topics, page, setPage, totalPages, toggleTopicStatus }) {
    return (
                    <div className="p-10 space-y-8 animate-in slide-in-from-bottom-4 duration-500">
                        <div className="flex justify-between items-center mb-4">
                           <h3 className="text-sm font-black text-outline uppercase tracking-[0.2em] px-4">Đề tài Nghiên cứu thuộc Khoa</h3>
                           <span className="px-4 py-2 bg-primary/10 text-primary rounded-xl text-[10px] font-black uppercase tracking-widest">Trang {page + 1} / {totalPages || 1}</span>
                        </div>
                        
                        <div className="overflow-x-auto min-h-[400px]">
                            <table className="w-full text-left border-collapse">
                                <thead>
                                    <tr className="bg-surface-container-low/50">
                                        <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline">ID</th>
                                        <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline">Tiêu đề đề tài</th>
                                        <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline">Giảng viên hướng dẫn</th>
                                        <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline">Tình trạng</th>
                                        <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline">Thống kê đăng ký</th>
                                        <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline text-right">Quản trị viên</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-outline-variant/5">
                                    {topics.map(t => (
                                        <tr key={t.id} className="group hover:bg-primary/[0.01] transition-colors">
                                            <td className="px-8 py-6 font-black text-[10px] text-outline tracking-widest">#{t.id}</td>
                                            <td className="px-8 py-6 max-w-sm">
                                               <h4 className="text-sm font-black text-on-surface uppercase tracking-tight group-hover:text-primary transition-colors line-clamp-2">{t.title}</h4>
                                            </td>
                                            <td className="px-8 py-6">
                                               <p className="text-xs font-bold text-on-surface-variant uppercase tracking-tight">{t.lecturerName || '—'}</p>
                                               {t.workspaceName && <p className="text-[9px] font-black text-outline uppercase mt-0.5">{t.workspaceName}</p>}
                                            </td>
                                            <td className="px-8 py-6">
                                               <div className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-md text-[9px] font-black uppercase tracking-widest ${t.status === 'OPEN' ? 'bg-green-500/10 text-green-600' : 'bg-surface-container text-outline'}`}>
                                                  <span className="w-1.5 h-1.5 rounded-full bg-current"></span>
                                                  {getTopicStatusLabel(t.status)}
                                               </div>
                                            </td>
                                            <td className="px-8 py-6">
                                               <div className="flex items-center gap-4">
                                                  <div className="text-center">
                                                     <p className="text-lg font-black text-primary leading-none">{t.registrationCount}</p>
                                                     <p className="text-[8px] font-black text-outline uppercase mt-1">Đã Đăng Ký</p>
                                                  </div>
                                                  <div className="w-[1px] h-6 bg-outline-variant/20"></div>
                                                  <div className="text-center">
                                                     <p className="text-lg font-black text-secondary leading-none">{t.pendingCount}</p>
                                                     <p className="text-[8px] font-black text-outline uppercase mt-1">Chờ Duyệt</p>
                                                  </div>
                                               </div>
                                            </td>
                                            <td className="px-8 py-6 text-right">
                                               <button 
                                                  onClick={() => toggleTopicStatus(t)}
                                                  className={`px-6 py-2 rounded-xl text-[10px] font-black uppercase tracking-widest shadow-sm transition-all ${t.status === 'OPEN' || t.status === 'FULL' ? 'bg-error/5 text-error hover:bg-error hover:text-white' : 'bg-primary/5 text-primary hover:bg-primary hover:text-white'}`}
                                               >
                                                  {t.status === 'OPEN' || t.status === 'FULL' ? 'Đóng đề tài' : 'Mở đề tài'}
                                               </button>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                        {totalPages > 1 && (
                           <div className="pt-8 flex justify-center gap-2">
                              <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0} className="w-10 h-10 rounded-xl bg-white border border-outline-variant/10 flex items-center justify-center hover:bg-primary hover:text-white transition-all disabled:opacity-20">
                                 <span className="material-symbols-outlined">chevron_left</span>
                              </button>
                              <div className="px-6 h-10 flex items-center justify-center bg-surface-container rounded-xl text-[11px] font-black uppercase tracking-widest text-on-surface-variant">
                                 Trang {page + 1}
                              </div>
                              <button onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} disabled={page + 1 >= totalPages} className="w-10 h-10 rounded-xl bg-white border border-outline-variant/10 flex items-center justify-center hover:bg-primary hover:text-white transition-all disabled:opacity-20">
                                 <span className="material-symbols-outlined">chevron_right</span>
                              </button>
                           </div>
                        )}
                    </div>
                
    );
}
