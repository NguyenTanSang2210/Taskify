import { WEEKDAYS, dayKey, fmtTime, eventDate, monthGrid, typeOf } from "./calendarUtils";

const MAX_CHIPS = 3;

/** Lưới tháng (màn hình rộng). Mỗi sự kiện là một nút mở chi tiết; ô hôm nay được đánh dấu. */
export default function MonthGrid({ year, month, byDay, onSelect }) {
  const todayKey = dayKey(new Date());
  const cells = monthGrid(year, month);

  return (
    <section className="bg-white dark:bg-slate-900 rounded-[2rem] border border-outline-variant/10 shadow-sm overflow-hidden"
      aria-label={`Lịch tháng ${month + 1} năm ${year}`}>
      <div className="grid grid-cols-7 bg-surface-container-low" aria-hidden="true">
        {WEEKDAYS.map((w, i) => (
          <div key={w}
            className={`py-3 text-center text-[10px] font-black uppercase tracking-widest ${i === 6 ? "text-error" : "text-outline"}`}>
            {w}
          </div>
        ))}
      </div>
      <div className="grid grid-cols-7">
        {cells.map(({ date, inMonth }, i) => {
          const key = dayKey(date);
          const events = byDay.get(key) || [];
          const isToday = key === todayKey;
          const isSunday = i % 7 === 6;
          return (
            <div key={key}
              className={`min-h-[7.5rem] p-2 border-t border-l border-outline-variant/10 first:border-l-0 [&:nth-child(7n+1)]:border-l-0 ${
                isToday ? "bg-primary/5" : ""
              }`}>
              <div className={`text-sm font-bold mb-1.5 ${
                isToday ? "text-primary" : !inMonth ? "text-outline" : isSunday ? "text-error" : "text-on-surface"
              }`}>
                <span className="sr-only">{WEEKDAYS[i % 7]}, ngày </span>
                {date.getDate()}
                <span className="sr-only"> tháng {date.getMonth() + 1}</span>
                {isToday && <span className="ml-1 text-[10px] font-black uppercase tracking-widest">Hôm nay</span>}
              </div>
              <div className="flex flex-col gap-1">
                {events.slice(0, MAX_CHIPS).map((ev) => (
                  <button key={`${ev.type}-${ev.id ?? ev.milestoneId}`} type="button" onClick={() => onSelect(ev)}
                    className={`text-left text-[11px] font-semibold leading-snug px-2 py-1 rounded-md border-l-[3px] truncate hover:brightness-95 ${typeOf(ev).chip}`}
                    title={ev.title}>
                    {ev.type === "MEETING" && <span className="opacity-70 mr-1">{fmtTime(eventDate(ev))}</span>}
                    {ev.title}
                  </button>
                ))}
                {events.length > MAX_CHIPS && (
                  <button type="button" onClick={() => onSelect(events[MAX_CHIPS])}
                    className="text-left text-[10px] font-black uppercase tracking-widest text-outline hover:text-primary px-2">
                    +{events.length - MAX_CHIPS} sự kiện khác
                  </button>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
}
