import Modal from "../../components/Modal";
import { eventDate, fmtDay, typeOf } from "./calendarUtils";

const fmtDateTime = (v) =>
  v ? new Date(v).toLocaleString("vi-VN", { hour: "2-digit", minute: "2-digit", day: "2-digit", month: "2-digit", year: "numeric" }) : null;

/** Các sự kiện của một ngày. GV xóa được sự kiện lịch; mốc chỉ đọc (sửa ở Không gian làm việc). */
export default function EventDialog({ events, canEdit, deletingId, onDelete, onClose }) {
  const day = eventDate(events[0]);
  return (
    <Modal eyebrow="Sự kiện trong ngày" title={day ? fmtDay(day) : "Sự kiện"} onClose={onClose}>
      <ul className="space-y-4">
        {events.map((ev) => {
          const start = fmtDateTime(ev.startTime);
          const end = fmtDateTime(ev.endTime);
          return (
            <li key={`${ev.type}-${ev.id ?? ev.milestoneId}`} className={`rounded-2xl border-l-4 p-4 ${typeOf(ev).chip}`}>
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <span className="text-[10px] font-black uppercase tracking-widest opacity-80">{typeOf(ev).label}</span>
                  <h3 className="text-base font-bold break-words">{ev.title}</h3>
                </div>
                {canEdit && ev.id != null && (
                  <button type="button" onClick={() => onDelete(ev)} disabled={deletingId === ev.id}
                    className="shrink-0 rounded-xl p-2 text-error hover:bg-error/10 disabled:opacity-50"
                    aria-label={`Xóa sự kiện ${ev.title}`}>
                    <span className="material-symbols-outlined text-xl" aria-hidden="true">delete</span>
                  </button>
                )}
              </div>
              <dl className="mt-2 text-xs space-y-1">
                {start && (
                  <div className="flex gap-2"><dt className="font-bold">Bắt đầu:</dt><dd>{start}</dd></div>
                )}
                {end && end !== start && (
                  <div className="flex gap-2"><dt className="font-bold">{ev.type === "MEETING" ? "Kết thúc:" : "Hạn chót:"}</dt><dd>{end}</dd></div>
                )}
                {ev.topic?.title && (
                  <div className="flex gap-2"><dt className="font-bold">Đề tài:</dt><dd className="break-words">{ev.topic.title}</dd></div>
                )}
              </dl>
              {ev.description && <p className="mt-2 text-sm whitespace-pre-line break-words">{ev.description}</p>}
              {ev.milestoneId != null && (
                <p className="mt-2 text-[11px] opacity-80">Mốc đồ án — xem nộp bài và chấm điểm ở Không gian làm việc.</p>
              )}
            </li>
          );
        })}
      </ul>
    </Modal>
  );
}
