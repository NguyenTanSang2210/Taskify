import { useCallback, useMemo, useState } from "react";
import { calendarApi } from "../api/calendarApi";
import InlineNotice from "../components/InlineNotice";
import TopicPicker from "../components/TopicPicker";
import { EmptyState, ErrorState, LoadingState } from "../components/PageState";
import useLoadEffect from "../hooks/useLoadEffect";
import useMyTopics from "../hooks/useMyTopics";
import { useMediaQuery } from "../hooks/useMediaQuery";
import MonthGrid from "./calendar/MonthGrid";
import AgendaList from "./calendar/AgendaList";
import EventDialog from "./calendar/EventDialog";
import EventForm from "./calendar/EventForm";
import { EVENT_TYPES, dayKey, eventDate, eventsInMonth, fmtDay, groupByDay, monthTitle, typeOf, upcoming } from "./calendar/calendarUtils";

const navBtn = "rounded-xl p-2 hover:bg-surface-container text-on-surface";
const tabBtn = (active) =>
  `px-4 py-2 rounded-xl text-sm font-bold transition-colors ${active ? "bg-white dark:bg-slate-800 text-primary shadow-sm" : "text-outline hover:text-on-surface"}`;

/** P4-11 — Lịch học thuật: sự kiện + mốc của đề tài, dạng tháng hoặc danh sách; GV thêm/xóa sự kiện. */
export default function CalendarPage() {
  const { topics, topicId, setTopicId, loading: topicsLoading, error: topicsError, isLecturer } = useMyTopics();
  const wide = useMediaQuery("(min-width: 768px)");

  const now = new Date();
  const [cursor, setCursor] = useState({ year: now.getFullYear(), month: now.getMonth() });
  const [view, setView] = useState("month");
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState(null);
  const [selectedKey, setSelectedKey] = useState(null);
  const [formOpen, setFormOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState("");
  const [deletingId, setDeletingId] = useState(null);

  const load = useCallback(async () => {
    if (!topicId) {
      setEvents([]);
      return;
    }
    setLoading(true);
    setError("");
    try {
      setEvents((await calendarApi.list(topicId)) || []);
    } catch (e) {
      setError(e.message || "Không tải được lịch.");
    } finally {
      setLoading(false);
    }
  }, [topicId]);
  useLoadEffect(load);

  const byDay = useMemo(() => groupByDay(events), [events]);
  const monthEvents = useMemo(() => eventsInMonth(events, cursor.year, cursor.month), [events, cursor]);
  const soon = useMemo(() => upcoming(events), [events]);
  const topicTitle = topics.find((t) => String(t.id) === topicId)?.title || "";
  const showList = view === "list" || !wide;

  const shift = (delta) =>
    setCursor(({ year, month }) => {
      const d = new Date(year, month + delta, 1);
      return { year: d.getFullYear(), month: d.getMonth() };
    });
  const goToday = () => setCursor({ year: now.getFullYear(), month: now.getMonth() });
  const select = (ev) => setSelectedKey(dayKey(eventDate(ev)));

  const create = async (payload) => {
    setSaving(true);
    setFormError("");
    try {
      await calendarApi.create({ ...payload, topicId: Number(topicId) });
      setFormOpen(false);
      setNotice({ type: "success", message: "Đã thêm sự kiện vào lịch." });
      const d = new Date(payload.type === "MEETING" ? payload.startTime : payload.endTime || payload.startTime);
      setCursor({ year: d.getFullYear(), month: d.getMonth() });
      load();
    } catch (e) {
      setFormError(e.message || "Không lưu được sự kiện.");
    } finally {
      setSaving(false);
    }
  };

  const remove = async (ev) => {
    if (!window.confirm(`Xóa sự kiện "${ev.title}"?`)) return;
    setDeletingId(ev.id);
    try {
      await calendarApi.delete(ev.id);
      setEvents((list) => list.filter((x) => !(x.id === ev.id && x.milestoneId == null)));
      setNotice({ type: "success", message: "Đã xóa sự kiện." });
    } catch (e) {
      setNotice({ type: "danger", message: e.message || "Không xóa được sự kiện." });
    } finally {
      setDeletingId(null);
    }
  };

  const selectedEvents = selectedKey ? byDay.get(selectedKey) || [] : [];

  let body;
  if (topicsLoading) body = <LoadingState />;
  else if (topicsError) body = <ErrorState message={topicsError} />;
  else if (topics.length === 0)
    body = (
      <EmptyState icon="event_busy" title="Chưa có lịch để hiển thị">
        {isLecturer ? "Bạn chưa có đề tài nào. Lịch hiện sự kiện và mốc của các đề tài bạn hướng dẫn."
          : "Lịch hiện sự kiện và mốc của đề tài sau khi đăng ký của bạn được duyệt."}
      </EmptyState>
    );
  else if (error) body = <ErrorState message={error} onRetry={load} />;
  else if (loading && events.length === 0) body = <LoadingState label="Đang tải lịch..." />;
  else body = showList ? <AgendaList events={monthEvents} onSelect={select} /> : <MonthGrid year={cursor.year} month={cursor.month} byDay={byDay} onSelect={select} />;

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      <div className="flex flex-col lg:flex-row lg:items-end justify-between gap-4">
        <div>
          <span className="text-[10px] font-black uppercase tracking-[0.2em] text-primary mb-1 block">Kế hoạch đồ án</span>
          <h1 className="text-3xl md:text-4xl font-black font-headline tracking-tighter uppercase">Lịch học thuật</h1>
          {topicTitle && topics.length === 1 && <p className="text-sm text-on-surface-variant mt-2">{topicTitle}</p>}
        </div>
        <div className="flex flex-wrap items-center gap-3">
          <TopicPicker topics={topics} value={topicId} onChange={setTopicId} />
          {isLecturer && topicId && (
            <button type="button" onClick={() => { setFormError(""); setFormOpen(true); }}
              className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-primary text-on-primary text-sm font-bold shadow-lg shadow-primary/20">
              <span className="material-symbols-outlined text-lg" aria-hidden="true">add</span>
              Thêm sự kiện
            </button>
          )}
        </div>
      </div>

      {notice && <InlineNotice type={notice.type} message={notice.message} onClose={() => setNotice(null)} autoHideMs={3000} />}

      {topics.length > 0 && (
        <div className="flex flex-wrap items-center justify-between gap-3">
          {wide ? (
            <div className="inline-flex p-1 rounded-2xl bg-surface-container-low" role="group" aria-label="Kiểu hiển thị">
              <button type="button" className={tabBtn(view === "month")} aria-pressed={view === "month"} onClick={() => setView("month")}>Tháng</button>
              <button type="button" className={tabBtn(view === "list")} aria-pressed={view === "list"} onClick={() => setView("list")}>Danh sách</button>
            </div>
          ) : <span />}
          <div className="flex items-center gap-1">
            <button type="button" className={navBtn} onClick={() => shift(-1)} aria-label="Tháng trước">
              <span className="material-symbols-outlined" aria-hidden="true">chevron_left</span>
            </button>
            <h2 className="min-w-[10rem] text-center text-lg font-black" aria-live="polite">{monthTitle(cursor.year, cursor.month)}</h2>
            <button type="button" className={navBtn} onClick={() => shift(1)} aria-label="Tháng sau">
              <span className="material-symbols-outlined" aria-hidden="true">chevron_right</span>
            </button>
            <button type="button" onClick={goToday} className="ml-2 px-3 py-2 rounded-xl text-sm font-bold border border-outline-variant/20 hover:bg-surface-container">
              Hôm nay
            </button>
          </div>
        </div>
      )}

      {body}

      {topics.length > 0 && !error && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <section className="lg:col-span-2 bg-surface-container-low rounded-[2rem] p-6" aria-labelledby="cal-upcoming">
            <h2 id="cal-upcoming" className="text-lg font-black mb-4">Sắp tới</h2>
            {soon.length === 0 ? (
              <p className="text-sm text-outline">Không có sự kiện sắp tới.</p>
            ) : (
              <ul className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {soon.map((ev) => (
                  <li key={`${ev.type}-${ev.id ?? ev.milestoneId}`}>
                    <button type="button" onClick={() => select(ev)}
                      className="w-full text-left flex items-center gap-3 bg-white dark:bg-slate-900 rounded-2xl p-4 hover:shadow-md transition-shadow">
                      <span className={`h-10 w-1.5 rounded-full shrink-0 ${typeOf(ev).dot}`} aria-hidden="true"></span>
                      <span className="min-w-0">
                        <span className="block text-sm font-bold truncate">{ev.title}</span>
                        <span className="block text-xs text-outline first-letter:uppercase">{fmtDay(eventDate(ev))}</span>
                      </span>
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </section>
          <section className="bg-surface-container-low rounded-[2rem] p-6" aria-labelledby="cal-legend">
            <h2 id="cal-legend" className="text-[11px] font-black uppercase tracking-widest text-outline mb-4">Chú thích</h2>
            <ul className="space-y-3">
              {Object.entries(EVENT_TYPES).map(([k, t]) => (
                <li key={k} className="flex items-center gap-3 text-sm font-medium">
                  <span className={`h-3 w-3 rounded-full ${t.dot}`} aria-hidden="true"></span>
                  {t.label}
                </li>
              ))}
            </ul>
          </section>
        </div>
      )}

      {selectedEvents.length > 0 && (
        <EventDialog events={selectedEvents} canEdit={isLecturer} deletingId={deletingId} onDelete={remove}
          onClose={() => setSelectedKey(null)} />
      )}
      {formOpen && (
        <EventForm topicTitle={topicTitle} saving={saving} error={formError} onSubmit={create} onClose={() => setFormOpen(false)} />
      )}
    </div>
  );
}
