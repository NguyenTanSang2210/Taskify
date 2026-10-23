import { Link } from "react-router-dom";
import { RateBar, StatTile, fmtDate, panel, panelTitle } from "./charts";

const STATUS = { PENDING: "Chờ duyệt", APPROVED: "Đã được duyệt", REJECTED: "Bị từ chối" };

/** P4-01 — sinh viên: đề tài + trạng thái, tiến độ Kanban (% hoàn thành), mốc sắp tới, điểm. */
export default function StudentSection({ stats }) {
    const reg = stats.registration;
    const p = stats.progress || {};
    const milestones = stats.upcomingMilestones || [];
    if (!reg) {
        return (
            <section className={`${panel} text-center md:text-left`} aria-label="Đề tài của tôi">
                <h2 className="text-2xl font-black text-on-surface uppercase tracking-tight font-headline">Bạn chưa đăng ký đề tài</h2>
                <p className="text-sm text-on-surface-variant font-medium mt-2">Khám phá danh sách đề tài của các đợt mà lớp bạn tham gia để bắt đầu.</p>
                <Link to="/" className="inline-block mt-5 px-8 py-3 bg-primary text-white rounded-xl font-black text-xs uppercase tracking-widest shadow-xl shadow-primary/20">Xem danh sách đề tài</Link>
            </section>
        );
    }
    return (
        <section className="space-y-6" aria-label="Đề tài của tôi">
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <div className={`${panel} md:col-span-2`}>
                    <h3 className={panelTitle}>Đề tài của tôi</h3>
                    <p className="text-xl font-black text-on-surface">{reg.topicTitle}</p>
                    <p className="text-xs font-bold text-outline mt-1">GV hướng dẫn: {reg.lecturerName || "—"} · {reg.workspaceName || ""}</p>
                    <p className="mt-3 inline-block px-3 py-1 rounded-lg bg-surface-container text-xs font-black">{STATUS[reg.status] || reg.status}</p>
                    {reg.status === "APPROVED" && (
                        <div className="mt-5">
                            <RateBar label="Tiến độ (báo cáo đã hoàn thành)" rate={p.percent}
                                detail={`${p.done ?? 0} hoàn thành · ${p.inProgress ?? 0} đang làm · ${p.todo ?? 0} cần làm`} />
                        </div>
                    )}
                </div>
                <StatTile icon="grade" label="Điểm" value={reg.score ?? "—"} hint={reg.score == null ? "Chưa có điểm" : "Thang điểm 10"} />
            </div>
            <section className={panel} aria-label="Mốc sắp tới">
                <h3 className={panelTitle}>Mốc sắp tới</h3>
                {milestones.length === 0 ? <p className="text-xs font-bold text-outline">Chưa có mốc nào sắp tới.</p> : (
                    <ul className="divide-y divide-outline-variant/10">
                        {milestones.map((m) => (
                            <li key={m.id} className="py-3 flex justify-between gap-3 text-sm">
                                <span className="font-bold">{m.title}</span>
                                <span className="text-xs font-bold text-on-surface-variant">{fmtDate(m.deadline)}</span>
                            </li>
                        ))}
                    </ul>
                )}
            </section>
            <div className="flex flex-wrap gap-3">
                <Link to="/progress" className="px-6 py-2.5 bg-primary text-white rounded-xl font-black text-[10px] uppercase tracking-widest shadow-lg shadow-primary/20">Theo dõi tiến độ</Link>
                <Link to="/project-chat" className="px-6 py-2.5 bg-surface-container text-on-surface-variant rounded-xl font-black text-[10px] uppercase tracking-widest">Trao đổi với giảng viên</Link>
            </div>
        </section>
    );
}
