import { eventDate, fmtDay, fmtTime, typeOf } from "./calendarUtils";

/** Dạng danh sách theo ngày — dùng trên điện thoại và khi người dùng chọn "Danh sách". */
export default function AgendaList({ events, onSelect }) {
  if (events.length === 0) {
    return (
      <div className="bg-white dark:bg-slate-900 rounded-[2rem] border border-outline-variant/10 p-12 text-center">
        <span className="material-symbols-outlined text-5xl text-outline/40" aria-hidden="true">event_busy</span>
        <p className="mt-3 text-sm font-bold text-outline">Tháng này chưa có sự kiện nào.</p>
      </div>
    );
  }

  const days = [];
  for (const ev of events) {
    const label = fmtDay(eventDate(ev));
    const last = days[days.length - 1];
    if (last && last.label === label) last.items.push(ev);
    else days.push({ label, items: [ev] });
  }

  return (
    <ol className="space-y-4">
      {days.map((day) => (
        <li key={day.label} className="bg-white dark:bg-slate-900 rounded-[1.5rem] border border-outline-variant/10 p-4">
          <h3 className="text-[11px] font-black uppercase tracking-widest text-outline mb-3 first-letter:uppercase">{day.label}</h3>
          <ul className="space-y-2">
            {day.items.map((ev) => (
              <li key={`${ev.type}-${ev.id ?? ev.milestoneId}`}>
                <button type="button" onClick={() => onSelect(ev)}
                  className={`w-full text-left px-3 py-2.5 rounded-xl border-l-4 ${typeOf(ev).chip}`}>
                  <span className="block text-sm font-bold">{ev.title}</span>
                  <span className="block text-[11px] opacity-80 mt-0.5">
                    {typeOf(ev).label} · {fmtTime(eventDate(ev))}
                  </span>
                </button>
              </li>
            ))}
          </ul>
        </li>
      ))}
    </ol>
  );
}
