// Cấu hình bảng Kanban tiến độ của sinh viên (tách từ StudentProgressPage — P3-11)
import { PROGRESS_STATUS } from "../../constants/status";

export const STATUS_CONFIG = {
  TODO: {
    label: PROGRESS_STATUS.TODO,
    icon: "radio_button_unchecked",
    color: "text-outline",
    bg: "bg-surface-container",
    border: "border-outline-variant/20",
    headerBg: "bg-surface-container-high",
    dot: "bg-outline",
    badge: "bg-outline/10 text-outline",
  },
  IN_PROGRESS: {
    label: PROGRESS_STATUS.IN_PROGRESS,
    icon: "autorenew",
    color: "text-secondary",
    bg: "bg-secondary-container/30",
    border: "border-secondary/20",
    headerBg: "bg-secondary/10",
    dot: "bg-secondary",
    badge: "bg-secondary/15 text-secondary",
  },
  DONE: {
    label: PROGRESS_STATUS.DONE,
    icon: "check_circle",
    color: "text-primary",
    bg: "bg-primary/5",
    border: "border-primary/20",
    headerBg: "bg-primary/10",
    dot: "bg-primary",
    badge: "bg-primary/10 text-primary",
  },
};

export const STATUS_ORDER = ["TODO", "IN_PROGRESS", "DONE"];

export const fmtDate = (d) =>
  new Date(d).toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" });

// ─── KanbanCard ─────────────────────────────────────────────────────────────
