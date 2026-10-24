import { useState } from "react";
import Modal from "../../components/Modal";

const field = "w-full px-4 py-3 bg-surface-container-low rounded-xl text-sm font-medium outline-none border border-transparent focus:border-primary";
const label = "block text-[10px] font-black uppercase tracking-widest text-outline mb-1.5";

const TYPES = [
  { value: "MEETING", label: "Họp & sự kiện" },
  { value: "DEADLINE", label: "Hạn chót" },
];

/** GV thêm sự kiện vào lịch của đề tài đang chọn. Mốc có trọng số được tạo ở Không gian làm việc. */
export default function EventForm({ topicTitle, saving, error, onSubmit, onClose }) {
  const [form, setForm] = useState({ title: "", type: "MEETING", startTime: "", endTime: "", description: "" });
  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));

  const submit = (e) => {
    e.preventDefault();
    onSubmit({
      title: form.title.trim(),
      type: form.type,
      startTime: form.startTime || null,
      endTime: form.endTime || null,
      description: form.description.trim() || null,
    });
  };

  return (
    <Modal eyebrow={topicTitle} title="Thêm sự kiện" onClose={onClose}
      footer={
        <>
          <button type="button" onClick={onClose} className="px-5 py-2.5 rounded-xl text-sm font-bold hover:bg-surface-container">Hủy</button>
          <button type="submit" form="calendar-event-form" disabled={saving || !form.title.trim() || !form.startTime}
            className="px-5 py-2.5 rounded-xl bg-primary text-on-primary text-sm font-bold disabled:opacity-50">
            {saving ? "Đang lưu..." : "Lưu sự kiện"}
          </button>
        </>
      }>
      <form id="calendar-event-form" onSubmit={submit} className="space-y-4">
        {error && <p role="alert" className="text-sm font-bold text-error">{error}</p>}
        <div>
          <label htmlFor="ev-title" className={label}>Tiêu đề *</label>
          <input id="ev-title" data-autofocus className={field} value={form.title} onChange={set("title")} maxLength={255} required />
        </div>
        <div>
          <label htmlFor="ev-type" className={label}>Loại</label>
          <select id="ev-type" className={field} value={form.type} onChange={set("type")}>
            {TYPES.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}
          </select>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label htmlFor="ev-start" className={label}>Bắt đầu *</label>
            <input id="ev-start" type="datetime-local" className={field} value={form.startTime} onChange={set("startTime")} required />
          </div>
          <div>
            <label htmlFor="ev-end" className={label}>{form.type === "DEADLINE" ? "Hạn chót" : "Kết thúc"}</label>
            <input id="ev-end" type="datetime-local" className={field} value={form.endTime} min={form.startTime || undefined}
              onChange={set("endTime")} />
          </div>
        </div>
        <div>
          <label htmlFor="ev-desc" className={label}>Mô tả</label>
          <textarea id="ev-desc" rows={3} className={field} value={form.description} onChange={set("description")} maxLength={5000} />
        </div>
      </form>
    </Modal>
  );
}
