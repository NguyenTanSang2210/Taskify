import { useEffect, useState, useCallback, useMemo } from "react";
import { registrationApi } from "../api/registrationApi";
import { progressApi } from "../api/progressApi";
import { fileApi } from "../api/fileApi";
import { connectAndSubscribe } from "../ws/stomp";
import { useAuth } from "../context/AuthContext";
import InlineNotice from "../components/InlineNotice";

import { STATUS_CONFIG, STATUS_ORDER } from "./studentProgress/kanbanConfig";
import KanbanColumn from "./studentProgress/KanbanColumn";
import ProgressForm from "./studentProgress/ProgressForm";
import ProgressTable from "./studentProgress/ProgressTable";
import useLoadEffect from '../hooks/useLoadEffect';

// ─── Main Page ───────────────────────────────────────────────────────────────
export default function StudentProgressPage() {
  const { user } = useAuth();
  const userId = user?.id;

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
    if (!userId) return;
    try {
      const data = await registrationApi.getMine(userId);
      const approved = (data || []).filter((r) => r.approved === true);
      if (approved.length > 0) {
        setTopicId(String(approved[0].topic?.id || ""));
      }
    } catch (e) {
      console.error(e);
    }
  }, [userId]);

  useLoadEffect(loadStudentTopic);

  // --- LOAD PROGRESS ---
  const loadProgress = useCallback(async () => {
    if (!topicId) { setItems([]); return; }
    setLoading(true);
    try {
      const arr = await progressApi.listByStudent(userId, topicId);
      setItems(arr || []);
    } catch (e) {
      console.error(e);
      setNotice({ type: "danger", message: "Không tải được dữ liệu tiến độ" });
    } finally {
      setLoading(false);
    }
  }, [topicId, userId]);

  useLoadEffect(loadProgress);

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
    } catch {
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
      if (file) fileUrl = (await fileApi.upload(file)).fileUrl;
      await progressApi.create(userId, topicId, {
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
              <ProgressForm content={content} file={file} handleStudentSubmit={handleStudentSubmit} setContent={setContent} setFile={setFile} setTitle={setTitle} submitting={submitting} title={title} />
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
                <ProgressTable items={items} />
              </div>
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
