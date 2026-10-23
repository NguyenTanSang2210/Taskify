import { Link } from "react-router-dom";

/** Tách từ LecturerTopicsPage.jsx (P3-11). */
export default function TopicTable({ actionLoadingId, error, getStatusInfo, handleClose, handleDelete, handleOpen, items, load, loading, openEdit, page, setPage, totalPages }) {
  return (
    <div className="bg-white dark:bg-slate-900 rounded-[2.5rem] border border-outline-variant/10 shadow-sm overflow-hidden min-h-[400px] flex flex-col">
      {loading ? (
          <div className="flex-grow flex items-center justify-center p-20 opacity-40">
             <div className="animate-spin rounded-full h-12 w-12 border-4 border-primary border-t-transparent"></div>
          </div>
      ) : error ? (
          <div className="flex-grow flex flex-col items-center justify-center p-20 text-center">
             <span className="material-symbols-outlined text-6xl text-error/30 mb-4">error</span>
             <p className="text-sm font-black uppercase tracking-widest text-error">{error}</p>
             <button onClick={() => load()} className="mt-4 text-xs font-black uppercase tracking-widest text-primary hover:underline">Thử lại</button>
          </div>
      ) : items.length === 0 ? (
          <div className="flex-grow flex flex-col items-center justify-center p-24 text-center opacity-40">
             <span className="material-symbols-outlined text-7xl mb-4">move_to_inbox</span>
             <h3 className="text-xl font-black uppercase tracking-widest">Chưa có đề tài nào</h3>
             <p className="text-xs font-bold mt-2">Bắt đầu bằng cách tạo đề tài nghiên cứu đầu tiên của bạn.</p>
          </div>
      ) : (
        <>
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
               <thead>
                  <tr className="bg-surface-container-low/50">
                     <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline">Thông tin đề tài</th>
                     <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline">Trạng thái</th>
                     <th className="px-6 py-5 text-[10px] uppercase tracking-widest font-black text-outline text-center">Đăng ký</th>
                     <th className="px-6 py-5 text-[10px] uppercase tracking-widest font-black text-outline text-center">Chờ</th>
                     <th className="px-6 py-5 text-[10px] uppercase tracking-widest font-black text-outline text-center">Duyệt</th>
                     <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline text-right">Thao tác</th>
                  </tr>
               </thead>
               <tbody className="divide-y divide-outline-variant/10">
                  {items.map((t) => {
                     const status = getStatusInfo(t.status);
                     return (
                       <tr key={t.id} className="group hover:bg-primary/[0.02] transition-colors">
                          <td className="px-8 py-6 max-w-sm">
                             <p className="text-sm font-black text-on-surface uppercase tracking-tight leading-snug group-hover:text-primary transition-colors">{t.title}</p>
                             <p className="text-[11px] text-outline mt-1.5 font-medium line-clamp-1">{t.description || "Không có mô tả chi tiết."}</p>
                          </td>
                          <td className="px-8 py-6">
                             <div className={`inline-flex items-center gap-2 px-3 py-1 rounded-full ${status.bg} ${status.color}`}>
                                <span className="material-symbols-outlined text-sm">{status.icon}</span>
                                <span className="text-[10px] font-black uppercase tracking-widest">{status.label}</span>
                             </div>
                          </td>
                          <td className="px-6 py-6 text-center">
                             <span className="text-sm font-black text-on-surface">{t.registrationCount ?? 0}</span>
                          </td>
                          <td className="px-6 py-6 text-center">
                             <span className={`text-sm font-black ${t.pendingCount > 0 ? "text-secondary" : "text-outline/30"}`}>{t.pendingCount ?? 0}</span>
                          </td>
                          <td className="px-6 py-6 text-center">
                             <span className={`text-sm font-black ${t.approvedCount > 0 ? "text-primary" : "text-outline/30"}`} title="Đã duyệt / số chỗ">{t.approvedCount ?? 0}/{t.capacity || 1}</span>
                          </td>
                          <td className="px-8 py-6 text-right">
                             <div className="flex justify-end gap-2">
                                <Link 
                                  to={`/lecturer?topicId=${t.id}`}
                                  className="p-2.5 rounded-xl bg-surface-container hover:bg-secondary hover:text-white transition-all shadow-sm group/btn"
                                  title="Xem danh sách đăng ký"
                                >
                                   <span className="material-symbols-outlined text-lg">group</span>
                                </Link>
                                <button 
                                  onClick={() => openEdit(t)}
                                  className="p-2.5 rounded-xl bg-surface-container hover:bg-primary hover:text-white transition-all shadow-sm"
                                  title="Chỉnh sửa đề tài"
                                >
                                   <span className="material-symbols-outlined text-lg">edit</span>
                                </button>
                                {t.status === "COMPLETED" ? null : t.status === "CLOSED" ? (
                                  <button 
                                    onClick={() => handleOpen(t.id)} 
                                    disabled={actionLoadingId === t.id}
                                    className="p-2.5 rounded-xl bg-surface-container hover:bg-green-600 hover:text-white transition-all shadow-sm disabled:opacity-50"
                                    title="Mở đăng ký"
                                  >
                                     <span className="material-symbols-outlined text-lg">play_arrow</span>
                                  </button>
                                ) : (
                                  <button 
                                    onClick={() => handleClose(t.id)} 
                                    disabled={actionLoadingId === t.id}
                                    className="p-2.5 rounded-xl bg-surface-container hover:bg-amber-600 hover:text-white transition-all shadow-sm disabled:opacity-50"
                                    title="Đóng đăng ký"
                                  >
                                     <span className="material-symbols-outlined text-lg">stop</span>
                                  </button>
                                )}
                                <button 
                                  onClick={() => handleDelete(t.id)} 
                                  disabled={actionLoadingId === t.id}
                                  className="p-2.5 rounded-xl bg-surface-container hover:bg-error hover:text-white transition-all shadow-sm disabled:opacity-50"
                                  title="Xóa đề tài"
                                >
                                   <span className="material-symbols-outlined text-lg">delete</span>
                                </button>
                             </div>
                          </td>
                       </tr>
                     );
                  })}
               </tbody>
            </table>
          </div>
    
          {/* Pagination Layer */}
          <div className="mt-auto px-8 py-6 bg-surface-container-low/30 border-t border-outline-variant/10 flex flex-col md:flex-row justify-between items-center gap-4">
             <p className="text-[10px] font-black uppercase tracking-widest text-outline">
                Hiển thị đề tài trang {page + 1} của {Math.max(1, totalPages)}
             </p>
             
             <div className="flex items-center gap-2">
                <button
                  className={`p-2 rounded-xl transition-all ${
                    page <= 0 
                    ? "bg-surface-container-high/50 text-outline/30 cursor-not-allowed" 
                    : "bg-white border border-outline-variant/20 text-on-surface hover:bg-primary hover:text-white"
                  }`}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page <= 0}
                >
                  <span className="material-symbols-outlined">chevron_left</span>
                </button>
    
                <div className="flex items-center gap-1 mx-2">
                   {[...Array(totalPages)].map((_, i) => (
                      <button 
                        key={i}
                        onClick={() => setPage(i)}
                        className={`w-8 h-8 rounded-lg text-[10px] font-black transition-all ${
                          page === i ? "bg-primary text-white" : "text-outline hover:bg-surface-container"
                        }`}
                      >
                        {i + 1}
                      </button>
                   ))}
                </div>
    
                <button
                  className={`p-2 rounded-xl transition-all ${
                    page + 1 >= totalPages
                    ? "bg-surface-container-high/50 text-outline/30 cursor-not-allowed" 
                    : "bg-white border border-outline-variant/20 text-on-surface hover:bg-primary hover:text-white"
                  }`}
                  onClick={() => setPage((p) => (p + 1 < totalPages ? p + 1 : p))}
                  disabled={page + 1 >= totalPages}
                >
                  <span className="material-symbols-outlined">chevron_right</span>
                </button>
             </div>
          </div>
        </>
      )}
    </div>
  );
}
