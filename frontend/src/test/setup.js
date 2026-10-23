// Thiết lập chung cho test (Vitest): matcher DOM (toBeInTheDocument, toBeDisabled...) và dọn DOM sau mỗi test
import "@testing-library/jest-dom/vitest";
import { afterEach } from "vitest";
import { cleanup } from "@testing-library/react";

afterEach(() => {
  cleanup();
  try {
    localStorage.clear();
  } catch { /* jsdom luôn có localStorage */ }
});
