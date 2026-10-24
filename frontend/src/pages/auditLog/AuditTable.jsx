import { Fragment } from "react";
import { auditActionLabel } from "../../constants/auditActions";

const fmt = (iso) => (iso ? new Date(iso).toLocaleString("vi-VN", { dateStyle: "short", timeStyle: "medium" }) : "—");

/** Bảng nhật ký; bấm một dòng để xem chi tiết (nội dung, trình duyệt). Hành động điểm số được tô nổi bật. */
export default function AuditTable({ items, loading, expandedId, onToggle }) {
    return (
        <div className="overflow-x-auto rounded-2xl border border-outline-variant/10">
            <table className="w-full text-left text-sm">
                <thead className="bg-surface-container-low text-[10px] font-black uppercase tracking-widest text-outline">
                    <tr>
                        <th className="px-4 py-3">Thời gian</th>
                        <th className="px-4 py-3">Người thực hiện</th>
                        <th className="px-4 py-3">Hành động</th>
                        <th className="px-4 py-3">Đối tượng</th>
                        <th className="px-4 py-3">IP</th>
                        <th className="px-4 py-3"><span className="sr-only">Chi tiết</span></th>
                    </tr>
                </thead>
                <tbody className="divide-y divide-outline-variant/10">
                    {items.map((a) => {
                        const open = expandedId === a.id;
                        const grade = a.action?.startsWith("GRADE_");
                        return (
                            <Fragment key={a.id}>
                                <tr className={`cursor-pointer hover:bg-surface-container-low ${open ? "bg-surface-container-low" : ""}`} onClick={() => onToggle(a.id)}>
                                    <td className="px-4 py-3 whitespace-nowrap text-xs font-bold text-outline">{fmt(a.timestamp)}</td>
                                    <td className="px-4 py-3 font-bold">{a.actorUsername || "—"}</td>
                                    <td className="px-4 py-3">
                                        <span className={`inline-block px-2.5 py-1 rounded-lg text-xs font-bold ${grade ? "bg-tertiary-container text-on-tertiary-container" : "bg-surface-container text-on-surface"}`}>
                                            {auditActionLabel(a.action)}
                                        </span>
                                    </td>
                                    <td className="px-4 py-3 text-xs">{a.entityName ? `${a.entityName} #${a.entityId ?? ""}` : "—"}</td>
                                    <td className="px-4 py-3 font-mono text-xs text-outline">{a.ipAddress || "—"}</td>
                                    <td className="px-4 py-3 text-right">
                                        <button type="button" aria-expanded={open} aria-label={open ? "Ẩn chi tiết" : "Xem chi tiết"}
                                            onClick={(e) => { e.stopPropagation(); onToggle(a.id); }} className="p-1 rounded-lg hover:bg-surface-container">
                                            <span className="material-symbols-outlined text-lg" aria-hidden="true">{open ? "expand_less" : "expand_more"}</span>
                                        </button>
                                    </td>
                                </tr>
                                {open && (
                                    <tr className="bg-surface-container-low">
                                        <td colSpan={6} className="px-6 pb-5 pt-1">
                                            <dl className="grid gap-3 md:grid-cols-[10rem_1fr] text-xs">
                                                <dt className="font-black uppercase tracking-widest text-outline">Mã hành động</dt>
                                                <dd className="font-mono">{a.action}</dd>
                                                <dt className="font-black uppercase tracking-widest text-outline">Chi tiết</dt>
                                                <dd className="whitespace-pre-wrap break-words font-bold">{a.details || "—"}</dd>
                                                <dt className="font-black uppercase tracking-widest text-outline">Trình duyệt</dt>
                                                <dd className="break-words text-outline">{a.userAgent || "—"}</dd>
                                            </dl>
                                        </td>
                                    </tr>
                                )}
                            </Fragment>
                        );
                    })}
                    {!loading && items.length === 0 && (
                        <tr><td colSpan={6} className="px-4 py-10 text-center text-xs font-bold text-outline">Không có bản ghi phù hợp bộ lọc.</td></tr>
                    )}
                </tbody>
            </table>
        </div>
    );
}
