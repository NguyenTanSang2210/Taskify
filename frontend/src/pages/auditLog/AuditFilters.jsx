import { AUDIT_CATEGORIES, auditActionLabel } from "../../constants/auditActions";

const input = "w-full px-4 py-3 bg-surface-container-low rounded-2xl text-sm font-bold outline-none focus:ring-2 focus:ring-primary/30";
const label = "block text-[10px] font-black uppercase tracking-[0.2em] text-outline mb-2";

/** Bộ lọc nhật ký: nhóm nhanh, người thực hiện, hành động, đối tượng, khoảng ngày (P4-02). */
export default function AuditFilters({ draft, setDraft, options, onApply, onReset, onCategory }) {
    const set = (key) => (e) => setDraft((d) => ({ ...d, [key]: e.target.value }));
    return (
        <form className="space-y-5" onSubmit={(e) => { e.preventDefault(); onApply(); }}>
            <div className="flex flex-wrap gap-2" role="group" aria-label="Nhóm hành động">
                {AUDIT_CATEGORIES.map((c) => (
                    <button key={c.id || "all"} type="button" aria-pressed={draft.category === c.id} onClick={() => onCategory(c.id)}
                        className={`px-5 py-2.5 rounded-2xl text-[10px] font-black uppercase tracking-widest transition-all ${draft.category === c.id ? "bg-primary text-white shadow-lg shadow-primary/20" : "bg-surface-container-low text-outline hover:text-on-surface"}`}>
                        {c.label}
                    </button>
                ))}
            </div>
            <div className="grid gap-4 md:grid-cols-5">
                <div>
                    <label htmlFor="audit-actor" className={label}>Người thực hiện</label>
                    <input id="audit-actor" value={draft.actor} onChange={set("actor")} placeholder="Tên đăng nhập" className={input} />
                </div>
                <div>
                    <label htmlFor="audit-action" className={label}>Hành động</label>
                    <select id="audit-action" value={draft.action} onChange={set("action")} className={input}>
                        <option value="">Tất cả</option>
                        {(options.actions || []).map((a) => <option key={a} value={a}>{auditActionLabel(a)}</option>)}
                    </select>
                </div>
                <div>
                    <label htmlFor="audit-entity" className={label}>Đối tượng</label>
                    <select id="audit-entity" value={draft.entity} onChange={set("entity")} className={input}>
                        <option value="">Tất cả</option>
                        {(options.entities || []).map((e) => <option key={e} value={e}>{e}</option>)}
                    </select>
                </div>
                <div>
                    <label htmlFor="audit-from" className={label}>Từ ngày</label>
                    <input id="audit-from" type="date" value={draft.from} onChange={set("from")} className={input} />
                </div>
                <div>
                    <label htmlFor="audit-to" className={label}>Đến ngày</label>
                    <input id="audit-to" type="date" value={draft.to} onChange={set("to")} className={input} />
                </div>
            </div>
            <div className="flex flex-wrap gap-3">
                <button type="submit" className="inline-flex items-center gap-2 px-6 py-3 rounded-2xl bg-primary text-white text-[10px] font-black uppercase tracking-widest shadow-lg shadow-primary/20">
                    <span className="material-symbols-outlined text-lg" aria-hidden="true">filter_alt</span>Lọc
                </button>
                <button type="button" onClick={onReset} className="px-6 py-3 rounded-2xl bg-surface-container-low text-on-surface text-[10px] font-black uppercase tracking-widest hover:bg-surface-container">
                    Xóa bộ lọc
                </button>
            </div>
        </form>
    );
}
