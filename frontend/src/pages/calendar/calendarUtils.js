// P4-11 — tiện ích thuần cho trang Lịch học thuật (tuần bắt đầu từ Thứ 2, giờ theo trình duyệt).

export const WEEKDAYS = ["Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "Chủ nhật"];

export const EVENT_TYPES = {
  DEADLINE: { label: "Hạn chót", chip: "bg-error-container text-on-error-container border-error", dot: "bg-error" },
  MILESTONE: { label: "Mốc đồ án", chip: "bg-primary/10 text-primary border-primary", dot: "bg-primary" },
  MEETING: { label: "Họp & sự kiện", chip: "bg-secondary-container text-on-secondary-container border-secondary", dot: "bg-secondary" },
};

export const typeOf = (ev) => EVENT_TYPES[ev?.type] || EVENT_TYPES.MEETING;

const pad = (n) => String(n).padStart(2, "0");

/** Khóa ngày "YYYY-MM-DD" theo giờ địa phương. */
export const dayKey = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;

/**
 * Ngày sự kiện hiện trên lịch: hạn chót và mốc rơi vào ngày kết thúc (deadline), buổi họp rơi vào ngày bắt đầu.
 * Trả về null nếu sự kiện không có thời gian.
 */
export function eventDate(ev) {
  const raw = ev?.type === "MEETING" ? ev.startTime || ev.endTime : ev?.endTime || ev?.startTime;
  if (!raw) return null;
  const d = new Date(raw);
  return Number.isNaN(d.getTime()) ? null : d;
}

/** Các ô của lưới tháng: đủ số tuần để chứa cả tháng, mỗi ô { date, inMonth }. */
export function monthGrid(year, month) {
  const first = new Date(year, month, 1);
  const offset = (first.getDay() + 6) % 7; // Thứ 2 = 0
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const weeks = Math.ceil((offset + daysInMonth) / 7);
  return Array.from({ length: weeks * 7 }, (_, i) => {
    const date = new Date(year, month, 1 - offset + i);
    return { date, inMonth: date.getMonth() === month };
  });
}

/** Nhóm sự kiện theo ngày hiển thị; trong mỗi ngày xếp theo giờ. */
export function groupByDay(events) {
  const map = new Map();
  for (const ev of events || []) {
    const d = eventDate(ev);
    if (!d) continue;
    const key = dayKey(d);
    if (!map.has(key)) map.set(key, []);
    map.get(key).push(ev);
  }
  for (const list of map.values()) list.sort((a, b) => eventDate(a) - eventDate(b));
  return map;
}

/** Sự kiện từ đầu ngày `now` trở đi, gần nhất trước, tối đa `limit`. */
export function upcoming(events, now = new Date(), limit = 4) {
  const start = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  return (events || [])
    .filter((ev) => {
      const d = eventDate(ev);
      return d && d >= start;
    })
    .sort((a, b) => eventDate(a) - eventDate(b))
    .slice(0, limit);
}

/** Sự kiện thuộc tháng (year, month), xếp theo thời gian — dùng cho dạng danh sách. */
export function eventsInMonth(events, year, month) {
  return (events || [])
    .filter((ev) => {
      const d = eventDate(ev);
      return d && d.getFullYear() === year && d.getMonth() === month;
    })
    .sort((a, b) => eventDate(a) - eventDate(b));
}

export const fmtTime = (d) => d.toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" });

export const fmtDay = (d) =>
  d.toLocaleDateString("vi-VN", { weekday: "long", day: "2-digit", month: "2-digit", year: "numeric" });

export const monthTitle = (year, month) => `Tháng ${month + 1}, ${year}`;
