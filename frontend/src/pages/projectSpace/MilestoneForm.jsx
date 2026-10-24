import { useState } from "react";
import { toLocalInput } from "./milestoneUtils";

const input = "w-full px-4 py-3 bg-surface-container-low rounded-xl text-sm font-bold outline-none focus:ring-2 focus:ring-primary/30";
const label = "block text-[10px] font-black uppercase tracking-[0.2em] text-outline mb-1.5";

/** Tạo / sửa một mốc: tên, hạn chót, trọng số (%), mô tả. */
export default function MilestoneForm({ initial, onSubmit, onCancel, busy }) {
    const [form, setForm] = useState({
        title: initial?.title || "",
        deadline: toLocalInput(initial?.deadline),
        weight: initial?.weight ?? "",
        description: initial?.description || "",
    });
    const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));
    const submit = (e) => {
        e.preventDefault();
        onSubmit({
            title: form.title,
            deadline: form.deadline || null,
            weight: form.weight === "" ? null : Number(form.weight),
            description: form.description,
        });
    };
    return (
        <form onSubmit={submit} className="grid gap-4 md:grid-cols-12 items-end bg-surface-container-low/50 p-5 rounded-2xl" aria-label={initial ? "Sửa mốc" : "Thêm mốc"}>
            <div className="md:col-span-4">
                <label htmlFor="ms-title" className={label}>Tên mốc</label>
                <input id="ms-title" required value={form.title} onChange={set("title")} placeholder="VD: Nộp đề cương" className={input} />
            </div>
            <div className="md:col-span-3">
                <label htmlFor="ms-deadline" className={label}>Hạn chót</label>
                <input id="ms-deadline" required type="datetime-local" value={form.deadline} onChange={set("deadline")} className={input} />
            </div>
            <div className="md:col-span-2">
                <label htmlFor="ms-weight" className={label}>Trọng số (%)</label>
                <input id="ms-weight" type="number" min="0" max="100" value={form.weight} onChange={set("weight")} placeholder="Không tính điểm" className={input} />
            </div>
            <div className="md:col-span-3 flex gap-2">
                <button type="submit" disabled={busy} className="flex-1 px-5 py-3 rounded-xl bg-primary text-on-primary text-[10px] font-black uppercase tracking-widest shadow-lg shadow-primary/20 disabled:opacity-50">
                    {initial ? "Lưu" : "Thêm mốc"}
                </button>
                {onCancel && <button type="button" onClick={onCancel} className="px-4 py-3 rounded-xl bg-surface-container text-[10px] font-black uppercase tracking-widest">Hủy</button>}
            </div>
            <div className="md:col-span-12">
                <label htmlFor="ms-desc" className={label}>Yêu cầu nộp (mô tả)</label>
                <textarea id="ms-desc" rows={2} value={form.description} onChange={set("description")} placeholder="Nội dung, định dạng file cần nộp…" className={input} />
            </div>
        </form>
    );
}
