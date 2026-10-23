/** Tách từ StudentProgressPage.jsx (P3-11). */
export default function ProgressForm({ content, file, handleStudentSubmit, setContent, setFile, setTitle, submitting, title }) {
  return (
    <form onSubmit={handleStudentSubmit} className="space-y-5">
      <div className="space-y-1.5">
        <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Tiêu đề báo cáo</label>
        <input
          className="w-full px-4 py-3 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-xl text-sm font-bold transition-all duration-300 outline-none"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="VD: Báo cáo tuần 1: Nghiên cứu tài liệu"
        />
      </div>
      <div className="space-y-1.5">
        <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Nội dung chi tiết</label>
        <textarea
          className="w-full px-4 py-3 bg-surface-container-low border-transparent focus:border-primary focus:ring-0 focus:bg-white rounded-xl text-sm font-bold transition-all duration-300 outline-none min-h-[120px]"
          rows={4}
          value={content}
          onChange={(e) => setContent(e.target.value)}
          placeholder="Mô tả công việc đã hoàn thành, các khó khăn gặp phải..."
        />
      </div>
      <div className="space-y-1.5">
        <label className="text-[10px] font-black uppercase tracking-widest text-outline ml-1">Đính kèm tài liệu</label>
        <div className="relative group">
          <input
            type="file"
            className="absolute inset-0 opacity-0 cursor-pointer z-10"
            onChange={(e) => setFile(e.target.files[0])}
          />
          <div className="w-full px-4 py-4 border-2 border-dashed border-outline-variant/30 group-hover:border-primary/50 group-hover:bg-primary/[0.02] rounded-xl flex items-center justify-center gap-3 transition-all">
            <span className="material-symbols-outlined text-outline group-hover:text-primary">upload_file</span>
            <span className="text-[11px] font-black uppercase tracking-widest text-outline group-hover:text-primary truncate">
              {file ? file.name : "Tải tệp đính kèm"}
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
  );
}
