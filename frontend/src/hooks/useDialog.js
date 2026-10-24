import { useEffect, useId, useRef } from "react";

const FOCUSABLE = 'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

/**
 * Hành vi hộp thoại (P4-11, a11y): Esc để đóng, focus vào hộp thoại khi mở (ưu tiên phần tử [data-autofocus]),
 * giữ Tab trong hộp thoại và trả focus về phần tử đã mở nó khi đóng.
 * Gắn `panelRef` vào khung hộp thoại cùng role="dialog" aria-modal="true" aria-labelledby={titleId}.
 */
export default function useDialog(onClose) {
  const titleId = useId();
  const panelRef = useRef(null);
  const onCloseRef = useRef(onClose);
  useEffect(() => {
    onCloseRef.current = onClose;
  }, [onClose]);

  // chạy một lần khi mở: onClose đọc qua ref để hàm mới mỗi lần render không làm focus nhảy lại
  useEffect(() => {
    const opener = document.activeElement;
    const panel = panelRef.current;
    (panel?.querySelector("[data-autofocus]") || panel?.querySelector(FOCUSABLE) || panel)?.focus();

    const onKey = (e) => {
      if (e.key === "Escape") {
        e.stopPropagation();
        onCloseRef.current();
        return;
      }
      if (e.key !== "Tab" || !panel) return;
      const items = [...panel.querySelectorAll(FOCUSABLE)];
      if (items.length === 0) return;
      const first = items[0];
      const last = items[items.length - 1];
      if (e.shiftKey && document.activeElement === first) {
        e.preventDefault();
        last.focus();
      } else if (!e.shiftKey && document.activeElement === last) {
        e.preventDefault();
        first.focus();
      }
    };
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("keydown", onKey);
      if (opener && typeof opener.focus === "function") opener.focus();
    };
  }, []);

  return { panelRef, titleId };
}
