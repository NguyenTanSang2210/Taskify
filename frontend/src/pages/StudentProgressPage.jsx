import React, { useEffect, useState, useCallback, useMemo, useRef } from "react";
import { registrationApi } from "../api/registrationApi";
import { progressApi } from "../api/progressApi";
import { fileApi } from "../api/fileApi";
import { connectAndSubscribe } from "../ws/stomp";
import { useAuth } from "../context/AuthContext";
import InlineNotice from "../components/InlineNotice";

// ─── Helpers ────────────────────────────────────────────────────────────────
const STATUS_CONFIG = {
  TODO: {
    label: "Cần làm",
    icon: "radio_button_unchecked",
    color: "text-outline",
    bg: "bg-surface-container",
    border: "border-outline-variant/20",
    headerBg: "bg-surface-container-high",
    dot: "bg-outline",
    badge: "bg-outline/10 text-outline",
  },
  IN_PROGRESS: {
    label: "Đang thực hiện",
    icon: "autorenew",
    color: "text-secondary",
    bg: "bg-secondary-container/30",
    border: "border-secondary/20",
    headerBg: "bg-secondary/10",
    dot: "bg-secondary",
    badge: "bg-secondary/15 text-secondary",
  },
  DONE: {
    label: "Hoàn thành",
    icon: "check_circle",
    color: "text-primary",
    bg: "bg-primary/5",
    border: "border-primary/20",
    headerBg: "bg-primary/10",
    dot: "bg-primary",
    badge: "bg-primary/10 text-primary",
  },
};

const STATUS_ORDER = ["TODO", "IN_PROGRESS", "DONE"];

const fmtDate = (d) =>
  new Date(d).toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" });

