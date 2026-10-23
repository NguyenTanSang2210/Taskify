/** Tách từ ProjectSpacePage.jsx (P3-11). */
export default function ProgressSubmitPanel({ content, file, handleStudentSubmit, selectedLecturerTopic, setContent, setFile, setTitle, studentCanSubmit, submitting, title }) {
  return (
    <aside className="lg:col-span-4 space-y-8 animate-in slide-in-from-left-4 duration-500">
       <div className="bg-white dark:bg-slate-900 p-8 rounded-[2.5rem] border border-outline-variant/10 shadow-sm">
          <div className="flex items-center gap-4 mb-8">
             <div className="w-12 h-12 rounded-2xl bg-primary/5 text-primary flex items-center justify-center">
                <span className="material-symbols-outlined text-2xl">post_add</span>
             </div>
             <div>
                <h2 className="text-sm font-black text-on-surface uppercase tracking-widest">Gửi báo cáo</h2>
                <p className="text-[10px] text-outline font-bold mt-0.5 uppercase tracking-tight">Cập nhật tiến trình mới</p>
             </div>
          </div>
    
          {!studentCanSubmit ? (
            <div className="text-center py-10 space-y-4 opacity-40">
               <span className="material-symbols-outlined text-5xl">lock</span>
               <p className="text-xs font-bold text-outline leading-relaxed uppercase tracking-[0.2em]">Cần có đề tài được phê duyệt để bắt đầu nộp báo cáo.</p>
            </div>
          ) : (
            <form onSubmit={handleStudentSubmit} className="space-y-6">
              <div className="space-y-1.5">
                 <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Tiêu đề báo cáo</label>
                 <input
                   className="w-full px-4 py-3 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-xl text-sm font-bold transition-all duration-300 outline-none"
                   value={title}
                   onChange={(e) => setTitle(e.target.value)}
                   placeholder="VD: Báo cáo tuần 1: Xây dựng Database"
                   required
                 />
              </div>
              <div className="space-y-1.5">
                 <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Nội dung thực hiện</label>
                 <textarea
                   className="w-full px-4 py-3 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-xl text-sm font-bold transition-all duration-300 outline-none min-h-[160px]"
                   rows={5}
                   value={content}
                   onChange={(e) => setContent(e.target.value)}
                   placeholder="Mô tả các đầu việc đã hoàn thành và khó khăn gặp phải..."
                 />
              </div>
              <div className="space-y-1.5">
                 <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Tài liệu đính kèm</label>
                 <div className="relative group">
                    <input type="file" className="absolute inset-0 opacity-0 cursor-pointer z-10" onChange={(e) => setFile(e.target.files[0])} />
                    <div className="w-full px-4 py-4 border-2 border-dashed border-outline-variant/30 group-hover:border-primary group-hover:bg-primary/[0.02] rounded-xl flex items-center justify-center gap-3 transition-all">
                       <span className="material-symbols-outlined text-outline group-hover:text-primary">upload_file</span>
                       <span className="text-[10px] font-black uppercase tracking-widest text-outline group-hover:text-primary truncate">
                          {file ? file.name : "Tải tệp tin hỗ trợ báo cáo"}
                       </span>
                    </div>
                 </div>
              </div>
              <button 
                 className="w-full py-4 bg-primary text-white rounded-xl font-black text-sm uppercase tracking-widest shadow-xl shadow-primary/20 hover:scale-[1.02] active:scale-95 transition-all disabled:opacity-50" 
                 type="submit" 
                 disabled={submitting}
              >
                {submitting ? "Đang xử lý..." : "Xác nhận gửi báo cáo"}
              </button>
            </form>
          )}
       </div>
    
       <div className="bg-secondary-container/20 p-6 rounded-[2rem] border border-secondary/10">
          <div className="flex items-center gap-3 mb-2">
             <span className="material-symbols-outlined text-secondary">verified_user</span>
             <p className="text-[10px] font-black uppercase tracking-widest text-secondary">Đề tài hiện tại</p>
          </div>
          <p className="text-xs font-bold text-on-secondary-container leading-relaxed">
             {selectedLecturerTopic ? selectedLecturerTopic.title : "Đang tải tên đề tài..."}
          </p>
       </div>
    </aside>
  );
}
