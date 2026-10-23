import { useSyncExternalStore } from "react";

/** true khi media query khớp (cập nhật khi đổi kích thước cửa sổ) — thay cho đọc window.innerWidth trong render. */
export function useMediaQuery(query) {
  return useSyncExternalStore(
    (onChange) => {
      const mql = window.matchMedia(query);
      mql.addEventListener("change", onChange);
      return () => mql.removeEventListener("change", onChange);
    },
    () => window.matchMedia(query).matches,
    () => false
  );
}
