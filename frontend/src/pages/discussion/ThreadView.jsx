import { useState } from "react";

const fmt = (v) =>
  v ? new Date(v).toLocaleString("vi-VN", { hour: "2-digit", minute: "2-digit", day: "2-digit", month: "2-digit", year: "numeric" }) : "";

/** Các bài viết của một chủ đề (cũ → mới) và ô trả lời; Ctrl/⌘ + Enter để gửi. */
export default function ThreadView({ thread, posts, myUserId, hasMore, loadingMore, onLoadMore, onSend, sending }) {
  const [content, setContent] = useState("");

  const send = async (e) => {
    e?.preventDefault();
    if (!content.trim() || sending) return;
    if (await onSend(content.trim())) setContent("");
  };

  return (
    <div className="flex flex-col h-full min-h-[24rem]">
      <div className="px-6 py-4 border-b border-outline-variant/10">
        <h2 className="text-lg font-black break-words">{thread.title}</h2>
        <p className="text-xs text-outline">Mở bởi {thread.createdBy?.fullName || "—"} · {fmt(thread.createdAt)}</p>
      </div>

      <div className="flex-1 overflow-y-auto px-6 py-5 space-y-4" aria-live="polite">
        {posts.length === 0 && <p className="text-sm text-outline text-center py-8">Chưa có bài viết nào trong chủ đề này.</p>}
        {posts.map((p) => {
          const mine = p.author?.id === myUserId;
          return (
            <article key={p.id} className={`flex flex-col ${mine ? "items-end" : "items-start"}`}>
              <header className="text-[11px] text-outline mb-1">
                <span className="font-bold text-on-surface">{mine ? "Bạn" : p.author?.fullName || "—"}</span> · <time dateTime={p.createdAt}>{fmt(p.createdAt)}</time>
              </header>
              <p className={`max-w-[85%] px-4 py-3 rounded-2xl text-sm whitespace-pre-line break-words ${
                mine ? "bg-primary text-on-primary rounded-tr-sm" : "bg-surface-container-low text-on-surface rounded-tl-sm"
              }`}>
                {p.content}
              </p>
            </article>
          );
        })}
        {hasMore && (
          <div className="text-center">
            <button type="button" onClick={onLoadMore} disabled={loadingMore}
              className="px-4 py-2 rounded-xl text-xs font-bold border border-outline-variant/20 hover:bg-surface-container disabled:opacity-50">
              {loadingMore ? "Đang tải..." : "Tải thêm bài viết"}
            </button>
          </div>
        )}
      </div>

      <form onSubmit={send} className="border-t border-outline-variant/10 p-4 flex gap-3 items-end">
        <label htmlFor="post-content" className="sr-only">Nội dung trả lời</label>
        <textarea id="post-content" rows={2} value={content} maxLength={5000}
          onChange={(e) => setContent(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter" && (e.ctrlKey || e.metaKey)) send(e);
          }}
          placeholder="Viết trả lời... (Ctrl + Enter để gửi)"
          className="flex-1 min-w-0 resize-y px-4 py-3 bg-surface-container-low rounded-xl text-sm outline-none border border-transparent focus:border-primary" />
        <button type="submit" disabled={sending || !content.trim()}
          className="shrink-0 inline-flex items-center gap-2 px-5 py-3 rounded-xl bg-primary text-on-primary text-sm font-bold disabled:opacity-50">
          {sending ? "Đang gửi..." : "Gửi"}
          <span className="material-symbols-outlined text-lg" aria-hidden="true">send</span>
        </button>
      </form>
    </div>
  );
}
