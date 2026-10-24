import { describe, expect, it } from "vitest";
import { dayKey, eventDate, eventsInMonth, groupByDay, monthGrid, upcoming } from "./calendarUtils";

describe("monthGrid", () => {
  it("bắt đầu từ Thứ 2 và đủ tuần cho cả tháng", () => {
    // 01/05/2024 là Thứ 4 → 2 ô đầu thuộc tháng 4
    const cells = monthGrid(2024, 4);
    expect(cells).toHaveLength(35);
    expect(dayKey(cells[0].date)).toBe("2024-04-29");
    expect(cells[0].inMonth).toBe(false);
    expect(dayKey(cells[2].date)).toBe("2024-05-01");
    expect(cells.filter((c) => c.inMonth)).toHaveLength(31);
  });

  it("tháng cần 6 tuần", () => {
    // 01/09/2024 là Chủ nhật → ô thứ 7
    expect(monthGrid(2024, 8)).toHaveLength(42);
  });
});

describe("eventDate / groupByDay", () => {
  const meeting = { type: "MEETING", startTime: "2024-05-10T09:00:00", endTime: "2024-05-11T10:00:00" };
  const milestone = { type: "MILESTONE", startTime: "2024-05-01T00:00:00", endTime: "2024-05-08T23:59:00" };

  it("họp theo ngày bắt đầu, mốc theo hạn chót", () => {
    expect(dayKey(eventDate(meeting))).toBe("2024-05-10");
    expect(dayKey(eventDate(milestone))).toBe("2024-05-08");
    expect(eventDate({ type: "DEADLINE" })).toBeNull();
  });

  it("nhóm theo ngày và xếp theo giờ", () => {
    const early = { type: "MEETING", startTime: "2024-05-10T07:00:00" };
    const map = groupByDay([meeting, milestone, early, { type: "DEADLINE" }]);
    expect([...map.keys()].sort()).toEqual(["2024-05-08", "2024-05-10"]);
    expect(map.get("2024-05-10")).toEqual([early, meeting]);
  });
});

describe("upcoming / eventsInMonth", () => {
  const evs = [
    { id: 1, type: "MEETING", startTime: "2024-05-14T08:00:00" },
    { id: 2, type: "DEADLINE", endTime: "2024-05-15T08:00:00" },
    { id: 3, type: "DEADLINE", endTime: "2024-05-20T08:00:00" },
    { id: 4, type: "MEETING", startTime: "2024-06-02T08:00:00" },
  ];

  it("lấy từ đầu hôm nay trở đi, gần nhất trước", () => {
    const now = new Date(2024, 4, 15, 12, 0);
    expect(upcoming(evs, now, 2).map((e) => e.id)).toEqual([2, 3]);
  });

  it("lọc theo tháng", () => {
    expect(eventsInMonth(evs, 2024, 4).map((e) => e.id)).toEqual([1, 2, 3]);
    expect(eventsInMonth(evs, 2024, 5).map((e) => e.id)).toEqual([4]);
  });
});
