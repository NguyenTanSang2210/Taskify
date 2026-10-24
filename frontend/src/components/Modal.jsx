import useDialog from "../hooks/useDialog";

/** Hộp thoại dùng chung (P4-11, a11y): role="dialog" + aria-modal; hành vi bàn phím/focus ở useDialog. */
export default function Modal({ title, eyebrow, onClose, children, footer, size = "max-w-lg" }) {
  const { panelRef, titleId } = useDialog(onClose);

  return (
    <div className="fixed inset-0 z-[1100] flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-primary/40 backdrop-blur-sm" onClick={onClose} aria-hidden="true"></div>
      <div ref={panelRef} role="dialog" aria-modal="true" aria-labelledby={titleId} tabIndex={-1}
        className={`relative z-10 w-full ${size} max-h-[90vh] flex flex-col overflow-hidden rounded-3xl bg-white shadow-2xl dark:bg-slate-900 outline-none`}>
        <div className="flex items-start justify-between gap-4 border-b border-outline-variant/10 px-6 py-5">
          <div className="min-w-0">
            {eyebrow && <span className="mb-1 block text-[10px] font-black uppercase tracking-widest text-primary">{eyebrow}</span>}
            <h2 id={titleId} className="text-lg font-black text-on-surface break-words">{title}</h2>
          </div>
          <button type="button" onClick={onClose} className="shrink-0 rounded-xl p-2 hover:bg-surface-container-high" aria-label="Đóng">
            <span className="material-symbols-outlined" aria-hidden="true">close</span>
          </button>
        </div>
        <div className="overflow-auto p-6">{children}</div>
        {footer && <div className="border-t border-outline-variant/10 px-6 py-4 flex justify-end gap-3">{footer}</div>}
      </div>
    </div>
  );
}
