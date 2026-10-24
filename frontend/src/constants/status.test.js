import { describe, expect, it } from "vitest";
import { PROGRESS_STATUS, TOPIC_STATUS, WORKSPACE_STATUS, statusLabel, statusOptions } from "./status";

describe("constants/status", () => {
  it("trả nhãn tiếng Việt cho trạng thái đã biết", () => {
    expect(statusLabel(WORKSPACE_STATUS, "IN_PROGRESS")).toBe("Đang thực hiện");
    expect(statusLabel(TOPIC_STATUS, "FULL")).toBe("Đã đủ sinh viên");
  });

  it("giữ nguyên giá trị lạ, rỗng khi không có giá trị", () => {
    expect(statusLabel(PROGRESS_STATUS, "LA")).toBe("LA");
    expect(statusLabel(PROGRESS_STATUS, null)).toBe("");
    expect(statusLabel(PROGRESS_STATUS, undefined)).toBe("");
  });

  it("dựng danh sách lựa chọn đúng thứ tự khai báo", () => {
    expect(statusOptions(PROGRESS_STATUS)).toEqual([
      { value: "TODO", label: "Cần làm" },
      { value: "IN_PROGRESS", label: "Đang thực hiện" },
      { value: "DONE", label: "Hoàn thành" },
    ]);
  });

  it("đủ 6 trạng thái đợt khớp enum backend", () => {
    expect(Object.keys(WORKSPACE_STATUS)).toEqual(["DRAFT", "OPEN_TOPIC", "OPEN_REGISTRATION", "LOCK_REGISTRATION", "IN_PROGRESS", "CLOSED"]);
  });
});
