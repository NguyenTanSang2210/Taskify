export const fmtDeadline = (iso) => (iso ? new Date(iso).toLocaleString("vi-VN", { dateStyle: "short", timeStyle: "short" }) : "—");

/** Trạng thái nộp của một ô: chưa nộp / đúng hạn / trễ hạn (trễ hạn chỉ khi lần nộp đầu sau hạn chót). */
export function submissionState(cell, milestone, now = new Date()) {
    if (cell?.submittedAt) return cell.late ? { label: "Nộp trễ", tone: "bg-tertiary-container text-on-tertiary-container" } : { label: "Đúng hạn", tone: "bg-green-500/10 text-green-700" };
    const overdue = milestone?.deadline && new Date(milestone.deadline) < now;
    return overdue ? { label: "Quá hạn, chưa nộp", tone: "bg-error/10 text-error" } : { label: "Chưa nộp", tone: "bg-surface-container text-outline" };
}

/** Chuỗi datetime-local (yyyy-MM-ddTHH:mm) từ ISO của API. */
export const toLocalInput = (iso) => (iso ? iso.slice(0, 16) : "");
