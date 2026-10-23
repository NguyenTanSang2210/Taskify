import { describe, expect, it, vi } from "vitest";
import { TOAST_EVENT, showToast } from "./toast";

describe("toast", () => {
  it("phát sự kiện toàn cục kèm loại và nội dung", () => {
    const listener = vi.fn();
    window.addEventListener(TOAST_EVENT, listener);
    showToast("warning", "Bạn không có quyền");
    window.removeEventListener(TOAST_EVENT, listener);
    expect(listener).toHaveBeenCalledTimes(1);
    expect(listener.mock.calls[0][0].detail).toEqual({ type: "warning", message: "Bạn không có quyền" });
  });

  it("bỏ qua thông báo rỗng", () => {
    const listener = vi.fn();
    window.addEventListener(TOAST_EVENT, listener);
    showToast("danger", "");
    window.removeEventListener(TOAST_EVENT, listener);
    expect(listener).not.toHaveBeenCalled();
  });
});
