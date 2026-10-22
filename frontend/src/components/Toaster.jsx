import { useEffect, useState } from "react";
import { TOAST_EVENT } from "../toast";

const STYLE = {
  success: { box: "bg-secondary-container text-on-secondary-container", icon: "check_circle", iconColor: "text-secondary" },
  danger: { box: "bg-error-container text-on-error-container", icon: "error", iconColor: "text-error" },
  warning: { box: "bg-tertiary-container text-on-tertiary-container", icon: "warning", iconColor: "text-tertiary" },
};

const AUTO_HIDE_MS = 5000;
const MAX_VISIBLE = 4;
let nextId = 1;

export default function Toaster() {
  const [toasts, setToasts] = useState([]);

  useEffect(() => {
    const onToast = (e) => {
      const { type, message } = e.detail || {};
      const id = nextId++;
      setToasts((list) => {
        // Bỏ thông báo trùng đang hiển thị (nhiều request cùng lỗi một lúc)
        if (list.some((t) => t.message === message)) return list;
        return [...list, { id, type: STYLE[type] ? type : "danger", message }].slice(-MAX_VISIBLE);
      });
      setTimeout(() => setToasts((list) => list.filter((t) => t.id !== id)), AUTO_HIDE_MS);
    };
    window.addEventListener(TOAST_EVENT, onToast);
    return () => window.removeEventListener(TOAST_EVENT, onToast);
  }, []);

  if (toasts.length === 0) return null;

  return (
    <div className="fixed top-4 right-4 z-[1000] flex flex-col gap-2 w-[min(24rem,calc(100vw-2rem))]" aria-live="polite">
      {toasts.map((t) => {
        const s = STYLE[t.type];
        return (
          <div key={t.id} role="alert" className={`flex items-start gap-3 px-4 py-3 rounded-2xl shadow-lg border border-black/5 ${s.box}`}>
            <span className={`material-symbols-outlined ${s.iconColor} text-xl`} style={{ fontVariationSettings: "'FILL' 1" }}>
              {s.icon}
            </span>
            <span className="text-sm font-semibold flex-1">{t.message}</span>
            <button
              type="button"
              className="p-1 hover:bg-black/10 rounded-lg transition-colors"
              onClick={() => setToasts((list) => list.filter((x) => x.id !== t.id))}
              aria-label="Đóng"
            >
              <span className="material-symbols-outlined text-lg">close</span>
            </button>
          </div>
        );
      })}
    </div>
  );
}
