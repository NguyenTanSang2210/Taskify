import ProgressRow from "../../components/ProgressRow";
import FileDownloadButton from "../../components/FileDownloadButton";

/** Tách từ ProjectSpacePage.jsx (P3-11). */
export default function ProgressHistoryPanel({ filteredItems, handleLecturerUpdate, isLecturer, isStudent, loading, loadingId, recentCount }) {
  return (
    <div className={isStudent ? "lg:col-span-8 animate-in slide-in-from-right-4 duration-500" : "w-full animate-in slide-in-from-bottom-4 duration-500"}>
       <div className="bg-white dark:bg-slate-900 rounded-[2.5rem] border border-outline-variant/10 shadow-sm overflow-hidden min-h-[500px] flex flex-col">
          <div className="px-8 py-6 border-b border-outline-variant/5 bg-surface-container-low/30 flex justify-between items-center">
             <div>
                <h2 className="text-xl font-black text-on-surface uppercase tracking-tight font-headline">Lịch sử hoạt động</h2>
                <p className="text-[10px] text-outline font-bold mt-1 uppercase tracking-widest">Danh sách các báo cáo tiến độ và phản hồi</p>
             </div>
             <div className="flex items-center gap-2">
                <span className="text-xs font-black text-outline mr-2">{recentCount} MỤC</span>
                <div className="w-10 h-10 rounded-xl bg-surface-container flex items-center justify-center">
                   <span className="material-symbols-outlined text-outline">view_agenda</span>
                </div>
             </div>
          </div>
    
          <div className="overflow-x-auto flex-grow">
             <table className="w-full text-left border-collapse">
                <thead>
                   <tr className="bg-surface-container-low/10">
                      <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline" style={{ width: isLecturer ? "15%" : "20%" }}>Mốc thời gian</th>
                      <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline" style={{ width: isLecturer ? "20%" : "30%" }}>{isLecturer ? "Chi tiết sinh viên" : "Tiêu đề báo cáo"}</th>
                      {isLecturer ? <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline" style={{ width: "20%" }}>Nội dung báo cáo</th> : <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline text-center" style={{ width: "15%" }}>Trạng thái</th>}
                      {isLecturer ? <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline" style={{ width: "15%" }}>Tiến độ / Hạn</th> : <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline" style={{ width: "25%" }}>Nhận xét từ GV</th>}
                      {isLecturer && <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline" style={{ width: "20%" }}>Phản hồi chuyên môn</th>}
                      {isLecturer && <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline text-right" style={{ width: "10%" }}>Xác nhận</th>}
                      {!isLecturer && <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline text-right" style={{ width: "10%" }}>Tài liệu</th>}
                   </tr>
                </thead>
                <tbody className="divide-y divide-outline-variant/5">
                   {loading ? (
                      <tr>
                         <td colSpan={isLecturer ? 6 : 5} className="py-20 text-center opacity-40">
                            <div className="animate-spin rounded-full h-8 w-8 border-4 border-primary border-t-transparent mx-auto mb-4"></div>
                            <p className="text-[10px] font-black uppercase tracking-widest">Đang đồng bộ dữ liệu...</p>
                         </td>
                      </tr>
                   ) : filteredItems.length === 0 ? (
                      <tr>
                         <td colSpan={isLecturer ? 6 : 5} className="py-32 text-center opacity-40">
                            <span className="material-symbols-outlined text-6xl mb-4">move_item</span>
                            <h3 className="text-lg font-black uppercase tracking-widest">Không có dữ liệu</h3>
                            <p className="text-xs font-bold mt-2 uppercase tracking-tight">Chưa có hoạt động nào trong không gian này.</p>
                         </td>
                      </tr>
                   ) : (
                      filteredItems.map((it) => (
                        isLecturer ? (
                          <ProgressRow
                            key={it.id}
                            item={it}
                            loadingId={loadingId}
                            onSave={(s, c, d) => handleLecturerUpdate(it, s, c, d)}
                          />
                        ) : (
                          <tr key={it.id} className="group hover:bg-primary/[0.01] transition-colors">
                             <td className="px-8 py-6">
                                <p className="text-[11px] font-black text-on-surface uppercase">{new Date(it.createdAt).toLocaleDateString('vi-VN')}</p>
                                <p className="text-[10px] font-bold text-outline mt-1 uppercase opacity-60">{new Date(it.createdAt).toLocaleTimeString('vi-VN', {hour: '2-digit', minute:'2-digit'})}</p>
                             </td>
                             <td className="px-8 py-6">
                                <p className="text-sm font-black text-on-surface uppercase tracking-tight group-hover:text-primary transition-colors">{it.title}</p>
                                <p className="text-[11px] text-outline mt-1 font-medium line-clamp-1">{it.content}</p>
                             </td>
                             <td className="px-8 py-6 text-center">
                                <div className={`inline-flex items-center px-3 py-1 rounded-full text-[10px] font-black uppercase tracking-widest ${
                                   it.status === "DONE" ? "bg-primary text-white" :
                                   it.status === "IN_PROGRESS" ? "bg-secondary-container text-on-secondary-container" : "bg-surface-container text-outline"
                                }`}>
                                   {it.status}
                                </div>
                             </td>
                             <td className="px-8 py-6">
                                {it.lecturerComment ? (
                                   <div className="bg-surface-container-low p-3 rounded-xl border border-outline-variant/10">
                                      <p className="text-[11px] font-bold text-on-surface-variant leading-relaxed line-clamp-2">{it.lecturerComment}</p>
                                      {it.deadline && (
                                          <p className="mt-2 text-[9px] font-black text-secondary uppercase tracking-widest flex items-center gap-1">
                                             <span className="material-symbols-outlined text-[12px]">event</span>
                                             Hạn: {new Date(it.deadline).toLocaleDateString('vi-VN')}
                                          </p>
                                      )}
                                   </div>
                                ) : (
                                   <span className="text-[10px] font-black uppercase tracking-widest text-outline/30 italic">Đang đợi giảng viên đánh giá...</span>
                                )}
                             </td>
                             <td className="px-8 py-6 text-right">
                                {it.fileUrl ? (
                                   <FileDownloadButton
                                      fileUrl={it.fileUrl}
                                      className="p-2.5 rounded-xl bg-surface-container text-primary hover:bg-primary hover:text-white transition-all shadow-sm inline-flex"
                                   >
                                      <span className="material-symbols-outlined text-lg">download</span>
                                   </FileDownloadButton>
                                ) : (
                                   <span className="material-symbols-outlined text-outline/20">block</span>
                                )}
                             </td>
                          </tr>
                        )
                      ))
                   )}
                </tbody>
             </table>
          </div>
       </div>
    </div>
  );
}
