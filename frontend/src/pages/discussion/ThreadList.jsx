import { useState } from "react";

const fmt = (v) => (v ? new Date(v).toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" }) : "");

/** Danh sách chủ đề (mới nhất trước) + ô mở chủ đề mới. */
export default function ThreadList({ threads, selectedId, onSelect, onCreate, creating }) {
  const [title, setTitle] = useState("");

  const submit = async (e) => {
    e.preventDefault();
    if (!title.trim()) return;
    if (await onCreate(title.trim())) setTitle("");
  };

  return (
    <div className="flex flex-col gap-4">
      <form onSubmit={submit} className="flex gap-2">
        <label htmlFor="new-thread" className="sr-only">Tiêu đề chủ đề mới</label>
        <input id="new-thread" value={title} onChange={(e) => setTitle(e.target.value)} maxLength={255}
          placeholder="Mở chủ đề mới..."
          className="flex-1 min-w-0 px-4 py-2.5 bg-surface-container-low rounded-xl text-sm outline-none border border-transparent focus:border-primary" />
        <button type="submit" disabled={creating || !title.trim()} aria-label="Tạo chủ đề"
          className="shrink-0 rounded-xl px-3 bg-primary text-on-primary disabled:opacity-50">
          <span className="material-symbols-outlined" aria-hidden="true">add</span>
        </button>
      </form>

      {threads.length === 0 ? (
        <p className="text-sm text-outline px-1">Chưa có chủ đề nào — hãy mở chủ đề đầu tiên.</p>
      ) : (
        <ul className="flex flex-col gap-1.5" aria-label="Các chủ đề thảo luận">
          {threads.map((t) => {
            const active = t.id === selectedId;
            return (
              <li key={t.id}>
                <button type="button" onClick={() => onSelect(t.id)} aria-current={active ? "true" : undefined}
                  className={`w-full text-left rounded-2xl px-4 py-3 transition-colors ${
                    active ? "bg-white dark:bg-slate-800 shadow-sm ring-1 ring-primary/20" : "hover:bg-white/60 dark:hover:bg-slate-800/60"
                  }`}>
                  <span className={`block text-sm font-bold break-words ${active ? "text-primary" : "text-on-surface"}`}>{t.title}</span>
                  <span className="block text-[11px] text-outline mt-0.5">
                    {t.createdBy?.fullName || "—"} · {fmt(t.createdAt)}
                  </span>
                </button>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
