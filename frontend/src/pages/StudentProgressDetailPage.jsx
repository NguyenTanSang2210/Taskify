import React, { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import InlineNotice from "../components/InlineNotice";
import { milestoneApi } from "../api/milestoneApi";
import { progressApi } from "../api/progressApi";
import { fileApi } from "../api/fileApi";
import { useAuth } from "../context/AuthContext";

export default function StudentProgressDetailPage() {
  const { user } = useAuth();
  const studentId = user?.id;
  const navigate = useNavigate();
  const location = useLocation();

  const [milestoneId, setMilestoneId] = useState("");
  const [milestone, setMilestone] = useState(null);
  const [loading, setLoading] = useState(true);
  const [notice, setNotice] = useState(null);

  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [file, setFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [fileUrl, setFileUrl] = useState("");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const msId = params.get("milestoneId");
    if (msId) setMilestoneId(msId);
  }, [location.search]);

  useEffect(() => {
    const load = async () => {
      if (!milestoneId) return;
      setLoading(true);
      try {
        const ms = await milestoneApi.get(milestoneId);
        setMilestone(ms);
        setTitle(ms?.title || "");
      } catch (e) {
        setNotice({ type: "danger", message: e.message || "Không tải được thông tin mốc tiến độ." });
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [milestoneId]);

  const handleUpload = async () => {
    if (!file) return;
    setUploading(true);
    try {
      const data = await fileApi.upload(file);
      setFileUrl(data.fileUrl);
      setNotice({ type: "success", message: "Tệp đính kèm đã được tải lên thành công!" });
    } catch (e) {
      setNotice({ type: "danger", message: e.message || "Tải tệp thất bại." });
    } finally {
      setUploading(false);
    }
  };

  const handleSubmit = async () => {
    if (!studentId || !milestone?.topic?.id) {
      setNotice({ type: "warning", message: "Thiếu thông tin nhận diện sinh viên hoặc đề tài." });
      return;
    }
    if (!title.trim() || !content.trim()) {
      setNotice({ type: "warning", message: "Vui lòng nhập cả tiêu đề và nội dung báo cáo." });
      return;
    }

    setSubmitting(true);
    try {
      let finalFileUrl = fileUrl;
      if (file && !finalFileUrl) {
        try {
            const upData = await fileApi.upload(file);
            finalFileUrl = upData.fileUrl;
        } catch (upErr) {
            throw new Error("Không thể tự động tải tệp: " + (upErr.message || "Lỗi không xác định"));
        }
      }

      const payload = {
        title: title.trim(),
        content: content.trim(),
        fileUrl: finalFileUrl || undefined,
        deadline: milestone?.deadline || undefined,
        milestoneId: milestone?.id || undefined,
      };
      await progressApi.create(studentId, milestone.topic.id, payload);
      setNotice({ type: "success", message: "Báo cáo tiến độ đã được nộp thành công!" });
      setTimeout(() => navigate("/progress"), 1500);
    } catch (e) {
      setNotice({ type: "danger", message: e.message || "Lỗi khi nộp báo cáo." });
    } finally {
      setSubmitting(false);
    }
  };

  const timeStatus = (() => {
    if (!milestone?.deadline) return { text: "Không giới hạn", color: "text-outline", bg: "bg-surface-container" };
    const diffMs = new Date(milestone.deadline) - new Date();
    if (diffMs <= 0) return { text: "Đã quá hạn", color: "text-error", bg: "bg-error/10" };
    
    const days = Math.floor(diffMs / (1000 * 60 * 60 * 24));
    const hours = Math.floor((diffMs / (1000 * 60 * 60)) % 24);
    
    if (days < 1) return { text: `Còn ${hours} giờ`, color: "text-amber-600", bg: "bg-amber-500/10" };
    return { text: `Còn ${days} ngày`, color: "text-primary", bg: "bg-primary/10" };
  })();

  return (
    <div className="max-w-5xl mx-auto space-y-8 animate-in fade-in duration-500 pb-12">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-6 shrink-0">
        <div>
          <span className="text-[10px] font-black uppercase tracking-[0.2em] text-primary mb-2 block">Cửa sổ nộp bài</span>
          <h1 className="text-3xl font-black text-on-surface tracking-tight font-headline uppercase leading-none">Báo cáo Tiến độ</h1>
          <p className="text-sm text-outline mt-2 font-medium max-w-xl">Cập nhật kết quả làm việc định kỳ để giảng viên có thể theo dõi và phản hồi kịp thời cho nhóm của bạn.</p>
        </div>
        
        <button 
           onClick={() => navigate(-1)}
           className="px-6 py-3 bg-white dark:bg-slate-900 border border-outline-variant/10 rounded-xl shadow-sm text-[10px] font-black uppercase tracking-widest flex items-center gap-2 hover:bg-surface-container transition-all"
        >
           <span className="material-symbols-outlined text-sm">arrow_back</span>
           Quay lại
        </button>
      </div>

      {notice && (
        <div className="shrink-0 w-full">
          <InlineNotice
            type={notice.type}
            message={notice.message}
            onClose={() => setNotice(null)}
            autoHideMs={3000}
          />
        </div>
      )}

      {loading ? (
        <div className="py-32 flex flex-col items-center justify-center opacity-30 select-none bg-white dark:bg-slate-900 rounded-[2.5rem] border border-outline-variant/10">
            <div className="animate-spin rounded-full h-10 w-10 border-4 border-primary border-t-transparent mb-4"></div>
            <p className="text-[10px] font-black uppercase tracking-widest">Đang tải cấu trúc mốc tiến độ...</p>
        </div>
      ) : !milestone ? (
        <div className="py-32 flex flex-col items-center justify-center opacity-30 select-none bg-white dark:bg-slate-900 rounded-[2.5rem] border border-outline-variant/10">
            <span className="material-symbols-outlined text-6xl mb-4 text-error">error</span>
            <p className="text-[10px] font-black uppercase tracking-widest">Không tìm thấy mốc tiến độ yêu cầu.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
           {/* Section 1: Milestone Info Sidebar */}
           <aside className="lg:col-span-4 space-y-6 lg:sticky lg:top-8">
              <div className="bg-white dark:bg-slate-900 p-8 rounded-[2.5rem] border border-outline-variant/10 shadow-sm space-y-6">
                 <div>
                    <h3 className="text-xs font-black text-outline uppercase tracking-widest mb-4">Yêu cầu hiện tại</h3>
                    <div className="p-5 bg-surface-container-low rounded-2xl border border-outline-variant/5">
                       <h4 className="text-sm font-black text-on-surface uppercase tracking-tight leading-snug">{milestone.title}</h4>
                       <div className="mt-4 flex items-center gap-2">
                          <span className={`px-2.5 py-1 rounded-lg text-[9px] font-black uppercase tracking-widest ${timeStatus.bg} ${timeStatus.color}`}>
                             {timeStatus.text}
                          </span>
                          <span className="text-[10px] font-bold text-outline uppercase tracking-tight">Hạn: {new Date(milestone.deadline).toLocaleDateString('vi-VN')}</span>
                       </div>
                    </div>
                 </div>

                 <div className="space-y-4">
                    <div className="flex items-start gap-4">
                       <div className="w-8 h-8 rounded-lg bg-secondary/10 text-secondary flex items-center justify-center shrink-0">
                          <span className="material-symbols-outlined text-sm">inventory_2</span>
                       </div>
                       <div>
                          <p className="text-[9px] font-black text-outline uppercase tracking-widest">Đề tài liên kết</p>
                          <p className="text-xs font-bold text-on-surface-variant uppercase mt-0.5 line-clamp-2">#{milestone.topic?.id} - {milestone.topic?.title}</p>
                       </div>
                    </div>
                    
                    {milestone.description && (
                       <div className="pt-4 border-t border-outline-variant/5">
                          <p className="text-[9px] font-black text-outline uppercase tracking-widest mb-2">Hướng dẫn chi tiết</p>
                          <div className="p-4 bg-surface-container-lowest/50 rounded-xl border border-outline-variant/5 italic text-[11px] font-medium text-on-surface-variant leading-relaxed">
                             "{milestone.description}"
                          </div>
                       </div>
                    )}
                 </div>
              </div>

              <div className="p-6 bg-primary/5 rounded-[2rem] border border-primary/10 flex items-center gap-4">
                 <div className="w-10 h-10 rounded-xl bg-primary text-white flex items-center justify-center shrink-0">
                    <span className="material-symbols-outlined">lightbulb_circle</span>
                 </div>
                 <p className="text-[11px] font-bold text-on-surface-variant leading-relaxed">Bạn có thể đính kèm đường dẫn mã nguồn hoặc tài liệu PDF để minh chứng cho tiến độ.</p>
              </div>
           </aside>

           {/* Section 2: Main Submission Form */}
           <main className="lg:col-span-8 space-y-8">
              <div className="bg-white dark:bg-slate-900 p-10 rounded-[2.5rem] border border-outline-variant/10 shadow-sm space-y-8 animate-in slide-in-from-right-4 duration-500">
                 <div>
                    <h2 className="text-xl font-black text-on-surface uppercase tracking-tight font-headline">Nội dung báo cáo</h2>
                    <p className="text-[10px] text-outline font-bold mt-1 uppercase tracking-widest">Trình bày tóm tắt các kết quả đã đạt được trong giai đoạn này</p>
                 </div>

                 <div className="space-y-6">
                    <div className="space-y-1.5">
                       <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Tiêu đề báo cáo công việc</label>
                       <input 
                          type="text" 
                          className="w-full px-6 py-4 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-2xl text-sm font-black transition-all duration-300 outline-none" 
                          value={title}
                          onChange={(e) => setTitle(e.target.value)}
                          placeholder="VD: Hoàn thiện thiết kế Database và API Login..."
                       />
                    </div>

                    <div className="space-y-1.5">
                       <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Chi tiết nội dung thực hiện</label>
                       <textarea 
                          className="w-full px-6 py-5 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-3xl text-sm font-medium transition-all duration-300 outline-none min-h-[240px] leading-relaxed" 
                          value={content}
                          onChange={(e) => setContent(e.target.value)}
                          placeholder="Mô tả cụ thể các công việc, những khó khăn gặp phải và hướng giải quyết..."
                       />
                    </div>

                    <div className="pt-6 border-t border-outline-variant/5">
                       <h3 className="text-xs font-black text-outline uppercase tracking-widest mb-4">Tài liệu và tệp đính kèm</h3>
                       <div className="bg-surface-container-low/20 p-6 rounded-[2rem] border border-outline-variant/5 space-y-5">
                          <div className="flex items-center gap-4 bg-white dark:bg-slate-800 p-2 rounded-2xl border border-outline-variant/10 overflow-hidden">
                             <input 
                                type="file" 
                                id="file-upload"
                                className="hidden" 
                                onChange={(e) => setFile(e.target.files?.[0] || null)} 
                             />
                             <label 
                                htmlFor="file-upload"
                                className="px-6 py-3 bg-surface-container text-on-surface-variant rounded-xl text-[10px] font-black uppercase tracking-widest cursor-pointer hover:bg-primary transition-all hover:text-white shrink-0"
                             >
                                <span className="flex items-center gap-2">
                                   <span className="material-symbols-outlined text-sm">attach_file</span>
                                   Chọn tệp tin
                                </span>
                             </label>
                             <div className="flex-grow px-2 overflow-hidden">
                                <span className="text-xs font-black text-outline truncate block">
                                   {file ? file.name : "Chưa chọn tệp nào..."}
                                </span>
                             </div>
                             {file && (
                                <button 
                                   onClick={handleUpload}
                                   disabled={uploading}
                                   className="w-10 h-10 rounded-xl bg-primary text-white flex items-center justify-center shadow-lg shadow-primary/20 hover:scale-105 active:scale-95 transition-all disabled:opacity-30"
                                >
                                   <span className="material-symbols-outlined text-lg">{uploading ? 'sync' : 'cloud_upload'}</span>
                                </button>
                             )}
                          </div>

                          {fileUrl && (
                             <div className="flex items-center justify-between bg-primary/[0.03] p-4 rounded-2xl border border-primary/10 animate-in zoom-in-95 duration-300">
                                <div className="flex items-center gap-3">
                                   <span className="material-symbols-outlined text-primary">verified</span>
                                   <span className="text-[10px] font-black text-primary uppercase tracking-widest">Đã tải lên hệ thống</span>
                                </div>
                                <div className="flex items-center gap-2">
                                   <a 
                                      href={fileUrl} 
                                      target="_blank" 
                                      rel="noreferrer"
                                      className="p-2 text-primary hover:bg-primary/10 rounded-lg transition-all"
                                      title="Xem tệp"
                                   >
                                      <span className="material-symbols-outlined text-lg">visibility</span>
                                   </a>
                                   <button 
                                      onClick={() => { setFileUrl(""); setFile(null); }}
                                      className="p-2 text-error hover:bg-error/10 rounded-lg transition-all"
                                      title="Gỡ tệp"
                                   >
                                      <span className="material-symbols-outlined text-lg">delete</span>
                                   </button>
                                </div>
                             </div>
                          )}

                          <div className="flex items-center gap-2 px-2">
                             <span className="material-symbols-outlined text-outline text-sm">info</span>
                             <span className="text-[9px] font-bold text-outline/50 uppercase tracking-tight">Hỗ trợ PDF, DOCX, ZIP, JPG (Max 50MB)</span>
                          </div>
                       </div>
                    </div>
                 </div>

                 <div className="pt-8 border-t border-outline-variant/5 flex justify-end gap-3">
                    <button 
                       onClick={() => navigate(-1)}
                       className="px-8 py-4 bg-surface-container text-on-surface-variant rounded-2xl font-black text-xs uppercase tracking-widest hover:bg-surface-container-high transition-all"
                    >
                       Hủy bỏ
                    </button>
                    <button 
                       onClick={handleSubmit}
                       disabled={submitting}
                       className="px-12 py-4 bg-primary text-white rounded-2xl font-black text-xs uppercase tracking-[0.2em] shadow-2xl shadow-primary/30 hover:scale-105 active:scale-95 transition-all flex items-center gap-3 disabled:opacity-50"
                    >
                       {submitting ? <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></span> : <span className="material-symbols-outlined">send</span>}
                       Xác nhận nộp báo cáo
                    </button>
                 </div>
              </div>
           </main>
        </div>
      )}
    </div>
  );
}
