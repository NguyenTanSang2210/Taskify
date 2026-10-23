import { vi } from "vitest";

/** jsdom không hỗ trợ điều hướng thật: thay window.location bằng bản giả có assign() ghi lại đích đến. */
export function stubLocation(pathname = "/") {
  const location = { pathname, search: "", href: `http://localhost${pathname}`, assign: vi.fn() };
  Object.defineProperty(window, "location", { value: location, writable: true, configurable: true });
  return location;
}
