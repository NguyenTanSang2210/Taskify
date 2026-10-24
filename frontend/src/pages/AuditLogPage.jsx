import { useCallback, useState } from "react";
import { auditApi } from "../api/auditApi";
import Pagination from "../components/Pagination";
import InlineNotice from "../components/InlineNotice";
import useLoadEffect from "../hooks/useLoadEffect";
import AuditFilters from "./auditLog/AuditFilters";
import AuditTable from "./auditLog/AuditTable";

const EMPTY = { actor: "", action: "", category: "", entity: "", from: "", to: "" };
const PAGE_SIZE = 50;

const nextDay = (date) => {
    const d = new Date(`${date}T00:00:00`);
    d.setDate(d.getDate() + 1);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}T00:00`;
};

/** Bộ lọc giao diện → tham số API ("Đến ngày" tính trọn ngày: mốc kết thúc là đầu ngày hôm sau, không bao gồm). */
const toParams = (f) => ({
    actor: f.actor.trim(), action: f.action, category: f.category, entity: f.entity,
    from: f.from ? `${f.from}T00:00` : "", to: f.to ? nextDay(f.to) : "",
});

/** P4-02 — nhật ký hệ thống cho Admin: lọc, xem chi tiết, xuất CSV. */
export default function AuditLogPage() {
    const [draft, setDraft] = useState(EMPTY);
    const [applied, setApplied] = useState(EMPTY);
    const [page, setPage] = useState(0);
    const [data, setData] = useState({ items: [], totalPages: 0, totalElements: 0 });
    const [options, setOptions] = useState({});
    const [loading, setLoading] = useState(true);
    const [exporting, setExporting] = useState(false);
    const [expandedId, setExpandedId] = useState(null);
    const [notice, setNotice] = useState(null);

    const loadOptions = useCallback(async () => {
        try {
            setOptions(await auditApi.options());
        } catch { /* ô chọn để trống: vẫn lọc được theo nhóm/người thực hiện */ }
    }, []);
    useLoadEffect(loadOptions);

    const load = useCallback(async () => {
        setLoading(true);
        try {
            setData(await auditApi.search({ ...toParams(applied), page, size: PAGE_SIZE }));
        } catch (e) {
            setNotice({ type: "danger", message: e.message || "Không tải được nhật ký." });
        } finally {
            setLoading(false);
        }
    }, [applied, page]);
    useLoadEffect(load);

    const apply = (filters) => {
        setApplied(filters);
        setPage(0);
        setExpandedId(null);
    };
    const onCategory = (category) => {
        const next = { ...draft, category, action: "" };
        setDraft(next);
        apply(next);
    };
    const reset = () => {
        setDraft(EMPTY);
        apply(EMPTY);
    };
    const exportCsv = async () => {
        setExporting(true);
        try {
            await auditApi.exportCsv(toParams(applied));
        } catch (e) {
            setNotice({ type: "danger", message: e.message || "Không xuất được file." });
        } finally {
            setExporting(false);
        }
    };

    return (
        <div className="space-y-8 animate-in fade-in duration-500">
            <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
                <div>
                    <span className="text-[10px] font-black uppercase tracking-[0.2em] text-primary mb-1 block">Kiểm toán hệ thống</span>
                    <h1 className="text-3xl md:text-4xl font-black font-headline tracking-tighter uppercase">Nhật ký hệ thống</h1>
                    <p className="text-sm text-on-surface-variant mt-2">Ai đã làm gì, lúc nào, từ đâu. Nhật ký giữ 12 tháng; nhật ký điểm số được giữ lâu dài.</p>
                </div>
                <button type="button" onClick={exportCsv} disabled={exporting}
                    className="inline-flex items-center gap-2 px-6 py-3 rounded-2xl bg-white dark:bg-slate-900 border border-outline-variant/20 text-[10px] font-black uppercase tracking-widest shadow-sm hover:bg-surface-container disabled:opacity-50">
                    <span className="material-symbols-outlined text-lg" aria-hidden="true">download</span>
                    {exporting ? "Đang xuất..." : "Xuất CSV"}
                </button>
            </div>

            {notice && <InlineNotice type={notice.type} message={notice.message} onClose={() => setNotice(null)} />}

            <div className="bg-white dark:bg-slate-900 rounded-[2.5rem] border border-outline-variant/10 shadow-sm p-6 md:p-8 space-y-6">
                <AuditFilters draft={draft} setDraft={setDraft} options={options} onApply={() => apply(draft)} onReset={reset} onCategory={onCategory} />
                <AuditTable items={data.items || []} loading={loading} expandedId={expandedId}
                    onToggle={(id) => setExpandedId((cur) => (cur === id ? null : id))} />
                <Pagination page={page} totalPages={data.totalPages} totalElements={data.totalElements} onChange={setPage} disabled={loading} />
            </div>
        </div>
    );
}
