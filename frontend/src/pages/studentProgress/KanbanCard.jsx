import { useEffect, useState, useRef } from "react";
import FileDownloadButton from "../../components/FileDownloadButton";
import { STATUS_CONFIG, STATUS_ORDER, fmtDate } from "./kanbanConfig";

export default function KanbanCard({ item, onMove, updating }) {
  const cfg = STATUS_CONFIG[item.status] || STATUS_CONFIG.TODO;
  const [dragging, setDragging] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef(null);

  // Close menu on outside click
  useEffect(() => {
    const handler = (e) => {
      if (menuRef.current && !menuRef.current.contains(e.target)) setMenuOpen(false);
    };
    if (menuOpen) document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, [menuOpen]);

  const nextStatuses = STATUS_ORDER.filter((s) => s !== item.status);

  return (
    <div
      draggable
      onDragStart={(e) => {
        e.dataTransfer.setData("progressId", String(item.id));
        e.dataTransfer.setData("currentStatus", item.status);
        setDragging(true);
      }}
      onDragEnd={() => setDragging(false)}
      className={`
        relative group rounded-2xl border p-5 cursor-grab active:cursor-grabbing
        transition-all duration-300 select-none
        ${cfg.bg} ${cfg.border}
        ${dragging ? "opacity-40 scale-95 rotate-1 shadow-2xl" : "hover:shadow-md hover:-translate-y-1 shadow-sm"}
        ${updating ? "opacity-60 pointer-events-none" : ""}
      `}
    >
      {/* Status dot + Move button */}
      <div className="flex items-start justify-between mb-3">
        <div className={`w-2 h-2 rounded-full mt-1 ${cfg.dot} shrink-0`} />
        <div className="relative" ref={menuRef}>
          <button
            onClick={(e) => { e.stopPropagation(); setMenuOpen(!menuOpen); }}
            className={`
              w-7 h-7 rounded-lg flex items-center justify-center transition-all
              opacity-0 group-hover:opacity-100
              bg-transparent hover:bg-black/5
            `}
            title="Đổi trạng thái"
          >
            <span className="material-symbols-outlined text-[18px] text-on-surface-variant">arrow_forward</span>
          </button>
          {menuOpen && (
            <div className="absolute right-0 top-8 z-50 bg-white dark:bg-slate-800 rounded-2xl shadow-2xl border border-outline-variant/10 overflow-hidden min-w-[180px] animate-in fade-in duration-200">
              <p className="text-[9px] font-black uppercase tracking-widest text-outline px-4 pt-3 pb-1">Chuyển sang</p>
              {nextStatuses.map((s) => {
                const c = STATUS_CONFIG[s];
                return (
                  <button
                    key={s}
                    onClick={() => { setMenuOpen(false); onMove(item.id, s); }}
                    className="w-full flex items-center gap-3 px-4 py-3 hover:bg-surface-container-low transition-colors text-left"
                  >
                    <span className={`material-symbols-outlined text-[18px] ${c.color}`}>{c.icon}</span>
                    <span className="text-xs font-bold text-on-surface">{c.label}</span>
                  </button>
                );
              })}
            </div>
          )}
        </div>
      </div>

      {/* Content */}
      <h3 className="text-sm font-black text-on-surface leading-snug uppercase tracking-tight line-clamp-2">
        {item.title}
      </h3>
      {item.content && (
        <p className="text-[11px] text-on-surface-variant mt-2 leading-relaxed line-clamp-3">
          {item.content}
        </p>
      )}

      {/* Footer */}
      <div className="flex items-center justify-between mt-4 pt-3 border-t border-outline-variant/10">
        <span className="text-[10px] font-bold text-outline">{fmtDate(item.createdAt)}</span>
        <div className="flex items-center gap-2">
          {item.fileUrl && (
            <FileDownloadButton
              fileUrl={item.fileUrl}
              className="text-primary hover:text-primary/70 transition-colors"
              title="Tài liệu đính kèm"
            >
              <span className="material-symbols-outlined text-[18px]">attach_file</span>
            </FileDownloadButton>
          )}
          {item.lecturerComment && (
            <span className="material-symbols-outlined text-[18px] text-secondary" title={item.lecturerComment}>
              rate_review
            </span>
          )}
          {updating && (
            <div className="w-4 h-4 border-2 border-primary border-t-transparent rounded-full animate-spin" />
          )}
        </div>
      </div>

      {/* Lecturer comment preview */}
      {item.lecturerComment && (
        <div className="mt-3 p-3 bg-surface-container-low rounded-xl border border-outline-variant/10">
          <p className="text-[10px] font-black text-secondary uppercase tracking-widest mb-1">Phản hồi GV</p>
          <p className="text-[11px] text-on-surface-variant line-clamp-2 leading-relaxed">{item.lecturerComment}</p>
        </div>
      )}
    </div>
  );
}

// ─── KanbanColumn ───────────────────────────────────────────────────────────
