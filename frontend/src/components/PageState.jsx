// P4-11 — trạng thái đang tải / rỗng / lỗi thống nhất giữa các trang.

const box = "bg-white dark:bg-slate-900 rounded-[2rem] border border-outline-variant/10 p-10 md:p-12 text-center";

export function LoadingState({ label = "Đang tải dữ liệu..." }) {
  return (
    <div className={box} role="status">
      <div className="mx-auto h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" aria-hidden="true"></div>
      <p className="mt-4 text-sm font-bold text-outline">{label}</p>
    </div>
  );
}

export function EmptyState({ icon = "inbox", title, children, action }) {
  return (
    <div className={box}>
      <span className="material-symbols-outlined text-5xl text-outline/50" aria-hidden="true">{icon}</span>
      <p className="mt-3 text-base font-black text-on-surface">{title}</p>
      {children && <p className="mt-1 text-sm text-on-surface-variant max-w-md mx-auto">{children}</p>}
      {action && <div className="mt-5">{action}</div>}
    </div>
  );
}

export function ErrorState({ message, onRetry }) {
  return (
    <div className={box} role="alert">
      <span className="material-symbols-outlined text-5xl text-error" aria-hidden="true">error</span>
      <p className="mt-3 text-base font-black text-on-surface">Không tải được dữ liệu</p>
      {message && <p className="mt-1 text-sm text-on-surface-variant">{message}</p>}
      {onRetry && (
        <button type="button" onClick={onRetry}
          className="mt-5 inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-primary text-on-primary text-sm font-bold">
          <span className="material-symbols-outlined text-lg" aria-hidden="true">refresh</span>
          Thử lại
        </button>
      )}
    </div>
  );
}