// ─── KanbanCard ─────────────────────────────────────────────────────────────
function KanbanCard({ item, onMove, updating }) {
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
            <a
              href={item.fileUrl}
              target="_blank"
              rel="noreferrer"
              onClick={(e) => e.stopPropagation()}
              className="text-primary hover:text-primary/70 transition-colors"
              title="Tài liệu đính kèm"
            >
              <span className="material-symbols-outlined text-[18px]">attach_file</span>
            </a>
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
function KanbanColumn({ status, cards, onMove, updatingId }) {
  const cfg = STATUS_CONFIG[status];
  const [dragOver, setDragOver] = useState(false);

  const handleDragOver = (e) => {
    e.preventDefault();
    setDragOver(true);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setDragOver(false);
    const id = Number(e.dataTransfer.getData("progressId"));
    const currentStatus = e.dataTransfer.getData("currentStatus");
    if (id && currentStatus !== status) {
      onMove(id, status);
    }
  };

  return (
    <div
      onDragOver={handleDragOver}
      onDragLeave={() => setDragOver(false)}
      onDrop={handleDrop}
      className={`
        flex flex-col rounded-[1.75rem] border-2 transition-all duration-200 min-h-[400px]
        ${dragOver
          ? `${cfg.border} bg-primary/5 shadow-lg shadow-primary/10 scale-[1.01]`
          : "border-transparent bg-surface-container-low/50"
        }
      `}
    >
      {/* Column Header */}
      <div className={`flex items-center justify-between px-5 py-4 rounded-2xl mb-3 ${cfg.headerBg}`}>
        <div className="flex items-center gap-3">
          <div className={`w-2.5 h-2.5 rounded-full ${cfg.dot}`} />
          <span className={`text-[11px] font-black uppercase tracking-widest ${cfg.color}`}>
            {cfg.label}
          </span>
        </div>
        <span className={`text-[10px] font-black px-2.5 py-0.5 rounded-full ${cfg.badge}`}>
          {cards.length}
        </span>
      </div>

      {/* Drop zone visual */}
      {dragOver && (
        <div className={`mx-2 mb-3 rounded-xl border-2 border-dashed ${cfg.border} h-20 flex items-center justify-center`}>
          <span className={`text-[10px] font-black uppercase tracking-widest ${cfg.color} opacity-60`}>
            Thả vào đây
          </span>
        </div>
      )}

      {/* Cards */}
      <div className="flex-1 px-2 pb-4 space-y-3 overflow-y-auto">
        {cards.length === 0 && !dragOver ? (
          <div className="flex flex-col items-center justify-center h-40 opacity-30">
            <span className={`material-symbols-outlined text-4xl ${cfg.color}`}>{cfg.icon}</span>
            <p className="text-[10px] font-black uppercase tracking-widest text-outline mt-2">Trống</p>
          </div>
        ) : (
          cards.map((item) => (
            <KanbanCard
              key={item.id}
              item={item}
              onMove={onMove}
              updating={updatingId === item.id}
            />
          ))
        )}
      </div>
    </div>
  );
}

// ─── Main Page ───────────────────────────────────────────────────────────────
export default function StudentProgressPage() {
  const { user } = useAuth();

  const [topicId, setTopicId] = useState("");
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);
  const [notice, setNotice] = useState(null);
  const [viewMode, setViewMode] = useState("kanban"); // 'list' | 'kanban'
  const [updatingId, setUpdatingId] = useState(null);

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

  useEffect(() => { loadStudentTopic(); }, [loadStudentTopic]);

  // --- LOAD PROGRESS ---
  const loadProgress = useCallback(async () => {
    if (!topicId) { setItems([]); return; }
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

  useEffect(() => { loadProgress(); }, [loadProgress]);

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
        if (type === "PROGRESS_CREATED") {
          setItems((prev) => {
            const exists = prev.find((x) => x.id === pr.id);
            return exists ? prev : [pr, ...prev];
          });
        }
      },
    });
    return () => client?.deactivate?.();
  }, [topicId]);

  // --- MOVE CARD (Kanban) ---
  const handleMove = useCallback(async (id, newStatus) => {
    setUpdatingId(id);
    // Optimistic update
    setItems((prev) => prev.map((x) => x.id === id ? { ...x, status: newStatus } : x));
    try {
      await progressApi.updateStatus(id, newStatus);
      setNotice({ type: "success", message: `Đã chuyển sang "${STATUS_CONFIG[newStatus].label}"` });
    } catch (e) {
      // Rollback on failure
      loadProgress();
      setNotice({ type: "danger", message: "Cập nhật trạng thái thất bại" });
    } finally {
      setUpdatingId(null);
    }
  }, [loadProgress]);

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
      if (file) fileUrl = await fileApi.upload(file);
      await progressApi.create(user.id, topicId, {
        title: title.trim(),
        content: content.trim(),
        fileUrl,
      });
      setNotice({ type: "success", message: "Gửi báo cáo tiến độ thành công!" });
      setTitle(""); setContent(""); setFile(null);
      loadProgress();
    } catch (err) {
      console.error(err);
      setNotice({ type: "danger", message: "Lỗi hệ thống khi gửi báo cáo." });
    } finally {
      setSubmitting(false);
    }
  };

  // --- KANBAN DATA ---
  const kanbanData = useMemo(() => ({
    TODO: items.filter((x) => x.status === "TODO"),
    IN_PROGRESS: items.filter((x) => x.status === "IN_PROGRESS"),
    DONE: items.filter((x) => x.status === "DONE"),
  }), [items]);

  const donePercent = items.length > 0
    ? Math.round((kanbanData.DONE.length / items.length) * 100)
    : 0;

  return (
    <div className="space-y-8 animate-in fade-in duration-500 pb-12">
      {/* ── Header ── */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">
        <div>
          <span className="text-[10px] font-black uppercase tracking-[0.2em] text-primary mb-2 block">
            Nhật ký nghiên cứu
          </span>
          <h1 className="text-3xl font-black text-on-surface tracking-tight font-headline uppercase leading-none">
            Theo dõi tiến độ
          </h1>
          <p className="text-sm text-outline mt-2 font-medium">
            Ghi lại các mốc quan trọng, nộp báo cáo tuần và nhận phản hồi từ giảng viên hướng dẫn.
          </p>
        </div>

        {/* View Toggle */}
        <div className="flex items-center gap-3">
          <div className="bg-surface-container-high p-1 rounded-xl flex items-center gap-1">
            <button
              onClick={() => setViewMode("kanban")}
              className={`flex items-center gap-2 px-4 py-2 rounded-xl text-[11px] font-black uppercase tracking-widest transition-all duration-200 ${
                viewMode === "kanban"
                  ? "bg-primary text-white shadow-lg shadow-primary/20"
                  : "text-outline hover:text-on-surface"
              }`}
            >
              <span className="material-symbols-outlined text-[16px]">view_kanban</span>
              Kanban
            </button>
            <button
              onClick={() => setViewMode("list")}
              className={`flex items-center gap-2 px-4 py-2 rounded-xl text-[11px] font-black uppercase tracking-widest transition-all duration-200 ${
                viewMode === "list"
                  ? "bg-primary text-white shadow-lg shadow-primary/20"
                  : "text-outline hover:text-on-surface"
              }`}
            >
              <span className="material-symbols-outlined text-[16px]">view_list</span>
              Danh sách
            </button>
          </div>
          <button
            onClick={loadProgress}
            className="p-2.5 bg-surface-container text-on-surface-variant rounded-xl hover:bg-surface-container-high transition-all"
          >
            <span className="material-symbols-outlined">refresh</span>
          </button>
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
        {/* ── Sidebar: Create Form ── */}
        <aside className="lg:col-span-4 space-y-6">
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
                <p className="text-xs font-bold text-outline leading-relaxed uppercase tracking-widest">
                  Bạn chưa tham gia đề tài nghiên cứu nào được phê duyệt.
                </p>
              </div>
            ) : (
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
            )}
          </div>

          {/* Progress stats mini */}
          {items.length > 0 && (
            <div className="bg-white dark:bg-slate-900 p-6 rounded-[2rem] border border-outline-variant/10 shadow-sm space-y-4">
              <p className="text-[10px] font-black uppercase tracking-widest text-outline">Tổng tiến độ đề tài</p>
              <div className="flex items-end gap-3">
                <span className="text-3xl font-black text-primary">{donePercent}%</span>
                <span className="text-xs font-bold text-outline mb-1">hoàn thành</span>
              </div>
              <div className="w-full bg-surface-container-high rounded-full h-2.5 overflow-hidden">
                <div
                  className="h-full bg-primary rounded-full transition-all duration-700"
                  style={{ width: `${donePercent}%` }}
                />
              </div>
              <div className="grid grid-cols-3 gap-2 pt-2">
                {STATUS_ORDER.map((s) => {
                  const c = STATUS_CONFIG[s];
                  return (
                    <div key={s} className={`rounded-xl p-3 text-center ${c.headerBg}`}>
                      <p className={`text-xl font-black ${c.color}`}>{kanbanData[s].length}</p>
                      <p className="text-[9px] font-black uppercase tracking-widest text-outline mt-0.5">{c.label}</p>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          <div className="bg-secondary/5 p-6 rounded-[2rem] border border-secondary/10 flex items-center gap-4">
            <div className="w-10 h-10 rounded-xl bg-secondary text-white flex items-center justify-center shrink-0">
              <span className="material-symbols-outlined">lightbulb</span>
            </div>
            <div>
              <p className="text-[10px] font-black uppercase tracking-[0.1em] text-secondary">Mẹo học thuật</p>
              <p className="text-xs font-bold text-on-surface-variant mt-0.5 leading-relaxed">
                Kéo thả Card để cập nhật trạng thái nhanh trong chế độ Kanban.
              </p>
            </div>
          </div>
        </aside>

        {/* ── Main Content ── */}
        <section className="lg:col-span-8">
          {loading ? (
            <div className="flex flex-col items-center justify-center min-h-[400px] bg-white dark:bg-slate-900 rounded-[2.5rem] border border-outline-variant/10">
              <div className="animate-spin rounded-full h-10 w-10 border-4 border-primary border-t-transparent mb-4" />
              <p className="text-[10px] font-black uppercase tracking-widest text-outline">Đang tải dữ liệu...</p>
            </div>
          ) : viewMode === "kanban" ? (
            /* ─── KANBAN VIEW ─── */
            <div className="space-y-4">
              <div className="flex items-center justify-between px-1">
                <h2 className="text-lg font-black text-on-surface uppercase tracking-tight">
                  Bảng Kanban
                </h2>
                <span className="text-[10px] font-bold text-outline uppercase tracking-widest flex items-center gap-1.5">
                  <span className="material-symbols-outlined text-[14px]">open_with</span>
                  Kéo thả để cập nhật trạng thái
                </span>
              </div>
              {items.length === 0 ? (
                <div className="flex flex-col items-center justify-center min-h-[400px] bg-white dark:bg-slate-900 rounded-[2.5rem] border border-outline-variant/10 opacity-50">
                  <span className="material-symbols-outlined text-6xl mb-4">view_kanban</span>
                  <h3 className="text-lg font-black uppercase tracking-widest">Bảng trống</h3>
                  <p className="text-xs font-bold mt-2 text-outline">Tạo báo cáo đầu tiên để bắt đầu.</p>
                </div>
              ) : (
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  {STATUS_ORDER.map((s) => (
                    <KanbanColumn
                      key={s}
                      status={s}
                      cards={kanbanData[s]}
                      onMove={handleMove}
                      updatingId={updatingId}
                    />
                  ))}
                </div>
              )}
            </div>
          ) : (
            /* ─── LIST VIEW ─── */
            <div className="bg-white dark:bg-slate-900 rounded-[2.5rem] border border-outline-variant/10 shadow-sm overflow-hidden min-h-[500px] flex flex-col">
              <div className="px-8 py-6 border-b border-outline-variant/5 bg-surface-container-low/30 flex justify-between items-center">
                <div>
                  <h2 className="text-xl font-black text-on-surface uppercase tracking-tight font-headline">Lịch sử báo cáo</h2>
                  <p className="text-[10px] text-outline font-bold mt-1 uppercase tracking-widest">
                    Danh sách các cập nhật và phản hồi từ giảng viên
                  </p>
                </div>
                <span className="text-xs font-black text-outline">{items.length} BÁO CÁO</span>
              </div>
              <div className="overflow-x-auto flex-grow">
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
                                <a href={it.fileUrl} target="_blank" rel="noreferrer"
                                  className="inline-flex items-center gap-1.5 mt-3 text-[10px] font-black text-primary uppercase tracking-widest hover:underline">
                                  <span className="material-symbols-outlined text-sm">attach_file</span>Tài liệu đính kèm
                                </a>
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
              </div>
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
