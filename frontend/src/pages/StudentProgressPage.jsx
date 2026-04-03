import React, { useEffect, useState, useCallback, useMemo } from "react";
import { registrationApi } from "../api/registrationApi";
import { progressApi } from "../api/progressApi";
import { fileApi } from "../api/fileApi";
import { connectAndSubscribe } from "../ws/stomp";
import { useAuth } from "../context/AuthContext";
import InlineNotice from "../components/InlineNotice";

export default function StudentProgressPage() {
  const { user } = useAuth();

  const [topicId, setTopicId] = useState("");
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);
  const [notice, setNotice] = useState(null);

  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [file, setFile] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  // --- LOAD TOPIC ---
  const loadStudentTopic = useCallback(async () => {
    if (!user?.id) return;
    try {
      const data = await registrationApi.getMine(user.id);
      const approved = (data || []).filter((r) => r.approved === true);
      if (approved.length > 0) {
        setTopicId(String(approved[0].topic?.id || ""));
      }
    } catch (e) {
      console.error(e);
    }
  }, [user?.id]);

  useEffect(() => {
    loadStudentTopic();
  }, [loadStudentTopic]);

  // --- LOAD PROGRESS ---
  const loadProgress = useCallback(async () => {
    if (!topicId) {
      setItems([]);
      return;
    }
    setLoading(true);
    try {
      const arr = await progressApi.listByStudent(user.id, topicId);
      setItems(arr || []);
    } catch (e) {
      console.error(e);
      setNotice({ type: "danger", message: "Không tải được dữ liệu tiến độ" });
    } finally {
      setLoading(false);
    }
  }, [topicId, user?.id]);

  useEffect(() => {
    loadProgress();
  }, [loadProgress]);

  // --- WEBSOCKET ---
  useEffect(() => {
    if (!topicId) return;
    const client = connectAndSubscribe({
      destination: `/topic/progress/${topicId}`,
      onMessage: (payload) => {
        const type = payload?.type;
        const pr = payload?.progress;
        if (!type || !pr) return;
        if (type === "PROGRESS_UPDATED") {
          setItems((prev) => prev.map((x) => (x.id === pr.id ? pr : x)));
          setNotice({ type: "info", message: "Giảng viên đã cập nhật đánh giá báo cáo của bạn!" });
        }
      },
    });
    return () => client?.deactivate?.();
  }, [topicId]);

  // --- SUBMIT ---
  const handleStudentSubmit = async (e) => {
    e.preventDefault();
    if (!title.trim()) {
      setNotice({ type: "warning", message: "Vui lòng nhập tiêu đề báo cáo." });
      return;
    }
    setSubmitting(true);
    try {
      let fileUrl = "";
      if (file) {
        fileUrl = await fileApi.upload(file);
      }
      await progressApi.create(user.id, topicId, {
        title: title.trim(),
        content: content.trim(),
        fileUrl,
      });
      setNotice({ type: "success", message: "Gửi báo cáo tiến độ thành công!" });
      setTitle("");
      setContent("");
      setFile(null);
      loadProgress();
    } catch (err) {
      console.error(err);
      setNotice({ type: "danger", message: "Lỗi hệ thống khi gửi báo cáo." });
    } finally {
      setSubmitting(false);
    }
  };

  const getStatusStyle = (s) => {
    if (s === 'DONE') return "bg-primary text-white";
    if (s === 'IN_PROGRESS') return "bg-secondary-container text-on-secondary-container";
    return "bg-surface-container text-outline";
  };

  return (
    <div className="space-y-8 animate-in fade-in duration-500 pb-12">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">
        <div>
          <span className="text-[10px] font-black uppercase tracking-[0.2em] text-primary mb-2 block">Nhật ký nghiên cứu</span>
          <h1 className="text-3xl font-black text-on-surface tracking-tight font-headline uppercase leading-none">Theo dõi tiến độ</h1>
          <p className="text-sm text-outline mt-2 font-medium">Ghi lại các mốc quan trọng, nộp báo cáo tuần và nhận phản hồi từ giảng viên hướng dẫn.</p>
        </div>
        
        <div className="flex items-center gap-3">
           <div className="px-4 py-2 bg-secondary-container text-on-secondary-container rounded-xl text-[10px] font-black uppercase tracking-widest flex items-center gap-2">
              <span className="material-symbols-outlined text-sm">history_edu</span>
              Học kỳ hiện tại
           </div>
        </div>
      </div>

      {notice && (
        <div className="max-w-2xl mx-auto">
          <InlineNotice
            type={notice.type}
            message={notice.message}
            onClose={() => setNotice(null)}
            autoHideMs={3000}
          />
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        {/* Creation Column */}
        <aside className="lg:col-span-4 space-y-8">
           <div className="bg-white dark:bg-slate-900 p-8 rounded-[2.5rem] border border-outline-variant/10 shadow-sm">
              <div className="flex items-center gap-4 mb-8">
                 <div className="w-12 h-12 rounded-2xl bg-primary/5 text-primary flex items-center justify-center">
                    <span className="material-symbols-outlined text-2xl">add_task</span>
                 </div>
                 <div>
                    <h2 className="text-sm font-black text-on-surface uppercase tracking-widest">Tạo báo cáo mới</h2>
                    <p className="text-[10px] text-outline font-bold mt-0.5 uppercase tracking-tight">Cập nhật tiến độ của bạn</p>
                 </div>
              </div>

              {!topicId ? (
                <div className="text-center py-10 space-y-4">
                   <div className="w-16 h-16 bg-surface-container rounded-full mx-auto flex items-center justify-center text-outline/30">
                      <span className="material-symbols-outlined text-3xl">lock</span>
                   </div>
                   <p className="text-xs font-bold text-outline leading-relaxed uppercase tracking-widest">Bạn chưa tham gia đề tài nghiên cứu nào được phê duyệt.</p>
                   <button className="text-[10px] font-black text-primary uppercase tracking-widest hover:underline">Tìm đề tài ngay →</button>
                </div>
              ) : (
                <form onSubmit={handleStudentSubmit} className="space-y-6">
                  <div className="space-y-1.5">
                    <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Tiêu đề báo cáo</label>
                    <input 
                      className="w-full px-4 py-3 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-xl text-sm font-bold transition-all duration-300 outline-none" 
                      value={title} 
                      onChange={e => setTitle(e.target.value)} 
                      placeholder="VD: Báo cáo tuần 1: Nghiên cứu tài liệu" 
                    />
                  </div>
                  <div className="space-y-1.5">
                    <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Nội dung chi tiết</label>
                    <textarea 
                      className="w-full px-4 py-3 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-xl text-sm font-bold transition-all duration-300 outline-none min-h-[150px]" 
                      rows={5} 
                      value={content} 
                      onChange={e => setContent(e.target.value)} 
                      placeholder="Mô tả công việc đã hoàn thành, các khó khăn gặp phải..."
                    />
                  </div>
                  <div className="space-y-1.5">
                    <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Đính kèm tài liệu (PDF, DOCX, ZIP)</label>
                    <div className="relative group">
                       <input 
                          type="file" 
                          className="absolute inset-0 opacity-0 cursor-pointer z-10" 
                          onChange={e => setFile(e.target.files[0])} 
                       />
                       <div className="w-full px-4 py-4 border-2 border-dashed border-outline-variant/30 group-hover:border-primary/50 group-hover:bg-primary/[0.02] rounded-xl flex items-center justify-center gap-3 transition-all">
                          <span className="material-symbols-outlined text-outline group-hover:text-primary">upload_file</span>
                          <span className="text-[11px] font-black uppercase tracking-widest text-outline group-hover:text-primary truncate">
                             {file ? file.name : "Tải tệp đính kèm lên"}
                          </span>
                       </div>
                    </div>
                  </div>
                  <button 
                    className="w-full py-4 bg-primary text-white rounded-xl font-black text-sm uppercase tracking-widest shadow-xl shadow-primary/20 hover:scale-105 active:scale-95 transition-all disabled:opacity-50" 
                    type="submit" 
                    disabled={submitting}
                  >
                    {submitting ? "Đang xử lý..." : "Gửi báo cáo tiến độ"}
                  </button>
                </form>
              )}
           </div>

           <div className="bg-secondary/5 p-6 rounded-[2rem] border border-secondary/10 flex items-center gap-4">
              <div className="w-10 h-10 rounded-xl bg-secondary text-white flex items-center justify-center shrink-0">
                 <span className="material-symbols-outlined">lightbulb</span>
              </div>
              <div>
                 <p className="text-[10px] font-black uppercase tracking-[0.1em] text-secondary">Mẹo học thuật</p>
                 <p className="text-xs font-bold text-on-surface-variant mt-0.5 leading-relaxed">Hãy nộp báo cáo đúng hạn mỗi tuần để giảng viên có thể theo dõi và phản hồi kịp thời.</p>
              </div>
           </div>
        </aside>

        {/* History Column */}
        <section className="lg:col-span-8 space-y-6">
           <div className="bg-white dark:bg-slate-900 rounded-[2.5rem] border border-outline-variant/10 shadow-sm overflow-hidden min-h-[500px] flex flex-col">
              <div className="px-8 py-6 border-b border-outline-variant/5 bg-surface-container-low/30 flex justify-between items-center">
                 <div>
                    <h2 className="text-xl font-black text-on-surface uppercase tracking-tight font-headline">Lịch sử báo cáo</h2>
                    <p className="text-[10px] text-outline font-bold mt-1 uppercase tracking-widest">Danh sách các cập nhật và phản hồi từ giảng viên</p>
                 </div>
                 <button onClick={loadProgress} className="p-2.5 bg-surface-container text-on-surface-variant rounded-xl hover:bg-surface-container-high transition-all">
                    <span className="material-symbols-outlined">refresh</span>
                 </button>
              </div>

              <div className="overflow-x-auto flex-grow">
                 <table className="w-full text-left border-collapse">
                    <thead>
                       <tr className="bg-surface-container-low/10">
                          <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline" style={{ width: "20%" }}>Thời gian</th>
                          <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline" style={{ width: "25%" }}>Nội dung báo cáo</th>
                          <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline text-center" style={{ width: "15%" }}>Trạng thái</th>
                          <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline" style={{ width: "40%" }}>Phản hồi từ Giảng viên</th>
                       </tr>
                    </thead>
                    <tbody className="divide-y divide-outline-variant/5">
                       {loading ? (
                          <tr>
                             <td colSpan="4" className="py-20 text-center opacity-40">
                                <div className="animate-spin rounded-full h-8 w-8 border-4 border-primary border-t-transparent mx-auto mb-4"></div>
                                <p className="text-[10px] font-black uppercase tracking-widest">Đang tải lịch sử...</p>
                             </td>
                          </tr>
                       ) : items.length === 0 ? (
                          <tr>
                             <td colSpan="4" className="py-32 text-center opacity-40">
                                <span className="material-symbols-outlined text-6xl mb-4">history_toggle_off</span>
                                <h3 className="text-lg font-black uppercase tracking-widest">Trống</h3>
                                <p className="text-xs font-bold mt-2 uppercase tracking-tight">Cần nộp báo cáo đầu tiên để bắt đầu theo dõi.</p>
                             </td>
                          </tr>
                       ) : (
                          items.map(it => (
                            <tr key={it.id} className="group hover:bg-primary/[0.01] transition-colors">
                               <td className="px-8 py-6">
                                  <p className="text-xs font-black text-on-surface">{new Date(it.createdAt).toLocaleDateString('vi-VN')}</p>
                                  <p className="text-[10px] font-bold text-outline mt-1 uppercase">{new Date(it.createdAt).toLocaleTimeString('vi-VN', {hour: '2-digit', minute:'2-digit'})}</p>
                               </td>
                               <td className="px-8 py-6">
                                  <p className="text-sm font-black text-on-surface uppercase tracking-tight group-hover:text-primary transition-colors">{it.title}</p>
                                  <p className="text-[11px] text-outline mt-1.5 font-medium line-clamp-2 leading-relaxed">{it.content || "Không có mô tả chi tiết."}</p>
                                  {it.fileUrl && (
                                     <a 
                                       href={it.fileUrl} 
                                       target="_blank" 
                                       rel="noreferrer" 
                                       className="inline-flex items-center gap-1.5 mt-3 text-[10px] font-black text-primary uppercase tracking-widest hover:underline"
                                     >
                                        <span className="material-symbols-outlined text-sm">attach_file</span>
                                        Tài liệu đính kèm
                                     </a>
                                  )}
                               </td>
                               <td className="px-8 py-6 text-center">
                                  <div className={`inline-flex items-center px-3 py-1 rounded-full text-[10px] font-black uppercase tracking-widest ${getStatusStyle(it.status)}`}>
                                     {it.status}
                                  </div>
                               </td>
                               <td className="px-8 py-6">
                                  {it.lecturerComment ? (
                                     <div className="bg-surface-container-low p-4 rounded-2xl border border-outline-variant/10 relative">
                                        <span className="material-symbols-outlined absolute -left-2 -top-2 text-primary p-1 bg-white dark:bg-slate-900 rounded-lg shadow-sm border border-outline-variant/10 text-lg">format_quote</span>
                                        <p className="text-xs font-bold text-on-surface-variant leading-relaxed">{it.lecturerComment}</p>
                                        {it.deadline && (
                                           <div className="mt-3 pt-3 border-t border-outline-variant/5 flex items-center gap-2 text-[9px] font-black text-secondary uppercase tracking-widest">
                                              <span className="material-symbols-outlined text-sm">event</span>
                                              Hạn chót tiếp theo: {new Date(it.deadline).toLocaleDateString('vi-VN')}
                                           </div>
                                        )}
                                     </div>
                                  ) : (
                                     <span className="text-[10px] font-black uppercase tracking-widest text-outline/30 italic">Đang chờ phản hồi...</span>
                                  )}
                               </td>
                            </tr>
                          ))
                       )}
                    </tbody>
                 </table>
              </div>
           </div>
        </section>
      </div>
    </div>
  );
}
