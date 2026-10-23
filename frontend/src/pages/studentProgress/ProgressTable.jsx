import { STATUS_CONFIG, fmtDate } from "./kanbanConfig";
import FileDownloadButton from "../../components/FileDownloadButton";

/** Tách từ StudentProgressPage.jsx (P3-11). */
export default function ProgressTable({ items }) {
  return (
    <table className="w-full text-left border-collapse">
      <thead>
        <tr className="bg-surface-container-low/10">
          <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline" style={{ width: "18%" }}>Thời gian</th>
          <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline" style={{ width: "27%" }}>Nội dung báo cáo</th>
          <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline text-center" style={{ width: "15%" }}>Trạng thái</th>
          <th className="px-8 py-5 text-[10px] uppercase tracking-widest font-black text-outline" style={{ width: "40%" }}>Phản hồi từ Giảng viên</th>
        </tr>
      </thead>
      <tbody className="divide-y divide-outline-variant/5">
        {items.length === 0 ? (
          <tr>
            <td colSpan="4" className="py-32 text-center opacity-40">
              <span className="material-symbols-outlined text-6xl mb-4">history_toggle_off</span>
              <h3 className="text-lg font-black uppercase tracking-widest">Trống</h3>
              <p className="text-xs font-bold mt-2 uppercase tracking-tight">Cần nộp báo cáo đầu tiên để bắt đầu theo dõi.</p>
            </td>
          </tr>
        ) : (
          items.map((it) => {
            const cfg = STATUS_CONFIG[it.status] || STATUS_CONFIG.TODO;
            return (
              <tr key={it.id} className="group hover:bg-primary/[0.01] transition-colors">
                <td className="px-8 py-6">
                  <p className="text-xs font-black text-on-surface">{fmtDate(it.createdAt)}</p>
                  <p className="text-[10px] font-bold text-outline mt-1 uppercase">
                    {new Date(it.createdAt).toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" })}
                  </p>
                </td>
                <td className="px-8 py-6">
                  <p className="text-sm font-black text-on-surface uppercase tracking-tight group-hover:text-primary transition-colors">{it.title}</p>
                  <p className="text-[11px] text-outline mt-1.5 font-medium line-clamp-2 leading-relaxed">{it.content || "Không có mô tả chi tiết."}</p>
                  {it.fileUrl && (
                    <FileDownloadButton fileUrl={it.fileUrl}
                      className="inline-flex items-center gap-1.5 mt-3 text-[10px] font-black text-primary uppercase tracking-widest hover:underline">
                      <span className="material-symbols-outlined text-sm">attach_file</span>Tài liệu đính kèm
                    </FileDownloadButton>
                  )}
                </td>
                <td className="px-8 py-6 text-center">
                  <div className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-[10px] font-black uppercase tracking-widest ${cfg.badge}`}>
                    <span className={`material-symbols-outlined text-[13px]`}>{cfg.icon}</span>
                    {cfg.label}
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
                          Hạn chót tiếp theo: {fmtDate(it.deadline)}
                        </div>
                      )}
                    </div>
                  ) : (
                    <span className="text-[10px] font-black uppercase tracking-widest text-outline/30 italic">Đang chờ phản hồi...</span>
                  )}
                </td>
              </tr>
            );
          })
        )}
      </tbody>
    </table>
  );
}
