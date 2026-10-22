// Thông báo nổi toàn cục. Gọi được từ bất kỳ đâu (kể cả ngoài React, ví dụ interceptor của axios);
// <Toaster /> (gắn ở main.jsx) lắng nghe sự kiện và hiển thị.
export const TOAST_EVENT = "app:toast";

export function showToast(type, message) {
  if (!message) return;
  window.dispatchEvent(new CustomEvent(TOAST_EVENT, { detail: { type, message } }));
}
