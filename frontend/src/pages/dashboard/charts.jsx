// Biểu đồ một chuỗi dữ liệu cho dashboard (P4-01): một màu (primary), nhãn giá trị trực tiếp, tooltip khi rê chuột,
// giá trị luôn hiện bằng chữ nên không phụ thuộc màu. Không cần thư viện biểu đồ.

export const panel = "bg-white dark:bg-slate-900 p-6 rounded-[1.5rem] border border-outline-variant/10 shadow-sm";
export const panelTitle = "text-[10px] font-black uppercase tracking-[0.2em] text-outline mb-5";

/** Ô số liệu chính (không phải biểu đồ). */
export function StatTile({ label, value, hint, icon }) {
    return (
        <div className={panel}>
            <div className="flex items-center gap-3 mb-3">
                {icon && <span className="material-symbols-outlined text-primary" aria-hidden="true">{icon}</span>}
                <p className="text-[10px] font-black uppercase tracking-widest text-outline">{label}</p>
            </div>
            <p className="text-3xl font-black text-on-surface tabular-nums">{value ?? "—"}</p>
            {hint && <p className="text-[11px] text-outline font-bold mt-2">{hint}</p>}
        </div>
    );
}

/** Danh sách cột ngang: [{ label, value, note? }] — độ dài theo giá trị lớn nhất. */
export function BarList({ title, items, unit = "", empty = "Chưa có dữ liệu." }) {
    const max = Math.max(1, ...items.map((i) => i.value));
    return (
        <section className={panel} aria-label={title}>
            <h3 className={panelTitle}>{title}</h3>
            {items.length === 0 ? <p className="text-xs font-bold text-outline">{empty}</p> : (
                <ul className="space-y-3">
                    {items.map((i) => (
                        <li key={i.label} title={`${i.label}: ${i.value}${unit}${i.note ? ` · ${i.note}` : ""}`}>
                            <div className="flex justify-between gap-3 text-xs font-bold mb-1">
                                <span className="truncate text-on-surface">{i.label}</span>
                                <span className="tabular-nums text-on-surface-variant shrink-0">{i.value}{unit}{i.note ? <span className="text-outline font-medium"> · {i.note}</span> : null}</span>
                            </div>
                            <div className="h-2 rounded-full bg-surface-container">
                                <div className="h-2 rounded-full bg-primary" style={{ width: `${(i.value / max) * 100}%`, minWidth: i.value > 0 ? 4 : 0 }} />
                            </div>
                        </li>
                    ))}
                </ul>
            )}
        </section>
    );
}

/** Histogram cột đứng: [{ label, count }] — dùng cho phân bố điểm. */
export function Histogram({ title, bins, caption }) {
    const max = Math.max(1, ...bins.map((b) => b.count));
    const total = bins.reduce((s, b) => s + b.count, 0);
    return (
        <section className={panel} aria-label={title}>
            <h3 className={panelTitle}>{title}</h3>
            {total === 0 ? <p className="text-xs font-bold text-outline">Chưa có điểm nào.</p> : (
                <>
                    <div className="flex items-end gap-[2px] h-40 border-b border-outline-variant/30" role="img"
                        aria-label={bins.map((b) => `${b.label}: ${b.count}`).join(", ")}>
                        {bins.map((b) => (
                            <div key={b.label} className="group relative flex-1 h-full flex flex-col justify-end" title={`Điểm ${b.label}: ${b.count} sinh viên`}>
                                {b.count > 0 && <span className="text-[10px] font-black text-on-surface-variant text-center tabular-nums mb-1">{b.count}</span>}
                                <div className="bg-primary rounded-t-[4px] group-hover:opacity-80" style={{ height: `${(b.count / max) * 80}%`, minHeight: b.count > 0 ? 4 : 0 }} />
                            </div>
                        ))}
                    </div>
                    <div className="flex gap-[2px] mt-1">
                        {bins.map((b) => <span key={b.label} className="flex-1 text-center text-[9px] font-bold text-outline tabular-nums">{b.label}</span>)}
                    </div>
                </>
            )}
            {caption && <p className="text-[11px] text-outline font-bold mt-3">{caption}</p>}
        </section>
    );
}

/** Thanh phần trăm một giá trị (tỉ lệ). */
export function RateBar({ label, rate, detail }) {
    const pct = rate == null ? null : Math.round(rate * 10) / 10;
    return (
        <div title={detail}>
            <div className="flex justify-between text-xs font-bold mb-1">
                <span className="text-on-surface">{label}</span>
                <span className="tabular-nums text-on-surface-variant">{pct == null ? "—" : `${pct}%`}</span>
            </div>
            <div className="h-2.5 rounded-full bg-surface-container">
                <div className="h-2.5 rounded-full bg-primary" style={{ width: `${pct ?? 0}%` }} />
            </div>
            {detail && <p className="text-[11px] text-outline font-medium mt-1">{detail}</p>}
        </div>
    );
}

export const fmtDate = (iso) => (iso ? new Date(iso).toLocaleString("vi-VN", { dateStyle: "short", timeStyle: "short" }) : "—");
