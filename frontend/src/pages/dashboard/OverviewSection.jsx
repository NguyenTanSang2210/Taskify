import { useCallback, useState } from "react";
import { dashboardApi } from "../../api/dashboardApi";
import InlineNotice from "../../components/InlineNotice";
import useLoadEffect from "../../hooks/useLoadEffect";
import { BarList, Histogram, RateBar, StatTile, panel, panelTitle } from "./charts";

const TOPIC_LABELS = { OPEN: "Đang mở", FULL: "Đã đủ sinh viên", CLOSED: "Đã đóng", COMPLETED: "Hoàn thành" };
const select = "px-4 py-3 bg-white dark:bg-slate-900 border border-outline-variant/20 rounded-2xl text-sm font-bold outline-none focus:ring-2 focus:ring-primary/30";

const hours = (h) => (h == null ? "—" : h < 48 ? `${Math.round(h * 10) / 10} giờ` : `${Math.round((h / 24) * 10) / 10} ngày`);

/** P4-01 — thống kê nâng cao cho ADMIN (mọi khoa hoặc một khoa) và quản trị khoa (khoa của mình), lọc theo đợt. */
export default function OverviewSection({ isAdmin }) {
    const [departmentId, setDepartmentId] = useState("");
    const [workspaceId, setWorkspaceId] = useState("");
    const [data, setData] = useState(null);
    const [error, setError] = useState(null);
    const [exporting, setExporting] = useState(false);

    const load = useCallback(async () => {
        try {
            setData(await dashboardApi.getOverview({ departmentId, workspaceId }));
            setError(null);
        } catch (e) {
            setError(e.message || "Không tải được số liệu thống kê.");
        }
    }, [departmentId, workspaceId]);
    useLoadEffect(load);

    const exportExcel = async () => {
        setExporting(true);
        try {
            await dashboardApi.exportOverview({ departmentId, workspaceId });
        } catch (e) {
            setError(e.message || "Không xuất được báo cáo.");
        } finally {
            setExporting(false);
        }
    };

    if (error && !data) return <InlineNotice type="danger" message={error} onClose={() => setError(null)} />;
    if (!data) return <p className="text-center text-xs font-bold uppercase tracking-widest text-outline animate-pulse">Đang tải số liệu thống kê...</p>;

    const topicItems = Object.entries(TOPIC_LABELS).map(([k, label]) => ({ label, value: data.topicsByStatus?.[k] ?? 0 }));
    const reg = data.registrations || {};
    const regItems = [
        { label: "Chờ duyệt", value: reg.pending ?? 0 },
        { label: "Đã duyệt", value: reg.approved ?? 0 },
        { label: "Từ chối", value: reg.rejected ?? 0 },
    ];
    const lecturerItems = (data.lecturerLoad || []).map((l) => ({ label: l.name, value: l.approvedStudents, note: `${l.topics} đề tài` }));

    return (
        <section className="space-y-6" aria-label="Thống kê">
            {error && <InlineNotice type="danger" message={error} onClose={() => setError(null)} />}
            <div className="flex flex-wrap items-end gap-3">
                {isAdmin && (
                    <label className="flex flex-col gap-1">
                        <span className="text-[10px] font-black uppercase tracking-[0.2em] text-outline">Khoa</span>
                        <select className={select} value={departmentId} onChange={(e) => { setDepartmentId(e.target.value); setWorkspaceId(""); }}>
                            <option value="">Toàn hệ thống</option>
                            {(data.departments || []).map((d) => <option key={d.id} value={String(d.id)}>{d.name}</option>)}
                        </select>
                    </label>
                )}
                <label className="flex flex-col gap-1">
                    <span className="text-[10px] font-black uppercase tracking-[0.2em] text-outline">Đợt đồ án</span>
                    <select className={select} value={workspaceId} onChange={(e) => setWorkspaceId(e.target.value)}>
                        <option value="">Tất cả các đợt</option>
                        {(data.workspaces || []).map((w) => <option key={w.id} value={String(w.id)}>{w.name}</option>)}
                    </select>
                </label>
                <button type="button" onClick={exportExcel} disabled={exporting}
                    className="ml-auto inline-flex items-center gap-2 px-6 py-3 rounded-2xl bg-white dark:bg-slate-900 border border-outline-variant/20 text-[10px] font-black uppercase tracking-widest shadow-sm hover:bg-surface-container disabled:opacity-50">
                    <span className="material-symbols-outlined text-lg" aria-hidden="true">download</span>
                    {exporting ? "Đang xuất..." : "Xuất Excel"}
                </button>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                <StatTile icon="school" label="Sinh viên đã có đề tài" value={`${data.students?.withTopic ?? 0}/${data.students?.total ?? 0}`}
                    hint={data.students?.rate == null ? "Chưa có sinh viên" : `${data.students.rate}% sinh viên`} />
                <StatTile icon="task_alt" label="Đã chấm điểm" value={`${reg.graded ?? 0}/${reg.approved ?? 0}`}
                    hint={reg.completionRate == null ? "Chưa có sinh viên được duyệt" : `${reg.completionRate}% sinh viên được duyệt`} />
                <StatTile icon="grade" label="Điểm trung bình" value={data.scores?.average ?? "—"} hint="Thang điểm 10" />
                <StatTile icon="schedule" label="Phản hồi tiến độ trung bình" value={hours(data.feedback?.avgHours)}
                    hint={`Từ lúc SV nộp đến khi GV nhận xét · ${data.feedback?.measured ?? 0} báo cáo · ${data.feedback?.uncommented ?? 0} chưa nhận xét`} />
            </div>

            <div className={`${panel} grid gap-5 md:grid-cols-2`}>
                <h3 className={`${panelTitle} md:col-span-2 mb-0`}>Tỉ lệ</h3>
                <RateBar label="Sinh viên đã có đề tài được duyệt" rate={data.students?.rate}
                    detail={`${data.students?.withTopic ?? 0} trên ${data.students?.total ?? 0} sinh viên`} />
                <RateBar label="Hoàn thành (đã có điểm)" rate={reg.completionRate}
                    detail={`${reg.graded ?? 0} trên ${reg.approved ?? 0} sinh viên được duyệt`} />
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <BarList title="Đề tài theo trạng thái" items={topicItems} />
                <BarList title="Đăng ký đề tài" items={regItems} />
                <Histogram title="Phân bố điểm" bins={data.scores?.histogram || []}
                    caption={data.scores?.average != null ? `Điểm trung bình ${data.scores.average} · ${reg.graded ?? 0} sinh viên có điểm` : null} />
                <BarList title="Số sinh viên mỗi giảng viên hướng dẫn" items={lecturerItems} empty="Chưa có giảng viên có đề tài." />
            </div>
        </section>
    );
}
